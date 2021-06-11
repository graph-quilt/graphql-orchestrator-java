package com.intuit.graphql.orchestrator.testhelpers;

import static com.intuit.graphql.orchestrator.testhelpers.JsonTestUtils.toJson;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import graphql.ExecutionResult;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.AbstractAssert;

public class ExecutionResultAssert extends AbstractAssert<ExecutionResultAssert, ExecutionResult> {

  private final DocumentContext documentContext;

  public ExecutionResultAssert(ExecutionResult executionResult) {
    super(executionResult, ExecutionResultAssert.class);

    Map<String, Object> resultInGraphQLSpecification = executionResult.toSpecification();

    String resultInGraphQLSpecificationAsString = toJson(resultInGraphQLSpecification);

    Configuration jsonPathConf = Configuration.defaultConfiguration();
    documentContext = JsonPath.using(jsonPathConf).parse(resultInGraphQLSpecificationAsString);
  }

  public void hasNoErrors() {
    isNotNull();
    assertThat(actual.getErrors()).isEmpty();
  }

  public void hasErrors() {
    isNotNull();
    assertThat(actual.getErrors()).isNotEmpty();
  }

  public void hasData() {
    isNotNull();
    //noinspection unchecked
    assertThat((Map<String, Object>)actual.getData()).isNotEmpty();
  }

  public void pathEquals(String jsonPathCriteria, String expectedValue) {
    isNotNull();

    String actualStringvalue = documentContext.read(jsonPathCriteria);

    assertThat(actualStringvalue).isEqualTo(expectedValue);
  }

  public void pathContains(String jsonPathCriteria, String expectedValue) {
    isNotNull();

    String actualStringvalue = documentContext.read(jsonPathCriteria);

    assertThat(actualStringvalue).contains(expectedValue);
  }

  public void pathHasArraySize(String jsonPathCriteria, int expectedSize) {
    isNotNull();

    List<Object> list = documentContext.read(jsonPathCriteria);

    assertThat(list).hasSize(expectedSize);
  }

  public void pathContainsKeys(String jsonPathCriteria, String... keys) {
    isNotNull();

    Map<String, Object> objectMap = documentContext.read(jsonPathCriteria);

    assertThat(objectMap).containsKeys(keys);
  }

  public void pathIsNull(String jsonPathCriteria) {
    isNotNull();

    Object objectMap = documentContext.read(jsonPathCriteria);

    assertThat(objectMap).isNull();
  }

  public void pathIsNotFound(String jsonPathCriteria) {
    isNotNull();

    try {
      Object objectMap = documentContext.read(jsonPathCriteria);
      assertThat(objectMap).isNull();
    } catch(Exception e) {
      assertThat(e).isInstanceOf(PathNotFoundException.class);
    }
  }
}
