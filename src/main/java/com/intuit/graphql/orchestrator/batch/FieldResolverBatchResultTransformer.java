package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.createAlias;
import static com.intuit.graphql.orchestrator.utils.ExecutionPathUtils.graphQLErrorPathStartsWith;

import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.execution.ExecutionPath;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;

@AllArgsConstructor
public class FieldResolverBatchResultTransformer implements BatchResultTransformer {

  private static final String GRAPHQL_ERROR_MESSAGE = "FieldResolverDirectiveDataFetcher "
      + "encountered an error while calling downstream service.";

  private final String[] resolverSelectedFields;
  private final ExecutionPath resolverExecutionPath;
  private final FieldResolverContext fieldResolverContext;


  public FieldResolverBatchResultTransformer(String[] resolverSelectedFields,
      FieldResolverContext fieldResolverContext) {

    if (ArrayUtils.isEmpty(resolverSelectedFields)) {
      throw new IllegalArgumentException("resolverSelectedFields is empty");
    }

    this.resolverSelectedFields = resolverSelectedFields;
    this.resolverExecutionPath = ExecutionPath.fromList(Arrays.asList(resolverSelectedFields));

    this.fieldResolverContext = fieldResolverContext;
  }

  @Override
  public List<DataFetcherResult<Object>> toBatchResult(DataFetcherResult<Map<String, Object>> dataFetcherResult,
      List<DataFetchingEnvironment> dataFetchingEnvironments) {

    List<DataFetcherResult<Object>> dataFetcherResults = new ArrayList<>();

    for (int i = 0; i < CollectionUtils.size(dataFetchingEnvironments); i++) {
      DataFetchingEnvironment dataFetchingEnvironment = dataFetchingEnvironments.get(i);

      Object pathData = getDataFromBatchResult(dataFetcherResult.getData(), i);
      List<GraphQLError> pathErrors = getErrorsFromBatchResult(dataFetcherResult.getErrors(), dataFetchingEnvironment, i);

      dataFetcherResults.add(DataFetcherResult.newResult()
          .data(pathData)
          .errors(pathErrors)
          .build());
    }

    return dataFetcherResults;
  }

  private Object getDataFromBatchResult(Map<String, Object> batchData, int aliasCounter) {
    int lastIndex = resolverSelectedFields.length - 1;

    Map<String, Object> tempMap = batchData;
    for (int i = 0; i < lastIndex; i++) {
      tempMap = (Map<String, Object>) batchData.get(resolverSelectedFields[i]);
    }
    String alias = createAlias(resolverSelectedFields[lastIndex], aliasCounter);
    return tempMap.get(alias);
  }

  private List<GraphQLError> getErrorsFromBatchResult(List<GraphQLError> batchErrors,
      DataFetchingEnvironment dfe, int aliasCounter) {

    int lastIndex = resolverSelectedFields.length - 1;
    String leafFieldName = resolverSelectedFields[lastIndex];

    ExecutionPath aliasedResolverExecutionPath = resolverExecutionPath
        .replaceSegment(createAlias(leafFieldName, aliasCounter));

    List<GraphQLError> errorsWithoutPath = Collections.emptyList();
    if (aliasCounter == 0) {
      errorsWithoutPath = batchErrors
          .stream()
          .filter(graphQLError -> CollectionUtils.isEmpty(graphQLError.getPath()))  // Ideally this shouldn't happen. The specs requires errors to have path if can be associated in the field.
          .map(this::mapErrorToPath)
          .collect(Collectors.toList());
    }

    List<GraphQLError> mappedErrors = batchErrors.stream()
        .filter(graphQLError -> graphQLErrorPathStartsWith(graphQLError, aliasedResolverExecutionPath))
        .map(graphQLError -> mapErrorToPath(graphQLError, dfe.getExecutionStepInfo().getPath()))
        .collect(Collectors.toList());

    return ListUtils.union(mappedErrors, errorsWithoutPath);
  }

  private GraphQLError mapErrorToPath(GraphQLError graphQLError) {
    return mapErrorToPath(graphQLError, ExecutionPath.fromList(Collections.emptyList()));
  }

  private GraphQLError mapErrorToPath(GraphQLError graphQLError, ExecutionPath dfeExecutionPath) {
    final Map<String, Object> extensions = new HashMap<>();
    extensions.put("serviceNamespace", fieldResolverContext.getServiceNamespace());
    extensions.put("parentTypename", fieldResolverContext.getParentTypename());
    extensions.put("fieldName", fieldResolverContext.getFieldName());
    extensions.put("downstreamErrors", graphQLError.toSpecification());

    return GraphqlErrorBuilder.newError()
        .message(GRAPHQL_ERROR_MESSAGE)
        .path(dfeExecutionPath)
        .errorType(graphQLError.getErrorType())
        .extensions(extensions)
        .build();
  }

}
