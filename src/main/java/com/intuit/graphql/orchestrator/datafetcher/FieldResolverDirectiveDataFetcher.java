package com.intuit.graphql.orchestrator.datafetcher;

import com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDataLoaderUtil;
import com.intuit.graphql.orchestrator.schema.transform.FieldWithResolverMetadata;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;

public class FieldResolverDirectiveDataFetcher implements DataFetcher<Object> {

  private final FieldWithResolverMetadata fieldWithResolverMetadata;

  private FieldResolverDirectiveDataFetcher(FieldWithResolverMetadata fieldWithResolverMetadata) {
    this.fieldWithResolverMetadata = fieldWithResolverMetadata;
  }

  public static FieldResolverDirectiveDataFetcher from(DataFetcherContext dataFetcherContext) {
    Objects.requireNonNull(dataFetcherContext, "DataFetcherContext is required");
    Objects.requireNonNull(dataFetcherContext.getFieldWithResolverMetadata(),
        "FieldWithResolverMetadata is required");
    return new FieldResolverDirectiveDataFetcher(dataFetcherContext.getFieldWithResolverMetadata());
  }

  @Override
  public Object get(final DataFetchingEnvironment dataFetchingEnvironment) {
    return dataFetchingEnvironment
        .getDataLoader(FieldResolverDataLoaderUtil.createDataLoaderKeyFrom(fieldWithResolverMetadata))
        .load(dataFetchingEnvironment);
  }

}
