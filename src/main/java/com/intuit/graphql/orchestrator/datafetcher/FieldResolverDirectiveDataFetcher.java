package com.intuit.graphql.orchestrator.datafetcher;

import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.graphql.orchestrator.batch.DataLoaderKeyUtil;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;
import lombok.Getter;

@Getter
public class FieldResolverDirectiveDataFetcher implements ServiceAwareDataFetcher<Object> {

  private final FieldResolverContext fieldResolverContext;
  private final String namespace;
  private final ServiceType serviceType;

  private FieldResolverDirectiveDataFetcher(FieldResolverContext fieldResolverContext,
      String namespace, ServiceType serviceType) {
    this.fieldResolverContext = fieldResolverContext;
    this.namespace = namespace;
    this.serviceType = serviceType;
  }

  public static FieldResolverDirectiveDataFetcher from(DataFetcherContext dataFetcherContext) {
    Objects.requireNonNull(dataFetcherContext, "DataFetcherContext is required");
    Objects.requireNonNull(dataFetcherContext.getFieldResolverContext(),
        "FieldResolverContext is required");
    return new FieldResolverDirectiveDataFetcher(dataFetcherContext.getFieldResolverContext(),
        dataFetcherContext.getNamespace(), dataFetcherContext.getServiceType());
  }

  @Override
  public Object get(final DataFetchingEnvironment dataFetchingEnvironment) {
    return dataFetchingEnvironment
        .getDataLoader(DataLoaderKeyUtil.createDataLoaderKeyFrom(fieldResolverContext))
        .load(dataFetchingEnvironment);
  }

  @Override
  public DataFetcherType getDataFetcherType() {
    return DataFetcherType.RESOLVER_ON_FIELD_DEFINITION;
  }
}
