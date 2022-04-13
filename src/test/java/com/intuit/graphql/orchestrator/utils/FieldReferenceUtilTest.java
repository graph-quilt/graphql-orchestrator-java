package com.intuit.graphql.orchestrator.utils;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.Set;
import org.junit.Test;

public class FieldReferenceUtilTest {

  @Test
  public void getAllFieldReference_fromEmtyString_returnsEmptySet() {
    Set<String> actual = FieldReferenceUtil.getAllFieldReferenceFromString("");
    assertThat(actual).isEmpty();
  }

  @Test
  public void getAllFieldReference_fromNull_returnsEmptySet() {
    Set<String> actual = FieldReferenceUtil.getAllFieldReferenceFromString(null);
    assertThat(actual).isEmpty();
  }

  @Test
  public void getAllFieldReference_fromFieldRef_returnsExtractedFields() {
    Set<String> actual = FieldReferenceUtil.getAllFieldReferenceFromString("$someFieldRef");
    assertThat(actual).hasSize(1);
    assertThat(actual.contains("someFieldRef")).isTrue();
  }

  @Test
  public void getAllFieldReference_fromJsonString_returnsExtractedFields() {
    Set<String> actual = FieldReferenceUtil
        .getAllFieldReferenceFromString("{ \"field\": $someFieldRef }");
    assertThat(actual).hasSize(1);
    assertThat(actual.contains("someFieldRef")).isTrue();
  }

}
