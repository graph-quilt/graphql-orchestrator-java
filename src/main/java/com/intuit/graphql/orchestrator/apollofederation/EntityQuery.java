package com.intuit.graphql.orchestrator.apollofederation;

import static graphql.language.AstPrinter.printAstCompact;
import static graphql.language.TypeName.newTypeName;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import org.apache.commons.collections4.CollectionUtils;

/** This class is used to create a downstream query to make an entity fetch. */
@Builder
public class EntityQuery {

  private static final String REPRESENTATIONS_VAR_NAME = "REPRESENTATIONS";
  private static final String REPRESENTATIONS_ARG_NAME = "representations";
  private static final String REPRESENTATIONS_TYPE_NAME = "[_Any!]!";

  private static final VariableDefinition VARIABLE_DEFINITION =
      VariableDefinition.newVariableDefinition()
          .name(REPRESENTATIONS_VAR_NAME)
          .type(newTypeName(REPRESENTATIONS_TYPE_NAME).build())
          .build();

  private static final Argument REPRESENTATIONS_ARGUMENT =
      Argument.newArgument()
          .name(REPRESENTATIONS_ARG_NAME)
          .value(VariableReference.newVariableReference().name(REPRESENTATIONS_VAR_NAME).build())
          .build();

  private static final String _ENTITIES_FIELD_NAME = "_entities";

  // required constructor arguments
  private final GraphQLContext graphQLContext;
  private final List<InlineFragment> inlineFragments;
  private final List<Map<String, Object>> variables;

  public ExecutionInput createExecutionInput() {
    if (CollectionUtils.isEmpty(inlineFragments)) {
      throw new IllegalStateException(
          "Failed to create ExecutionInput for entity request.  InlineFragments not found");
    }

    List<Selection<?>> selections = new ArrayList<>(inlineFragments);

    Field entitiesField =
        Field.newField()
            .name(_ENTITIES_FIELD_NAME)
            .arguments(Collections.singletonList(REPRESENTATIONS_ARGUMENT))
            .selectionSet(SelectionSet.newSelectionSet().selections(selections).build())
            .build();

    OperationDefinition queryOperation =
        OperationDefinition.newOperationDefinition()
            .operation(Operation.QUERY)
            .selectionSet(SelectionSet.newSelectionSet().selection(entitiesField).build())
            .variableDefinitions(Collections.singletonList(VARIABLE_DEFINITION))
            .build();

    Document document =
        Document.newDocument()
            // TODO in case the original selectionset has fragment definitions
            // .definitions(fragmentDefinitions)
            .definition(queryOperation)
            .build();

    Map<String, Object> representations = new HashMap<>();
    representations.put(REPRESENTATIONS_VAR_NAME, variables);

    return ExecutionInput.newExecutionInput()
        .context(graphQLContext)
        .root(document)
        .query(printAstCompact(document))
        .variables(representations) // TODO if not empty or throw exception
        .build();
  }

}

