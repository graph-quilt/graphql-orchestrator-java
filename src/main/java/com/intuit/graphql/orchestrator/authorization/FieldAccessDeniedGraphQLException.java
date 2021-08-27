package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.utils.ExecutionPathUtils;
import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphqlErrorException;
import graphql.execution.ExecutionPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;

public class FieldAccessDeniedGraphQLException extends GraphqlErrorException {

  private FieldAccessDeniedGraphQLException(FieldAccessDeniedGraphQLException.Builder builder) {
    super(builder);
  }

  @Override
  public ErrorClassification getErrorType() {
    return ErrorType.DataFetchingException;
  }

  public static FieldAccessDeniedGraphQLException.Builder builder() {
    return new FieldAccessDeniedGraphQLException.Builder();
  }

  public static class Builder extends GraphqlErrorException
      .BuilderBase<FieldAccessDeniedGraphQLException.Builder, FieldAccessDeniedGraphQLException> {

    {
      super.extensions = new HashMap<>();
      super.sourceLocations = new ArrayList();
    }

    public FieldAccessDeniedGraphQLException build() {
      return new FieldAccessDeniedGraphQLException(this);
    }

    public FieldAccessDeniedGraphQLException.Builder extension(String key, Object value) {
      this.extensions.put(key, value);
      return this;
    }

    public FieldAccessDeniedGraphQLException.Builder declinedFields(List<DeclinedField> declinedFields) {
      if (CollectionUtils.isNotEmpty(declinedFields)) {
        List<String> fieldPaths = declinedFields.stream()
            .map(declinedField -> ExecutionPath
                .fromList(declinedField.getPathList()).toString() + declinedField.getField().getName())
            .collect(Collectors.toList());
        extension("declinedFields", fieldPaths);
      }

      return this;
    }

    @Override
    public FieldAccessDeniedGraphQLException.Builder extensions(Map<String, Object> extensions) {
      this.extensions.putAll(extensions);
      return this;
    }
  }
}
