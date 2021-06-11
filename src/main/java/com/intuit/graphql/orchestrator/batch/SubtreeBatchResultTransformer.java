package com.intuit.graphql.orchestrator.batch;

import static graphql.execution.DataFetcherResult.newResult;

import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.execution.ExecutionStepInfo;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class SubtreeBatchResultTransformer implements BatchResultTransformer {

  @Override
  public List<DataFetcherResult<Object>> toBatchResult(final DataFetcherResult<Map<String, Object>> result,
      final List<DataFetchingEnvironment> keys) {

    List<DataFetcherResult<Object>> results = new ArrayList<>();
    final Map<String, Object> data = result.getData();

    for (int i = 0; i < keys.size(); i++) {

      final DataFetchingEnvironment key = keys.get(i);
      Stack<String> dataFetchingPath = buildDataFetchingPath(key.getExecutionStepInfo());

      Object partitionedResult = data;
      while (!dataFetchingPath.isEmpty()) {
        final String step = dataFetchingPath.pop();
        partitionedResult = get(partitionedResult, step);
        if (partitionedResult == null) {
          break;
        }
      }

      /*  Add all errors only once to any of the DataFetcherResult.
       *  Since this is per service graphql-java will collect this properly.
       */
      List<GraphQLError> errors = i == 0 ? result.getErrors() : Collections.emptyList();

      results.add(newResult()
          .data(partitionedResult)
          .errors(errors)
          .build());
    }

    return results;
  }

  private Object get(Object data, String fieldName) {
    if (data == null) {
      return null;
    }

    if (data instanceof Map) {
      return ((Map) data).get(fieldName);
    }

    //todo handle arrays
    return null;
  }

  private Stack<String> buildDataFetchingPath(ExecutionStepInfo leafInfo) {
    Stack<String> hierarchy = new Stack<>();

    ExecutionStepInfo curr = leafInfo;

    //ignore Query/Mutation
    while (curr != null && curr.getPath().getLevel() != 0) {
      final Field field = curr.getField().getSingleField();
      hierarchy.push(field.getAlias() != null ? field.getAlias() : field.getName());
      curr = curr.getParent();
    }

    return hierarchy;
  }
}
