package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.getErrors;

import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultBatchResultTransformer implements BatchResultTransformer {

  //since top level, expect one key per environment
  @Override
  public List<DataFetcherResult<Object>> toBatchResult(
      final DataFetcherResult<Map<String, Object>> result,
      final List<DataFetchingEnvironment> keys) {

    return keys.stream()
        .map(key -> toSingleResult(result, key))
        .collect(Collectors.toList());

  }

  public static DataFetcherResult<Object> toSingleResult(DataFetcherResult<Map<String, Object>> result,
      DataFetchingEnvironment environment) {
    final Field field = environment.getField();
    Object data = result.getData().get(field.getAlias() != null ? field.getAlias() : field.getName());
    List<GraphQLError> errors = getErrors(result, field);
    return DataFetcherResult.newResult().data(data).errors(errors).build();
  }
}
