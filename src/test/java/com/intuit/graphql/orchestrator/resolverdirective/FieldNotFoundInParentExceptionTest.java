package com.intuit.graphql.orchestrator.resolverdirective;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ErrorType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FieldNotFoundInParentExceptionTest {

  private static final String TEST_SERVICE_NAMESPACE = "testServiceNamespace";
  private static final String TEST_FIELD_NAME = "testFieldName";
  private static final String TEST_PARENT_TYPE_NAME = "testParentTypeName";

  @Mock
  private ResolverDirectiveDefinition mockResolverDirectiveDefinition;

  @Test
  public void canCreateFieldNotFoundInParentException() {
    // GIVEN
    FieldNotFoundInParentException.Builder builder = FieldNotFoundInParentException
        .builder()
        .serviceNameSpace(TEST_SERVICE_NAMESPACE)
        .fieldName(TEST_FIELD_NAME)
        .parentTypeName(TEST_PARENT_TYPE_NAME)
        .resolverDirectiveDefinition(mockResolverDirectiveDefinition);

    String expectedMessage = String.format("Field not found in parent's resolved value. "
        + " fieldName=%s,  parentTypeName=%s,  resolverDirectiveDefinition=%s, serviceNameSpace=%s",
        TEST_FIELD_NAME, TEST_PARENT_TYPE_NAME, mockResolverDirectiveDefinition, TEST_SERVICE_NAMESPACE);

    //WHEN
    FieldNotFoundInParentException fieldNotFoundInParentException = builder.build();

    // THEN
    assertThat(fieldNotFoundInParentException).isNotNull();
    assertThat(fieldNotFoundInParentException.getMessage()).isEqualTo(expectedMessage);
    assertThat(fieldNotFoundInParentException.getErrorType()).isEqualTo(ErrorType.ExecutionAborted);
  }


}
