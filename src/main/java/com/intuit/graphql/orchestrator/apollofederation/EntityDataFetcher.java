package com.intuit.graphql.orchestrator.apollofederation;

import graphql.GraphQLContext;
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

  private SubGraphContext subGraphContext;

  @Override
  public Object get(final DataFetchingEnvironment environment) {
    String dfeFieldName = environment.getField().getName();
    GraphQLContext context = environment.getContext();
    context.put(dfeFieldName, subGraphContext);
    return environment
        .getDataLoader(subGraphContext.getServiceMetadata().getServiceProvider().getNameSpace())
        .load(environment);
  }

}
