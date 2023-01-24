package com.intuit.graphql.orchestrator.datafetcher;

import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType;
import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;

@Getter
public class ServiceDataFetcher implements ServiceAwareDataFetcher {

  /**
   * One of Query or Mutation or Subscription
   */
  private final ServiceMetadata serviceMetadata;


  public ServiceDataFetcher(ServiceMetadata serviceMetadata) {
    this.serviceMetadata = serviceMetadata;
  }

  @Override
  public Object get(final DataFetchingEnvironment environment) {
    String dfeFieldName = environment.getField().getName();
    GraphQLContext context = environment.getContext();
    context.put(dfeFieldName, this.serviceMetadata);
    CompletableFuture r = environment
        .getDataLoader(serviceMetadata.getServiceProvider().getNameSpace())
        .load(environment);
    return r;
  }

  @Override
  public String getNamespace() {
    return this.serviceMetadata.getServiceProvider().getNameSpace();
  }

  @Override
  public DataFetcherType getDataFetcherType() {
    return DataFetcherType.SERVICE;
  }

  @Override
  public ServiceType getServiceType() {
    return this.serviceMetadata.getServiceProvider().getSeviceType();
  }
}
