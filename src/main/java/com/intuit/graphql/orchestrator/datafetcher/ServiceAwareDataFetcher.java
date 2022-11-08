package com.intuit.graphql.orchestrator.datafetcher;

import graphql.schema.DataFetcher;

public interface ServiceAwareDataFetcher<T> extends DataFetcher<T> {
  String getNamespace();
  DataFetcherType getDataFetcherType();
}
