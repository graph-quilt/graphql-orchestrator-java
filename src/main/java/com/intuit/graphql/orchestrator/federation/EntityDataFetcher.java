package com.intuit.graphql.orchestrator.federation;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;

import static com.intuit.graphql.orchestrator.batch.DataLoaderKeyUtil.createDataLoaderKey;

/**
 * This class is used for resolving fields added to an Entity by making an entity fetch request. To
 * build the entity fetch query, it uses {@link EntityQuery}.
 */
@RequiredArgsConstructor
public class EntityDataFetcher implements DataFetcher<CompletableFuture<Object>> {
  private final String entityName;

  @Override
  public CompletableFuture<Object> get(final DataFetchingEnvironment dataFetchingEnvironment) {
    String batchLoaderKey = createDataLoaderKey(entityName, dataFetchingEnvironment.getField().getName());

    return dataFetchingEnvironment
            .getDataLoader(batchLoaderKey)
            .load(dataFetchingEnvironment);
  }
}
