package com.intuit.graphql.orchestrator.fieldresolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentNotAFieldOfParentException;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FieldResolverValidatorTest {
  @Mock FieldResolverContext fieldResolverContextMock;

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  private static final Map<String, FieldDefinition> PARENT_TYPE_FIELDS =
      ImmutableMap.of("a", mock(FieldDefinition.class));

  @Before
  public void setup() {
    when(fieldResolverContextMock.getServiceNamespace()).thenReturn("TEST_NAMESPACE");
    when(fieldResolverContextMock.getParentTypename()).thenReturn("ParentType");
    when(fieldResolverContextMock.getFieldName()).thenReturn("fieldWithResolver");

    when(fieldResolverContextMock.getParentTypeFields()).thenReturn(PARENT_TYPE_FIELDS);
  }

  @Test
  public void validateRequiredFields_requiredFieldsNotInParentType_throwsException() {
    exceptionRule.expect(ResolverArgumentNotAFieldOfParentException.class);
    String expectedMessage =
        "'b' is not a field of parent type. serviceName=TEST_NAMESPACE, "
            + "parentTypeName=ParentType, fieldName=fieldWithResolver";
    exceptionRule.expectMessage(expectedMessage);

    Set<String> requiredFields = ImmutableSet.of("b");

    when(fieldResolverContextMock.getParentTypeFields()).thenReturn(PARENT_TYPE_FIELDS);
    when(fieldResolverContextMock.getRequiredFields()).thenReturn(requiredFields);
    FieldResolverValidator.validateRequiredFields(fieldResolverContextMock);
  }

  @Test
  public void validateRequiredFields_requiredFieldsIsPresentInParentType_doesNotThrowException() {
    when(fieldResolverContextMock.getRequiredFields()).thenReturn(ImmutableSet.of("a"));

    Assertions.assertThatCode(
            () -> FieldResolverValidator.validateRequiredFields(fieldResolverContextMock))
        .doesNotThrowAnyException();
  }

  @Test
  public void validateRequiredFields_noRequiredFields_doesNotThrowException() {
    when(fieldResolverContextMock.getRequiredFields()).thenReturn(Collections.emptySet());

    Assertions.assertThatCode(
            () -> FieldResolverValidator.validateRequiredFields(fieldResolverContextMock))
        .doesNotThrowAnyException();
  }

  @Test
  public void validateRequiredFields_getRequiredFieldsIsNull_doesNotThrowException() {
    when(fieldResolverContextMock.getRequiredFields()).thenReturn(null);

    Assertions.assertThatCode(
            () -> FieldResolverValidator.validateRequiredFields(fieldResolverContextMock))
        .doesNotThrowAnyException();
  }
}
