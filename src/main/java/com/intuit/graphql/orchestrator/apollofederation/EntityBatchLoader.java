package com.intuit.graphql.orchestrator.apollofederation;

import com.intuit.graphql.orchestrator.batch.BatchResultTransformer;
import com.intuit.graphql.orchestrator.batch.DefaultBatchResultTransformer;
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
import org.dataloader.BatchLoader;

public class EntityBatchLoader implements BatchLoader<EntityBatchLoadingEnvironment, DataFetcherResult<Object>>  {

  // TODO
  // private final BatchFieldAuthorization DEFAULT_FIELD_AUTHORIZATION = new DefaultBatchFieldAuthorization();

  //private final QueryExecutor baseServiceQueryExecutor;
  private final QueryExecutor queryExecutor;
  private final BatchResultTransformer batchResultTransformer = new EntityBatchResultTransformer();
  private final QueryResponseModifier queryResponseModifier = new DefaultQueryResponseModifier();
  private final EntityRequestFactory entityRequestFactory = new EntityRequestFactory();

  private final EntityExtensionDefinition entityExtensionDefinition;

  public EntityBatchLoader(EntityExtensionDefinition entityExtensionDefinition) {
    this.entityExtensionDefinition = entityExtensionDefinition;
    this.queryExecutor = entityExtensionDefinition.getServiceMetadata().getServiceProvider();
  }

  @Override
  public CompletionStage<List<DataFetcherResult<Object>>> load(List<EntityBatchLoadingEnvironment> environments) {
    GraphQLContext graphQLContext = environments.get(0).getGraphQLContext();

    EntityQuery entityQuery = new EntityQuery(graphQLContext);

    for (EntityBatchLoadingEnvironment batchLoadingEnvironment : environments) {
      entityQuery.add(batchLoadingEnvironment);
    }

    // TODO
    // It is possible that the type of the current field is the BaseType and
    // if the one or more fields in a sub-selection belongs to the BaseType, an _entities() must
    // be sent to the base service.
    ExecutionInput executionInput = entityQuery.createExecutionInput();
    return this.queryExecutor.query(executionInput, graphQLContext)
        .thenApply(result -> {
          //hooks.onQueryResult(context, result);
          return result;
        })
        .thenApply(queryResponseModifier::modify)
        .thenApply(result -> {
          List<DataFetchingEnvironment> dataFetchingEnvironments = environments.stream()
                  .map(entityBatchLoadingEnvironment -> entityBatchLoadingEnvironment.getDataFetchingEnvironment())
                      .collect(Collectors.toList());
          return batchResultTransformer.toBatchResult(result, dataFetchingEnvironments);
        })
        .thenApply(batchResult -> {
          //hooks.onBatchLoadEnd(context, batchResult);
          return batchResult;
        });
  }

}
