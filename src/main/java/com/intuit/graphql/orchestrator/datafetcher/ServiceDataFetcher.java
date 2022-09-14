package com.intuit.graphql.orchestrator.datafetcher;

import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.Getter;

@Getter
public class ServiceDataFetcher implements DataFetcher, ServiceContext {

  /**
   * One of Query or Mutation or Subscription
   */
  private final ServiceMetadata serviceMetadata;
  private final String namespace;


  public ServiceDataFetcher(ServiceMetadata serviceMetadata, String namespace) {
    this.serviceMetadata = serviceMetadata;
    this.namespace = namespace;
  }

  @Override
  public Object get(final DataFetchingEnvironment environment) {
    String dfeFieldName = environment.getField().getName();
    GraphQLContext context = environment.getContext();
    context.put(dfeFieldName, this.serviceMetadata);
    return environment
        .getDataLoader(serviceMetadata.getServiceProvider().getNameSpace())
        .load(environment);
  }
}
