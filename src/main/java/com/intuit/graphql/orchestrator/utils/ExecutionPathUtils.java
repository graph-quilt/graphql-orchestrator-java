package com.intuit.graphql.orchestrator.utils;

import graphql.GraphQLError;
import graphql.execution.ExecutionPath;
import java.util.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class ExecutionPathUtils {

  private ExecutionPathUtils(){}

  public static boolean graphQLErrorPathStartsWith(GraphQLError graphQLError, ExecutionPath aliasedResolverExecutionPath) {
    Objects.requireNonNull(graphQLError);
    Objects.requireNonNull(aliasedResolverExecutionPath);

    if (CollectionUtils.isEmpty(graphQLError.getPath())) {
      return false;
    }
    ExecutionPath graphQLErrorExecutionPath = ExecutionPath.fromList(graphQLError.getPath());
    return pathStartsWith(graphQLErrorExecutionPath, aliasedResolverExecutionPath);
  }

  public static boolean pathStartsWith(ExecutionPath pathToTest, ExecutionPath startPath) {
    Objects.requireNonNull(pathToTest);
    Objects.requireNonNull(startPath);

    return StringUtils.startsWith(pathToTest.toString(), startPath.toString());
  }

}
