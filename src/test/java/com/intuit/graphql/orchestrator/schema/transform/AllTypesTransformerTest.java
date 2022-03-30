package com.intuit.graphql.orchestrator.schema.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.schema.SchemaTransformationException;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder;
import org.junit.Test;

public class AllTypesTransformerTest {

  @Test
  public void testOperationTypesAreFilteredByTransformer() {

    String schema = "schema { query: QueryType } type QueryType { a: A } "
        + "type A { b: B } type B {c: C} "
        + "type C { field1: String, field2: String }";

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.GRAPHQL)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    XtextGraph transformed = new AllTypesTransformer().transform(xtextGraph);
    assertThat(transformed.getTypes().containsKey("QueryType")).isFalse();
    assertThat(transformed.getTypes()).hasSize(3);
    assertThat(transformed.getFieldCoordinates()).hasSize(5);
  }


  @Test
  public void testThrowsExceptionOnDuplicateTypeDefinition() {

    String schema = "schema { query: QueryType } type QueryType { a: A } "
        + "type A { b: B } type A {c: C} type B {c: C} "
        + "type C { field1: String, field2: String }";

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.GRAPHQL)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    assertThatThrownBy(() -> new AllTypesTransformer().transform(xtextGraph))
        .isInstanceOf(SchemaTransformationException.class)
        .hasMessageContainingAll("Duplicate TypeDefinition", "A", "SVC1");
  }
}
