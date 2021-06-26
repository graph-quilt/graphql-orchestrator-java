package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPreMergeTestHelper.SCHEMA_A_AND_C_WITH_WITH_FIELD_RESOLVER;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPreMergeTestHelper.SCHEMA_A_IS_INTERFACE;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPreMergeTestHelper.SCHEMA_A_IS_OBJECT_TYPE;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPreMergeTestHelper.SCHEMA_A_NOT_NULL_WRAPPED_IN_ARRAY;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPreMergeTestHelper.SCHEMA_A_TYPE_IS_NOT_NULL;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPreMergeTestHelper.SCHEMA_A_TYPE_WRAPPED_IN_ARRAY;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPreMergeTestHelper.SCHEMA_A_WITH_TWO__FIELD_RESOLVERS;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPreMergeTestHelper.SCHEMA_FIELD_WITH_RESOLVER_HAS_ARGUMENT;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPreMergeTestHelper.createTestXtextGraph;
import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.graphql.orchestrator.resolverdirective.ArgumentDefinitionNotAllowed;
import com.intuit.graphql.orchestrator.resolverdirective.NotAValidLocationForFieldResolverDirective;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FieldResolverTransformerPreMergeTest {

  private final Transformer<XtextGraph, XtextGraph> transformer = new FieldResolverTransformerPreMerge();

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void transformWithNoFieldResolverSuccessNoTransformation() {
    // GIVEN
    String schema =
        "type Query { "
            + "   basicField(arg: Int) : String"
            + "   fieldWithArgumentResolver(arg: Int @resolver(field: \"a.b.c\")): Int "
            + "} "
            + "directive @resolver(field: String) on ARGUMENT_DEFINITION";
    XtextGraph xtextGraph = createTestXtextGraph(schema);
    assertThat(xtextGraph.hasFieldResolverDirective()).isFalse();

    // WHEN
    final XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource.getCodeRegistry().size()).isEqualTo(0);
    assertThat(transformedSource.hasFieldResolverDirective()).isFalse();
  }

  @Test
  public void transformWithFieldResolverSuccess() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_IS_OBJECT_TYPE);
    assertThat(xtextGraph.hasFieldResolverDirective()).isFalse();

    // WHEN
    final XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource.hasFieldResolverDirective()).isTrue();
    assertThat(transformedSource.getFieldResolverContexts().size()).isEqualTo(1);

    FieldResolverContext actualFieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(actualFieldResolverContext.getParentTypename()).isEqualTo("AObjectType");
    assertThat(actualFieldResolverContext.getFieldName()).isEqualTo("b1");
  }

  @Test
  public void transformWithFieldResolverParentNotNullSuccess() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_TYPE_IS_NOT_NULL);
    assertThat(xtextGraph.hasFieldResolverDirective()).isFalse();

    // WHEN
    final XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource.hasFieldResolverDirective()).isTrue();
    assertThat(transformedSource.getFieldResolverContexts().size()).isEqualTo(1);

    FieldResolverContext actualFieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(actualFieldResolverContext.getParentTypename()).isEqualTo("AObjectType");
    assertThat(actualFieldResolverContext.getFieldName()).isEqualTo("b1");
  }

  @Test
  public void transformWithFieldResolverParentWrappedInArraySuccess() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_TYPE_WRAPPED_IN_ARRAY);
    assertThat(xtextGraph.hasFieldResolverDirective()).isFalse();

    // WHEN
    final XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource.hasFieldResolverDirective()).isTrue();
    assertThat(transformedSource.getFieldResolverContexts().size()).isEqualTo(1);

    FieldResolverContext actualFieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(actualFieldResolverContext.getParentTypename()).isEqualTo("AObjectType");
    assertThat(actualFieldResolverContext.getFieldName()).isEqualTo("b1");
  }

  @Test
  public void transformWithFieldResolverParentNotNullWrappedInArraySuccess() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_NOT_NULL_WRAPPED_IN_ARRAY);
    assertThat(xtextGraph.hasFieldResolverDirective()).isFalse();

    // WHEN
    final XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource.hasFieldResolverDirective()).isTrue();
    assertThat(transformedSource.getFieldResolverContexts().size()).isEqualTo(1);

    FieldResolverContext actualFieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(actualFieldResolverContext.getParentTypename()).isEqualTo("AObjectType");
    assertThat(actualFieldResolverContext.getFieldName()).isEqualTo("b1");
  }

  @Test
  public void transformWithTwoFieldResolversYieldsTwoFieldResolverContext() {
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_WITH_TWO__FIELD_RESOLVERS);
    assertThat(xtextGraph.hasFieldResolverDirective()).isFalse();

    // WHEN
    final XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource.hasFieldResolverDirective()).isTrue();
    assertThat(transformedSource.getFieldResolverContexts().size()).isEqualTo(2);
  }

  @Test
  public void transformTwoTypesWithFieldWithResolverYieldsTwoFieldResolverContexts() {
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_AND_C_WITH_WITH_FIELD_RESOLVER);
    assertThat(xtextGraph.hasFieldResolverDirective()).isFalse();

    // WHEN
    final XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource.hasFieldResolverDirective()).isTrue();
    assertThat(transformedSource.getFieldResolverContexts().size()).isEqualTo(2);
  }

  @Test
  public void transformWithFieldResolverParentIsAnInterfaceThrowsException() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_IS_INTERFACE);
    assertThat(xtextGraph.hasFieldResolverDirective()).isFalse();

    exceptionRule.expect(NotAValidLocationForFieldResolverDirective.class);

    // WHEN .. THEN throws exception
    transformer.transform(xtextGraph);
  }

  @Test
  public void transformWithFieldDefinitionResolveHasArgumentThrowsException() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_FIELD_WITH_RESOLVER_HAS_ARGUMENT);
    assertThat(xtextGraph.hasFieldResolverDirective()).isFalse();

    exceptionRule.expect(ArgumentDefinitionNotAllowed.class);

    // WHEN .. THEN throws exception
    transformer.transform(xtextGraph);
  }
}
