package com.intuit.graphql.orchestrator.apollofederation;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EntityBatchLoadingEnvironment {
  private DataFetchingEnvironment dataFetchingEnvironment;
  private EntityExtensionContext entityExtensionContext;
  private GraphQLContext graphQLContext;
}
