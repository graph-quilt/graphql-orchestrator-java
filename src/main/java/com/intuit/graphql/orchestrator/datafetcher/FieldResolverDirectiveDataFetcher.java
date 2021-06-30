package com.intuit.graphql.orchestrator.datafetcher;

import com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDataLoaderUtil;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;

public class FieldResolverDirectiveDataFetcher implements DataFetcher<Object> {

  private final FieldResolverContext fieldResolverContext;

  private FieldResolverDirectiveDataFetcher(FieldResolverContext fieldResolverContext) {
    this.fieldResolverContext = fieldResolverContext;
  }

  public static FieldResolverDirectiveDataFetcher from(DataFetcherContext dataFetcherContext) {
    Objects.requireNonNull(dataFetcherContext, "DataFetcherContext is required");
    Objects.requireNonNull(dataFetcherContext.getFieldResolverContext(),
        "FieldResolverContext is required");
    return new FieldResolverDirectiveDataFetcher(dataFetcherContext.getFieldResolverContext());
  }

  @Override
  public Object get(final DataFetchingEnvironment dataFetchingEnvironment) {
    return dataFetchingEnvironment
        .getDataLoader(FieldResolverDataLoaderUtil.createDataLoaderKeyFrom(fieldResolverContext))
        .load(dataFetchingEnvironment);
  }

}
