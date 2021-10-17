package com.intuit.graphql.orchestrator.authorization;

import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphqlErrorException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DownstreamCreateQueryException extends GraphqlErrorException {

  private DownstreamCreateQueryException(DownstreamCreateQueryException.Builder builder) {
    super(builder);
  }

  @Override
  public ErrorClassification getErrorType() {
    return ErrorType.DataFetchingException;
  }

  public static DownstreamCreateQueryException.Builder builder() {
    return new DownstreamCreateQueryException.Builder();
  }

  public static class Builder extends GraphqlErrorException
      .BuilderBase<DownstreamCreateQueryException.Builder, DownstreamCreateQueryException> {
    {
      // instance initializer
      super.extensions = new HashMap<>();
      super.sourceLocations = new ArrayList<>();
    }

    public DownstreamCreateQueryException build() {
      return new DownstreamCreateQueryException(this);
    }

    public DownstreamCreateQueryException.Builder extension(String key, Object value) {
      this.extensions.put(key, value);
      return this;
    }

    @Override
    public DownstreamCreateQueryException.Builder extensions(Map<String, Object> extensions) {
      this.extensions.putAll(extensions);
      return this;
    }

  }
}
