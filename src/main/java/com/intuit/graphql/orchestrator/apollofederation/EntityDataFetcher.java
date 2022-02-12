package com.intuit.graphql.orchestrator.apollofederation;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;

/**
 * This {@link DataFetcher} is used for sending entity data request to a subgraph using
 * <pre>
 * {@code
 * query {
 *    _entities(...) {...}}
 * }
 * </pre>
 */
@RequiredArgsConstructor
public class EntityDataFetcher implements DataFetcher<Object> {

  private final EntityExtensionContext entityExtensionContext; // Field added in entity

  @Override
  public Object get(final DataFetchingEnvironment environment) {
    return environment
        .getDataLoader(entityExtensionContext.createDataLoaderKey())
        .load(EntityBatchLoadingEnvironment.builder()
            .entityExtensionContext(entityExtensionContext)
            .dataFetchingEnvironment(environment)
            .build()
        );
  }

}
