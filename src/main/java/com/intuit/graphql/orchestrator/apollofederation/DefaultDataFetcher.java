package com.intuit.graphql.orchestrator.apollofederation;

import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.Data;

/**
 * This {@link DataFetcher} is used to fetch fields for an Apollo Federation compliant sub-graph
 */
@Data
public class DefaultDataFetcher implements DataFetcher<Object> {

  private FieldFetchContext fieldFetchContext;

  @Override
  public Object get(final DataFetchingEnvironment environment) {
    String dfeFieldName = environment.getField().getName();
    GraphQLContext context = environment.getContext();
    return environment
        .getDataLoader(fieldFetchContext.createDataLoaderKey())
        .load(environment);
  }

}
