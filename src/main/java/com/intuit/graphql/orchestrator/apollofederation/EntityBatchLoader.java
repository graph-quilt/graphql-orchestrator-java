package com.intuit.graphql.orchestrator.apollofederation;

import com.intuit.graphql.orchestrator.batch.BatchLoaderExecutionHooks;
import com.intuit.graphql.orchestrator.batch.BatchResultTransformer;
import com.intuit.graphql.orchestrator.batch.DefaultQueryResponseModifier;
import com.intuit.graphql.orchestrator.batch.QueryExecutor;
import com.intuit.graphql.orchestrator.batch.QueryResponseModifier;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
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
 * Using {@link EntityExtensionDefinition#getServiceProvider()}, this class may make a call to
 * the service that made the entity extension(s).
 *
 * Using {@link EntityExtensionDefinition#getBaseServiceProvider()}, this class may make a calls to
 * the base service who owns the entity type.  This occurs when the field's selection set
 * contains a field that is owned by base type and cannot be provided by the extended service.
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

    EntityQuery entityQuery = new EntityQuery(graphQLContext);

    for (EntityBatchLoadingEnvironment batchLoadingEnvironment : environments) {
      // TODO
      //  using @requires definition in EntityExtensionContext, get the required fields.
      //  if not present in dataFetchingEnvironment.source, make a call back to the base service
      //  Note: @requires definition, @key directive definitions should not be present
      //  in runtime schema
      entityQuery.add(batchLoadingEnvironment);
    }

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
}
