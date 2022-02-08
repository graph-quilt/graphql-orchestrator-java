package com.intuit.graphql.orchestrator.apollofederation;

import static graphql.language.AstPrinter.printAstCompact;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.introspection.Introspection;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.schema.GraphQLFieldsContainer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityQuery {

  private final OperationDefinition.Builder queryOperationBuilder = OperationDefinition.newOperationDefinition();
  private final GraphQLContext graphQLContext;
  List<Map<String, Object>> variables = new ArrayList<>();
  private SelectionSet.Builder entitiesSelectionSetBuilder = SelectionSet.newSelectionSet();
  Field.Builder entitiesFieldBuilder = Field.newField().name("entities");

  public EntityQuery(GraphQLContext graphQLContext) {
    this.graphQLContext = graphQLContext;
    queryOperationBuilder.operation(Operation.QUERY);
  }

  public ExecutionInput createExecutionInput() {
    Field entitiesField = entitiesFieldBuilder.selectionSet(entitiesSelectionSetBuilder.build()).build();
    queryOperationBuilder.selectionSet(SelectionSet.newSelectionSet().selection(entitiesField).build());
    Document document = Document.newDocument()
        //.definitions(fragmentDefinitions)
        .definition(queryOperationBuilder.build())
        .build();

    Map<String, Object> representations = new HashMap<>();
    representations.put("representations", variables);

    return ExecutionInput.newExecutionInput()
        .context(graphQLContext)
        .root(document)
        .query(printAstCompact(document))
        .variables(representations) // TODO if if not empty or throw exception
        .build();
  }

  public void add(EntityBatchLoadingEnvironment batchLoadingEnvironment) {

    GraphQLFieldsContainer entityType = (GraphQLFieldsContainer) batchLoadingEnvironment.getDataFetchingEnvironment().getParentType();
    String entityTypeName = entityType.getName();

    // create representation
    Map<String, Object> entityRepresentation = new HashMap<>();
    entityRepresentation.put(Introspection.TypeNameMetaFieldDef.getName(), entityTypeName);

    // add key values to represetation
    Map<String, Object> dataSource = batchLoadingEnvironment.getDataFetchingEnvironment().getSource();
    EntityExtensionDefinition extensionDefinition = batchLoadingEnvironment.getEntityExtensionContext()
        .getThisEntityExtensionDefinition();
    extensionDefinition.getKeyDirectiveDefinitions().stream()
            .flatMap(keyDirectiveDefinition -> keyDirectiveDefinition.getKeyFieldNames().stream())
            .forEach(keyFieldName -> entityRepresentation.put(keyFieldName, dataSource.get(keyFieldName)));

    variables.add(entityRepresentation);

    // create inline fragment definition
    Field originalField = batchLoadingEnvironment.getDataFetchingEnvironment().getField();
    Field __typenameField = Field.newField().name(Introspection.TypeNameMetaFieldDef.getName()).build();

    SelectionSet fieldSelectionSet = batchLoadingEnvironment.getDataFetchingEnvironment().getField()
        .getSelectionSet()
        .transform(builder -> builder.selection(__typenameField));

    InlineFragment.Builder inlineFragmentBuilder = InlineFragment.newInlineFragment();
    inlineFragmentBuilder.typeCondition(TypeName.newTypeName().name(entityTypeName).build());
    inlineFragmentBuilder.selectionSet(SelectionSet.newSelectionSet()
            .selection(Field.newField().selectionSet(fieldSelectionSet).name(originalField.getName()).build())
        .build());
    entitiesSelectionSetBuilder.selection(inlineFragmentBuilder.build());
  }
}
//  recommendations {
//    id
//    title
//  }

//  query ($representations: [_Any!]!) {
//    _entities(representations: $representations) {
//    ... on Movie {
//        recommendations {
//          id
//              __typename
//        }
//      }
//    }
//  }
//
//  VARIABLE:
//  {
//    "representations" : [ {
//    "__typename" : "Movie",
//        "id" : "1"
//  } ]
//  }