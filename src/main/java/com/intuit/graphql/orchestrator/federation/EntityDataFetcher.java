package com.intuit.graphql.orchestrator.federation;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;

/**
 * This {@link DataFetcher} is used for fields added to an entity type via entity extension. Those
 * fields will be exposed as part of the unified schema and this class should be used to fetch data
 * via an entity fetch. see {@link EntityBatchLoader}
 *
 * <p>The {@link EntityExtensionContext} shall give more metadata about the entity extension which
 * is further used to build a entity query. See {@link EntityQuery}
 */
@RequiredArgsConstructor
public class EntityDataFetcher implements DataFetcher<Object> {

  private final EntityExtensionContext entityExtensionContext; // Field added in entity

  @Override
  public Object get(final DataFetchingEnvironment environment) {
    return environment
        .getDataLoader(entityExtensionContext.getDataLoaderKey())
        .load(EntityBatchLoadingEnvironment.builder()
            .entityExtensionContext(entityExtensionContext)
            .dataFetchingEnvironment(environment)
            .build()
        );
  }

}
