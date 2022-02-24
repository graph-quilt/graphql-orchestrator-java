package com.intuit.graphql.orchestrator.xtext;

import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newBigDecimalType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newBigIntType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newBooleanType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newByteType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newCharType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newFieldSetType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newFloatType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newIdType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newIntType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newLongType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newShortType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newStringType;
import static graphql.schema.idl.ScalarInfo.isStandardScalar;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class XtextScalarsTest {

  @Test
  public void testDoesNotReturnSameInstance() {
    assertThat(newBigDecimalType()).isNotSameAs(newBigDecimalType());
    assertThat(newBigIntType()).isNotSameAs(newBigIntType());
    assertThat(newBooleanType()).isNotSameAs(newBooleanType());
    assertThat(newByteType()).isNotSameAs(newByteType());
    assertThat(newCharType()).isNotSameAs(newCharType());
    assertThat(newFloatType()).isNotSameAs(newFloatType());
    assertThat(newIntType()).isNotSameAs(newIntType());
    assertThat(newIdType()).isNotSameAs(newIdType());
    assertThat(newStringType()).isNotSameAs(newStringType());
    assertThat(newLongType()).isNotSameAs(newLongType());
    assertThat(newShortType()).isNotSameAs(newShortType());
    assertThat(newFieldSetType()).isNotSameAs(newFieldSetType());
  }

  @Test
  public void testStandardScalarMap() {
    assertThat(XtextScalars.STANDARD_SCALARS).hasSize(12);
  }

  @Test
  public void fieldSetNotInStandardScalars(){
    //If this is true, remove addition of field set scalar in XtextGraphQlVisitor(Should be done after upgrading graphql java)
    assertThat(isStandardScalar("_FieldSet")).isFalse();
  }
}