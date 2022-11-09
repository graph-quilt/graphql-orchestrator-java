package com.intuit.graphql.orchestrator.federation;

import static com.intuit.graphql.orchestrator.batch.DataLoaderKeyUtil.createDataLoaderKey;

import com.intuit.graphql.orchestrator.datafetcher.ServiceAwareDataFetcher;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;

/**
 * This class is used for resolving fields added to an Entity by making an entity fetch request. To
 * build the entity fetch query, it uses {@link EntityQuery}.
 */
@RequiredArgsConstructor
public class EntityDataFetcher implements ServiceAwareDataFetcher<CompletableFuture<Object>> {
  private final String entityName;
  private final String namespace;

  @Override
  public CompletableFuture<Object> get(final DataFetchingEnvironment dataFetchingEnvironment) {
    String batchLoaderKey = createDataLoaderKey(entityName, dataFetchingEnvironment.getField().getName());

    return dataFetchingEnvironment
            .getDataLoader(batchLoaderKey)
            .load(dataFetchingEnvironment);
  }

  @Override
  public String getNamespace() {
    return this.namespace;
  }

  @Override
  public DataFetcherType getDataFetcherType() {
    return DataFetcherType.ENTITY_FETCHER;
  }
}
