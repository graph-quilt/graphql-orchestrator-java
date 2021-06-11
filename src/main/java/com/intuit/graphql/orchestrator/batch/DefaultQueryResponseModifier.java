package com.intuit.graphql.orchestrator.batch;

import static java.util.Collections.emptyList;

import com.intuit.graphql.orchestrator.schema.GraphQLObjects;
import com.intuit.graphql.orchestrator.schema.RawGraphQLError;
import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultQueryResponseModifier implements QueryResponseModifier {

  @Override
  public DataFetcherResult<Map<String, Object>> modify(final Map<String, Object> queryResponse) {
    Map<String, Object> data = Optional.ofNullable(queryResponse.get("data"))
        .map(GraphQLObjects::<Map<String, Object>>cast)
        .orElse(Collections.emptyMap());
    final List<Map> errorsMap = Optional.ofNullable(queryResponse.get("errors"))
        .map(GraphQLObjects::<List<Map>>cast)
        .orElse(emptyList());

    List<GraphQLError> errors = errorsMap.stream()
        .map(val -> Optional.ofNullable(val)
            .map(GraphQLObjects::<Map<String, Object>>cast)
            .orElseThrow(IllegalArgumentException::new))
        .map(RawGraphQLError::new)
        .collect(Collectors.toList());

    return DataFetcherResult.<Map<String, Object>>newResult()
        .data(data)
        .errors(errors)
        .build();
  }
}
