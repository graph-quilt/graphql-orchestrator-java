package com.intuit.graphql.orchestrator.batch;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface BatchResultTransformer {

  List<DataFetcherResult<Object>> toBatchResult(DataFetcherResult<Map<String, Object>> result,
      List<DataFetchingEnvironment> keys);

}
