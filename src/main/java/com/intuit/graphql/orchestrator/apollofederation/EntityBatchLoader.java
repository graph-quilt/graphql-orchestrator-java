package com.intuit.graphql.orchestrator.apollofederation;

import com.intuit.graphql.orchestrator.batch.BatchLoaderExecutionHooks;
import com.intuit.graphql.orchestrator.batch.BatchResultTransformer;
import com.intuit.graphql.orchestrator.batch.DefaultQueryResponseModifier;
import com.intuit.graphql.orchestrator.batch.QueryExecutor;
import com.intuit.graphql.orchestrator.batch.QueryResponseModifier;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.introspection.Introspection;
import graphql.language.Field;
import graphql.language.InlineFragment;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldsContainer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import lombok.Builder;
import org.dataloader.BatchLoader;

/**
 * This class is used to make an entity fetch for a federated subgraph.
 *
 * <p>An entity fetch sends a graphql query for _entities field. i.e.
 *
 * <pre>{@code
 * _entities(representations: [_Any!]!): [_Entity]!
 * }</pre>
 *
 * Using {@link EntityExtensionDefinition#getServiceProvider()}, this class may make a call to the
 * service that made the entity extension(s).
 *
 * <p>Using {@link EntityExtensionDefinition#getBaseServiceProvider()}, this class may make a calls
 * to the base service who owns the entity type. This occurs when the field's selection set contains
 * a field that is owned by base type and cannot be provided by the extended service.
 */
@Builder
public class EntityBatchLoader
    implements BatchLoader<EntityBatchLoadingEnvironment, DataFetcherResult<Object>> {

  // TODO field level authorization

  private final EntityExtensionDefinition entityExtensionDefinition;
  private final BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> hooks;

  private final BatchResultTransformer batchResultTransformer = new EntityBatchResultTransformer();
  private final QueryResponseModifier queryResponseModifier = new DefaultQueryResponseModifier();

  @Override
  public CompletionStage<List<DataFetcherResult<Object>>> load(
      List<EntityBatchLoadingEnvironment> environments) {

    List<DataFetchingEnvironment> dataFetchingEnvironments =
        environments.stream()
            .map(EntityBatchLoadingEnvironment::getDataFetchingEnvironment)
            .collect(Collectors.toList());

    GraphQLContext graphQLContext = dataFetchingEnvironments.get(0).getContext();

    List<InlineFragment> inlineFragments = new ArrayList<>();
    List<Map<String, Object>> representationVariables = new ArrayList<>();
    for (EntityBatchLoadingEnvironment batchLoadingEnvironment : environments) {
      // TODO
      //  using @requires definition in EntityExtensionContext, get the required fields.
      //  if not present in dataFetchingEnvironment.source, make a call back to the base service
      //  Note: @requires definition, @key directive definitions should not be present
      //  in runtime schema
      GraphQLFieldsContainer entityType =
          (GraphQLFieldsContainer)
              batchLoadingEnvironment.getDataFetchingEnvironment().getParentType();
      String entityTypeName = entityType.getName();

      representationVariables.add(
          createEntityRequestRepresentationVariables(entityTypeName, batchLoadingEnvironment));
      inlineFragments.add(
          createEntityRequestInlineFragment(entityTypeName, batchLoadingEnvironment));
    }

    EntityQuery entityQuery =
        EntityQuery.builder()
            .graphQLContext(graphQLContext)
            .inlineFragments(inlineFragments)
            .variables(representationVariables)
            .build();
    // TODO CallBack base service for selected fields not provided
    //  It is possible that the type of the current field is the BaseType and
    //  if the one or more fields in a sub-selection belongs to the BaseType, an _entities() must
    //  be sent to the base service.
    ExecutionInput executionInput = entityQuery.createExecutionInput();
    QueryExecutor queryExecutor = entityExtensionDefinition.getServiceProvider();
    return queryExecutor
        .query(executionInput, graphQLContext)
        .thenApply(queryResponseModifier::modify)
        .thenApply(
            result -> batchResultTransformer.toBatchResult(result, dataFetchingEnvironments));
  }

  private InlineFragment createEntityRequestInlineFragment(
      String entityTypeName, EntityBatchLoadingEnvironment batchLoadingEnvironment) {
    Field originalField = batchLoadingEnvironment.getDataFetchingEnvironment().getField();
    Field __typenameField =
        Field.newField().name(Introspection.TypeNameMetaFieldDef.getName()).build();

    SelectionSet fieldSelectionSet =
        batchLoadingEnvironment
            .getDataFetchingEnvironment()
            .getField()
            .getSelectionSet()
            .transform(builder -> builder.selection(__typenameField));

    InlineFragment.Builder inlineFragmentBuilder = InlineFragment.newInlineFragment();
    inlineFragmentBuilder.typeCondition(TypeName.newTypeName().name(entityTypeName).build());
    inlineFragmentBuilder.selectionSet(
        SelectionSet.newSelectionSet()
            .selection(
                Field.newField()
                    .selectionSet(fieldSelectionSet)
                    .name(originalField.getName())
                    .build())
            .build());
    return inlineFragmentBuilder.build();
  }

  private Map<String, Object> createEntityRequestRepresentationVariables(
      String entityTypeName, EntityBatchLoadingEnvironment batchLoadingEnvironment) {

    Map<String, Object> entityRepresentation = new HashMap<>();
    entityRepresentation.put(Introspection.TypeNameMetaFieldDef.getName(), entityTypeName);

    Map<String, Object> dataSource =
        batchLoadingEnvironment.getDataFetchingEnvironment().getSource();
    EntityExtensionDefinition extensionDefinition =
        batchLoadingEnvironment.getEntityExtensionContext().getThisEntityExtensionDefinition();
    extensionDefinition.getKeyDirectiveDefinitions().stream()
        .flatMap(keyDirectiveDefinition -> keyDirectiveDefinition.getKeyFieldNames().stream())
        .forEach(
            keyFieldName -> entityRepresentation.put(keyFieldName, dataSource.get(keyFieldName)));

    return entityRepresentation;
  }
}
