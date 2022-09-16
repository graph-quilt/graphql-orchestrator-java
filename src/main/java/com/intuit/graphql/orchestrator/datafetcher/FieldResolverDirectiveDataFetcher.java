package com.intuit.graphql.orchestrator.datafetcher;

import com.intuit.graphql.orchestrator.batch.DataLoaderKeyUtil;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;
import lombok.Getter;

@Getter
public class FieldResolverDirectiveDataFetcher implements ServiceAwareDataFetcher<Object> {

  private final FieldResolverContext fieldResolverContext;
  private final String namespace;

  private FieldResolverDirectiveDataFetcher(FieldResolverContext fieldResolverContext,
      String namespace) {
    this.fieldResolverContext = fieldResolverContext;
    this.namespace = namespace;
  }

  public static FieldResolverDirectiveDataFetcher from(DataFetcherContext dataFetcherContext) {
    Objects.requireNonNull(dataFetcherContext, "DataFetcherContext is required");
    Objects.requireNonNull(dataFetcherContext.getFieldResolverContext(),
        "FieldResolverContext is required");
    return new FieldResolverDirectiveDataFetcher(dataFetcherContext.getFieldResolverContext(),
        dataFetcherContext.getNamespace());
  }

  @Override
  public Object get(final DataFetchingEnvironment dataFetchingEnvironment) {
    return dataFetchingEnvironment
        .getDataLoader(DataLoaderKeyUtil.createDataLoaderKeyFrom(fieldResolverContext))
        .load(dataFetchingEnvironment);
  }

}
