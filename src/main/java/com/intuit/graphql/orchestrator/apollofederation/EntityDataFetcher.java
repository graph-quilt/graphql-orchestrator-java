package com.intuit.graphql.orchestrator.apollofederation;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.Data;

/**
 * This {@link DataFetcher} is used for sending entity data request to a subgraph using
 * <pre>
 * {@code
 * query {
 *    _entities(...) {...}}
 * }
 * </pre>
 */
@Data
public class EntityDataFetcher implements DataFetcher<Object> {

  private EntityExtensionContext entityExtensionContext;

  @Override
  public Object get(final DataFetchingEnvironment environment) {
    return environment
        .getDataLoader(entityExtensionContext.createDataLoaderKey())
        .load(EntityBatchLoadingEnvironment.builder()
            .graphQLContext(environment.getContext())
            .entityExtensionContext(entityExtensionContext)
            .dataFetchingEnvironment(environment)
            .build()
        );
  }

}
