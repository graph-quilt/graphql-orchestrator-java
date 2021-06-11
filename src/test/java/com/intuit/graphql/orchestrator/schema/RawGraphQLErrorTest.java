package com.intuit.graphql.orchestrator.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import graphql.ErrorType;
import graphql.language.SourceLocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class RawGraphQLErrorTest {

  private Map<String, Object> rawGraphQLError;
  private List<Map> locations;
  private List<Object> path;
  private Map<String, Object> extensions;

  private Map<String, Number> location;

  @Before
  public void setUp() {
    this.rawGraphQLError = new HashMap<>();
    this.locations = new ArrayList<>();
    this.path = new ArrayList<>();
    this.extensions = new HashMap<>();
    this.location = new HashMap<>();
  }

  @Test
  public void shouldExtractEverything() {
    location.put("line", 2);
    location.put("column", 3L);
    locations.add(location);

    path.addAll(Arrays.asList("some", "path", 1, 2L, "field"));

    extensions.put("detailed", "message");

    rawGraphQLError.put("message", "boom");
    rawGraphQLError.put("locations", locations);
    rawGraphQLError.put("path", path);
    rawGraphQLError.put("extensions", extensions);

    final RawGraphQLError rawGraphQLError = new RawGraphQLError(this.rawGraphQLError);

    assertThat(rawGraphQLError.getMessage()).isEqualTo("boom");
    assertThat(rawGraphQLError.getLocations()).hasSize(1)
        .extracting(SourceLocation::getLine, SourceLocation::getColumn).containsOnly(tuple(2, 3));
    assertThat(rawGraphQLError.getPath()).hasSize(5)
        .containsExactly("some", "path", 1, 2L, "field");
    assertThat(rawGraphQLError.getExtensions().get("detailed")).isEqualTo("message");
    assertThat(rawGraphQLError.getErrorType()).isEqualTo(ErrorType.DataFetchingException);
  }

  @Test
  public void shouldProvideDefaultValuesForMissingErrorFields() {
    final RawGraphQLError rawGraphQLError = new RawGraphQLError(this.rawGraphQLError);

    assertThat(rawGraphQLError.getMessage()).isEqualTo("Unknown error");
    assertThat(rawGraphQLError.getErrorType()).isEqualTo(ErrorType.DataFetchingException);
    assertThat(rawGraphQLError.getLocations()).isNull();
    assertThat(rawGraphQLError.getPath()).isNull();
    assertThat(rawGraphQLError.getExtensions()).isNull();
  }
}