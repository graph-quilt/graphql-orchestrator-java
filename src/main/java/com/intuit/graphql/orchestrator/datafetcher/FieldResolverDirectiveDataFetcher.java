package com.intuit.graphql.orchestrator.datafetcher;

import com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDataLoaderUtil;
import com.intuit.graphql.orchestrator.schema.transform.FieldWithResolverMetadata;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;
import lombok.Getter;

@Getter
public class FieldResolverDirectiveDataFetcher implements DataFetcher<Object>, ServiceContext {

  private final FieldWithResolverMetadata fieldWithResolverMetadata;
  private final String namespace;

  private FieldResolverDirectiveDataFetcher(FieldWithResolverMetadata fieldWithResolverMetadata,
      String namespace) {
    this.fieldWithResolverMetadata = fieldWithResolverMetadata;
    this.namespace = namespace;
  }

  public static FieldResolverDirectiveDataFetcher from(DataFetcherContext dataFetcherContext) {
    Objects.requireNonNull(dataFetcherContext, "DataFetcherContext is required");
    Objects.requireNonNull(dataFetcherContext.getFieldWithResolverMetadata(),
        "FieldWithResolverMetadata is required");
    return new FieldResolverDirectiveDataFetcher(dataFetcherContext.getFieldWithResolverMetadata(),
        dataFetcherContext.getNamespace());
  }

  @Override
  public Object get(final DataFetchingEnvironment dataFetchingEnvironment) {
    return dataFetchingEnvironment
        .getDataLoader(FieldResolverDataLoaderUtil.createDataLoaderKeyFrom(fieldWithResolverMetadata))
        .load(dataFetchingEnvironment);
  }

}
