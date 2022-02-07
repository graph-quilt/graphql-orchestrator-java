package com.intuit.graphql.orchestrator.apollofederation;

import com.intuit.graphql.orchestrator.batch.BatchResultTransformer;
import com.intuit.graphql.orchestrator.batch.QueryExecutor;
import com.intuit.graphql.orchestrator.batch.QueryResponseModifier;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import org.dataloader.BatchLoader;

@RequiredArgsConstructor
public class EntityBatchLoader implements BatchLoader<DataFetchingEnvironment, DataFetcherResult<Object>>  {

  // TODO
  // private final BatchFieldAuthorization DEFAULT_FIELD_AUTHORIZATION = new DefaultBatchFieldAuthorization();

  private final QueryExecutor queryExecutor;
  private final BatchResultTransformer batchResultTransformer;
  private final QueryResponseModifier queryResponseModifier;
  private final EntityRequestFactory entityRequestFactory;

  @Override
  public CompletionStage<List<DataFetcherResult<Object>>> load(List<DataFetchingEnvironment> environments) {
    GraphQLContext graphQLContext = environments.get(0).getContext();
    SubGraphQueryPlan subGraphQueryPlan = new SubGraphQueryPlan();
    for (DataFetchingEnvironment dataFetchingEnvironment : environments) {
        subGraphQueryPlan.add(entityRequestFactory.createFrom(dataFetchingEnvironment));
    }

    ExecutionInput executionInput = subGraphQueryPlan.createExectionInput();
    return this.queryExecutor.query(executionInput, graphQLContext)
        .thenApply(result -> {
          //hooks.onQueryResult(context, result);
          return result;
        })
        .thenApply(queryResponseModifier::modify)
        .thenApply(result -> batchResultTransformer.toBatchResult(result, environments))
        .thenApply(batchResult -> {
          //hooks.onBatchLoadEnd(context, batchResult);
          return batchResult;
        });
  }

}
