package com.intuit.graphql.orchestrator.batch;

import graphql.execution.DataFetcherResult;
import java.util.Map;

@FunctionalInterface
public interface QueryResponseModifier {

  DataFetcherResult<Map<String, Object>> modify(Map<String, Object> queryResponse);

}
