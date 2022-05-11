package com.intuit.graphql.orchestrator.fieldresolver;

import com.google.common.collect.Sets;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

public class ValueTemplateTest {

  private Map<String, Object> testDataSource;
  private FieldResolverContext fieldResolverContextMock;

  @Before
  public void init() {
    fieldResolverContextMock = Mockito.mock(FieldResolverContext.class);
    testDataSource = new HashMap<>();
  }

  @Test
  public void compile_SimpleVariable_success() {
    String valueTemplateStr = "$someVar";
    ValueTemplate subjectUnderTest = new ValueTemplate(fieldResolverContextMock, valueTemplateStr);

    Set<String> requiredFields = Sets.newHashSet("someVar");
    when(fieldResolverContextMock.getRequiredFields()).thenReturn(requiredFields);

    testDataSource.put("someVar", "TEST_VALUE");

    String actual = subjectUnderTest.compile(testDataSource);

    assertThat(actual).isEqualTo("TEST_VALUE");
  }

  @Test
  public void compile_jsonStringWithVariable_success() {
    String valueTemplateStr = "{ id : \"$someVar\" }";
    ValueTemplate subjectUnderTest = new ValueTemplate(fieldResolverContextMock, valueTemplateStr);
    testDataSource.put("someVar", "TEST_VALUE");

    Set<String> requiredFields = Sets.newHashSet("someVar");
    when(fieldResolverContextMock.getRequiredFields()).thenReturn(requiredFields);

    String actual = subjectUnderTest.compile(testDataSource);

    assertThat(actual).isEqualTo("{ id : \"TEST_VALUE\" }");
  }

  @Test
  public void compile_jsonStringWithNullVariable_success() {
    String valueTemplateStr = "{ id : \"$someVar\" }";
    ValueTemplate subjectUnderTest = new ValueTemplate(fieldResolverContextMock, valueTemplateStr);
    testDataSource.put("someVar", null);

    Set<String> requiredFields = Sets.newHashSet("someVar");
    when(fieldResolverContextMock.getRequiredFields()).thenReturn(requiredFields);

    String actual = subjectUnderTest.compile(testDataSource);
    assertThat(actual).isEqualTo("{ id : null }");
  }
  @Test
  public void compile_jsonStringWithMultipleVariables_success() {
    String valueTemplateStr = "{ id : \"$petId\" name : \"$petName\" }";
    ValueTemplate subjectUnderTest = new ValueTemplate(fieldResolverContextMock, valueTemplateStr);
    testDataSource.put("petId", "pet-901");
    testDataSource.put("petName", null);

    Set<String> requiredFields = Sets.newHashSet("petId", "petName");
    when(fieldResolverContextMock.getRequiredFields()).thenReturn(requiredFields);

    String actual = subjectUnderTest.compile(testDataSource);

    assertThat(actual).isEqualTo("{ id : \"pet-901\" name : null }");
  }

  @Test
  public void compile_jsonStringWithMultipleVariablesNotString_success() {
    String valueTemplateStr = "{ includeName : \"$includeName\" name : \"$childCount\" }";
    ValueTemplate subjectUnderTest = new ValueTemplate(fieldResolverContextMock, valueTemplateStr);
    testDataSource.put("includeName", true);
    testDataSource.put("childCount", 5);

    Set<String> requiredFields = Sets.newHashSet("includeName", "childCount");
    when(fieldResolverContextMock.getRequiredFields()).thenReturn(requiredFields);

    String actual = subjectUnderTest.compile(testDataSource);
    assertThat(actual).isEqualTo("{ includeName : true name : 5 }");
  }
}
