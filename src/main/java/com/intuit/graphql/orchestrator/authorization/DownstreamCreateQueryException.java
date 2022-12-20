package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A GraphqlErrorException that should be returned as part of data fetcher result or
 * thrown by data fetcher/loader if the query for a downstream service was not created.
 */
public class DownstreamCreateQueryException extends GraphqlErrorException {

  private DownstreamCreateQueryException(DownstreamCreateQueryException.Builder builder) {
    super(builder);
  }

  public static DownstreamCreateQueryException.Builder builder() {
    return new DownstreamCreateQueryException.Builder();
  }

  public static class Builder extends GraphqlErrorException
      .BuilderBase<DownstreamCreateQueryException.Builder, DownstreamCreateQueryException> {

    { // instance initializer
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
  }
}