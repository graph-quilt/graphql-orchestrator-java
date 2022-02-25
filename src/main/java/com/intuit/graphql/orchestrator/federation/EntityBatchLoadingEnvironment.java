package com.intuit.graphql.orchestrator.federation;

import graphql.schema.DataFetchingEnvironment;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EntityBatchLoadingEnvironment {
  private DataFetchingEnvironment dataFetchingEnvironment;
  private EntityExtensionContext entityExtensionContext;
}

