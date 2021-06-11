package com.intuit.graphql.orchestrator.schema;

import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Converts a raw GraphQL error object into a {@link GraphQLError}
 */
@SuppressWarnings("squid:S1948") // suppress non-serializable interface usage on Serializable class
public class RawGraphQLError implements GraphQLError {

  private static final ErrorType errorType = ErrorType.DataFetchingException;
  private final String message;
  private final List<SourceLocation> locations;
  private final List<Object> path;
  private final Map<String, Object> extensions;

  public RawGraphQLError(Map<String, Object> error) {
    this.message = parseMessage(error);
    this.locations = parseLocations(error);
    this.path = parsePath(error);
    this.extensions = parseExtensions(error);
  }

  private static Integer parseInt(Object value) {
    if (value instanceof Integer) {
      return (Integer) value;
    } else if (value instanceof Long) {
      return ((Long) value).intValue();
    } else {
      return -1;
    }
  }

  private Map<String, Object> parseExtensions(final Map<String, Object> error) {
    return Optional.ofNullable(error.get("extensions"))
        .map(GraphQLObjects::<Map<String, Object>>cast)
        .orElse(null);
  }

  private List<Object> parsePath(final Map<String, Object> error) {
    return Optional.ofNullable(error.get("path"))
        .map(GraphQLObjects::<List<Object>>cast)
        .orElse(null);
  }

  private List<SourceLocation> parseLocations(final Map<String, Object> error) {
    return Optional.ofNullable(error.get("locations"))
        .map(GraphQLObjects::<List<Map>>cast)
        .map(errors -> errors.stream()
            .filter(err -> err.containsKey("line") && err.containsKey("column"))
            .map(err -> new SourceLocation(parseInt(err.get("line")), parseInt(err.get("column"))))
            .collect(Collectors.toList()))
        .orElse(null);
  }

  private String parseMessage(Map<String, Object> rawError) {
    return Optional.ofNullable(rawError.get("message")).map(String.class::cast).orElse("Unknown error");
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public List<SourceLocation> getLocations() {
    return locations;
  }

  @Override
  public ErrorClassification getErrorType() {
    return errorType;
  }

  @Override
  public List<Object> getPath() {
    return path;
  }

  @Override
  public Map<String, Object> getExtensions() {
    return extensions;
  }
}
