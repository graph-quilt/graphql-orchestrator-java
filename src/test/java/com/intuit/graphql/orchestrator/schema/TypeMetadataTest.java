package com.intuit.graphql.orchestrator.schema;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TypeMetadataTest {

  private static final String TEST_FIELD_NAME = "testField";
  private static final String TEST_PARENT_TYPE_NAME = "ParentType";
  private TypeMetadata subjectUnderTest;

  @Mock
  private TypeDefinition typeDefinitionMock;

  @Mock
  private FieldResolverContext fieldResolverContextMock;

  @Before
  public void setup() {
    subjectUnderTest = new TypeMetadata(typeDefinitionMock);
  }

  @Test
  public void hasResolverDirective_noAddedFieldResolvers_returnsFalse() {
    boolean actual = subjectUnderTest.hasResolverDirective(TEST_FIELD_NAME);
    assertThat(actual).isFalse();
  }

  @Test
  public void hasResolverDirective_withFieldResolvers_canReturnTrue() {
    when(fieldResolverContextMock.getFieldName()).thenReturn(TEST_FIELD_NAME);

    subjectUnderTest.addFieldResolverContext(fieldResolverContextMock);
    boolean actual = subjectUnderTest.hasResolverDirective(TEST_FIELD_NAME);
    assertThat(actual).isTrue();
  }

  @Test
  public void getFieldResolverContext_noFieldResolvers_returnsNull() {
    subjectUnderTest.addFieldResolverContext(fieldResolverContextMock);
    FieldResolverContext actual = subjectUnderTest.getFieldResolverContext(TEST_FIELD_NAME);
    assertThat(actual).isNull();
  }

  @Test
  public void getFieldResolverContext_withFieldResolvers_canReturnObject() {
    when(fieldResolverContextMock.getFieldName()).thenReturn(TEST_FIELD_NAME);
    subjectUnderTest.addFieldResolverContext(fieldResolverContextMock);
    FieldResolverContext actual = subjectUnderTest.getFieldResolverContext(TEST_FIELD_NAME);
    assertThat(actual).isSameAs(fieldResolverContextMock);
  }

}
