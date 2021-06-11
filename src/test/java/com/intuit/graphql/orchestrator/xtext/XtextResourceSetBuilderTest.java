package com.intuit.graphql.orchestrator.xtext;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.intuit.graphql.orchestrator.TestHelper;
import com.intuit.graphql.orchestrator.schema.SchemaParseException;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.junit.Test;

public class XtextResourceSetBuilderTest {

  @Test
  public void buildsEmptyResourceSet() {
    XtextResourceSet emptyResourceSet = XtextResourceSetBuilder.newBuilder().build();
    assertThat(emptyResourceSet).isNotNull();
  }

  @Test
  public void buildsSingleFileResourceSet() {
    XtextResourceSet resourceSet = XtextResourceSetBuilder.newBuilder()
        .file("top_level/eps/schema2.graphqls",TestHelper.getResourceAsString("top_level/eps/schema2.graphqls"))
        .build();
    assertThat(resourceSet).isNotNull();
    assertThat(resourceSet.getResources().size()).isEqualTo(1);
  }

  @Test
  public void buildsMultipleFileResourceSet() {
    XtextResourceSet resourceSet = XtextResourceSetBuilder.newBuilder()
        .files(TestHelper.getFileMapFromList("top_level/eps/schema2.graphqls", "top_level/person/schema1.graphqls"))
        .build();
    assertThat(resourceSet).isNotNull();
    assertThat(resourceSet.getResources().size()).isEqualTo(2);
  }

  @Test
  public void throwsException() {
    XtextResourceSetBuilder builder = XtextResourceSetBuilder.newBuilder();
    assertThatThrownBy(()->builder.file("foo",null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(()->builder.file(      null,"foo")).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void throwsExceptionWhenValidationFails() {
    XtextResourceSetBuilder builder = XtextResourceSetBuilder.newBuilder().file("foo",  "type foo { abc: Inta }");
    assertThatThrownBy(()->builder.build())
        .isInstanceOf(SchemaParseException.class)
        .hasMessageContaining("ERROR:Couldn't resolve reference to TypeDefinition 'Inta'");

    XtextResourceSetBuilder builder1 = XtextResourceSetBuilder.newBuilder()
        .file("foo",  "type foo { bar: String } type foo { abc: Int }");
    assertThatThrownBy(()->builder1.build())
        .isInstanceOf(SchemaParseException.class)
        .hasMessageContaining("Duplicate name in schema: foo");
  }

}
