package com.intuit.graphql.orchestrator.datafetcher;

import com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType;
import graphql.schema.DataFetcher;

public interface ServiceAwareDataFetcher<T> extends DataFetcher<T> {
  String getNamespace();
  DataFetcherType getDataFetcherType();
}
