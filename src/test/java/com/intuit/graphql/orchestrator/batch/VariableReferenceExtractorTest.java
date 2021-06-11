package com.intuit.graphql.orchestrator.batch;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.language.ArrayValue;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.VariableReference;
import java.math.BigInteger;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

public class VariableReferenceExtractorTest {

  private VariableReference reference = VariableReference.newVariableReference()
      .name("test_reference")
      .build();

  private VariableReferenceExtractor extractor;

  @Before
  public void setUp() {
    extractor = new VariableReferenceExtractor();
  }

  @Test
  public void extractsVariableReferences() {

    extractor.captureVariableReferences(Collections.singletonList(reference));

    assertThat(extractor.getVariableReferences()).containsOnly(reference);
  }

  @Test
  public void extractsVariableReferenceInObject() {
    ObjectValue objectValue = ObjectValue.newObjectValue()
        .objectField(ObjectField.newObjectField().value(reference).name("test_object").build()).build();

    extractor.captureVariableReferences(Collections.singletonList(objectValue));

    assertThat(extractor.getVariableReferences()).containsOnly(reference);
  }

  @Test
  public void extractsVariableReferenceInArray() {
    ArrayValue arrayValue = ArrayValue.newArrayValue()
        .value(IntValue.newIntValue().value(BigInteger.ONE).build())
        .value(reference).build();

    extractor.captureVariableReferences(Collections.singletonList(arrayValue));

    assertThat(extractor.getVariableReferences()).containsOnly(reference);
  }

  @Test
  public void extractsNoVariableReferences() {
    ArrayValue arrayValue = ArrayValue.newArrayValue()
        .value(IntValue.newIntValue(BigInteger.ONE).build())
        .value(StringValue.newStringValue("test").build())
        .build();

    extractor.captureVariableReferences(Collections.singletonList(arrayValue));

    assertThat(extractor.getVariableReferences()).isEmpty();
  }

  @Test
  public void returnsDifferentReferences() {
    assertThat(extractor.getVariableReferences()).isNotSameAs(extractor.getVariableReferences());
  }
}