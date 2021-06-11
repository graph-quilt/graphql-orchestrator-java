package com.intuit.graphql.orchestrator.xtext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class FieldContextTest {

  private static final String TEST_PARENT_TYPE_NAME = "testParentTypeName";
  private static final String TEST_FIELD_NAME = "testFieldName";

  private FieldContext fieldContext = new FieldContext(TEST_PARENT_TYPE_NAME,TEST_FIELD_NAME);

  @Test
  public void equalsReturnFalseForNull() {
    boolean actual = fieldContext.equals(null);

    assertThat(actual).isFalse();
  }

  @Test
  public void equalsReturnFalseForOtherType() {
    String someOtherObject = StringUtils.EMPTY;
    boolean actual = fieldContext.equals(someOtherObject);

    assertThat(actual).isFalse();
  }

  @Test
  public void equalsReturnFalseForDifferentFieldName() {
    FieldContext anotherFieldContext = new FieldContext(TEST_PARENT_TYPE_NAME, "anotherField");
    boolean actual = fieldContext.equals(anotherFieldContext);

    assertThat(actual).isFalse();
  }

  @Test
  public void equalsReturnFalseForDifferentParentTypName() {
    FieldContext anotherFieldContext = new FieldContext("anotherParentType", TEST_FIELD_NAME);
    boolean actual = fieldContext.equals(anotherFieldContext);

    assertThat(actual).isFalse();
  }

  @Test
  public void equalsReturnTrueForSameValues() {
    FieldContext anotherFieldContext = new FieldContext(TEST_PARENT_TYPE_NAME, TEST_FIELD_NAME);
    boolean actual = fieldContext.equals(anotherFieldContext);

    assertThat(actual).isTrue();
  }

  @Test
  public void equalsReturnTrueForSameObject() {
    boolean actual = fieldContext.equals(fieldContext);

    assertThat(actual).isTrue();
  }
}
