package com.intuit.graphql.orchestrator.fieldresolver;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ValueTemplateTest {

  private Map<String, Object> testDataSource = new HashMap<>();

  @Test
  public void compile_SimpleVariable_success() {
    String valueTemplateStr = "$someVar";
    ValueTemplate subjectUnderTest = new ValueTemplate(valueTemplateStr);
    testDataSource.put("someVar", "TEST_VALUE");

    String actual = subjectUnderTest.compile(testDataSource);

    assertThat(actual).isEqualTo("TEST_VALUE");
  }

  @Test
  public void compile_jsonStringWithVariable_success() {
    String valueTemplateStr = "{ id : \"$someVar\" }";
    ValueTemplate subjectUnderTest = new ValueTemplate(valueTemplateStr);
    testDataSource.put("someVar", "TEST_VALUE");

    String actual = subjectUnderTest.compile(testDataSource);

    assertThat(actual).isEqualTo("{ id : \"TEST_VALUE\" }");
  }

}
