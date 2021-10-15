package com.intuit.graphql.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.schema.fold.FieldMergeValidations;
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException;
import com.intuit.graphql.orchestrator.stitching.XtextStitcher;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder;
import graphql.schema.GraphQLObjectType;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExceptionTest {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void NestedTypePrimitiveAndObjectType_TypeConflictExceptionTest() {
    String schema1 = "schema { query: Query } type Query { a: A } type A {  bbc: String } type BB {cc: String}";
    String schema2 = "schema { query: Query } type Query { a: A } type A {  bbc: String } type BB {cc: String}";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_B").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    List<ServiceProvider> providerList = Arrays
        .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider());
    exceptionRule.expect(FieldMergeException.class);
    exceptionRule.expectMessage(startsWith("Nested fields (parentType:A, field:bbc) are not eligible to merge"));
    XtextStitcher.newBuilder().build().stitch(providerList);
  }

  @Test
  public void NestedTypeUnEqualArgumentsExceptionTest() {
    String schema1 = "schema { query: Query } type Query { a: A } type A {  bbc (arg1: Arg1, arg2: Arg2): BB } type BB {cc: String} type Arg1 { pp: String } type Arg2 { pp: String }";
    String schema2 = "schema { query: Query } type Query { a: A } type A {  bbc (arg1: Arg1): BB } type BB {dd: String} type Arg1 { pp: String }";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_B").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    List<ServiceProvider> providerList = Arrays
        .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider());
    exceptionRule.expect(FieldMergeException.class);
    exceptionRule.expectMessage(startsWith("Nested fields (parentType:A, field:bbc) are not eligible to merge"));
    XtextStitcher.newBuilder().build().stitch(providerList);
  }

  @Test
  public void NestedTypeMissingArgumentsExceptionTest() {
    String schema1 = "schema { query: Query } type Query { a: A } type A {  bbc (arg1: Arg1, arg2: String): BB } type BB {cc: String} input Arg1 { pp: String }";
    String schema2 = "schema { query: Query } type Query { a: A } type A {  bbc (arg1: Arg1, arg3: String): BB } type BB {dd: String} input Arg1 { pp: String }";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_B").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    List<ServiceProvider> providerList = Arrays
        .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider());
    exceptionRule.expect(FieldMergeException.class);
    exceptionRule.expectMessage(startsWith("Nested fields (parentType:A, field:bbc) are not eligible to merge"));
    XtextStitcher.newBuilder().build().stitch(providerList);
  }

  @Test
  public void NestedTypeMismatchedArgumentsExceptionTest() {
    String schema1 = "schema { query: Query } type Query { a: A } type A {  bbc (arg1: Arg1, arg2: Arg2): BB } type BB {cc: String} input Arg1 { pp: String } input Arg2 { pp: String }";
    String schema2 = "schema { query: Query } type Query { a: A } type A {  bbc (arg1: Arg1, arg2: Arg3): BB } type BB {dd: String} input Arg1 { pp: String } input Arg3 { pp: String }";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_B").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    List<ServiceProvider> providerList = Arrays
        .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider());
    exceptionRule.expect(FieldMergeException.class);
    exceptionRule.expectMessage(startsWith("Nested fields (parentType:A, field:bbc) are not eligible to merge"));
    XtextStitcher.newBuilder().build().stitch(providerList);
  }

  @Test
  public void NestedTypeMatchedArgumentsNoExceptionTest() {
    String schema1 = "schema { query: Query } type Query { a: A } type A {  bbc (arg1: Arg1, arg2: Arg2): BB } type BB {cc: String} input Arg1 { pp: String } input Arg2 { pp: String }";
    String schema2 = "schema { query: Query } type Query { a: A } type A {  bbc (arg1: Arg1, arg2: Arg2): BB } type BB {dd: String} input Arg1 { pp: String } input Arg2 { pp: String }";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_B").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    List<ServiceProvider> providerList = Arrays
        .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider());
    XtextStitcher.newBuilder().build().stitch(providerList);
  }

  @Test
  public void NestedTypeMatchedArgumentsMultilevelNoExceptionTest() {
    String schema1 = "schema { query: Query } type Query { a: A } type A {  bbc (arg1: [Arg1], arg2: Arg2): BB } type BB {cc: String} input Arg1 { pp: QQ } input Arg2 { pp: Arg2 } input QQ { mm: String }";
    String schema2 = "schema { query: Query } type Query { a: A } type A {  bbc (arg1: [Arg1], arg2: Arg2): BB } type BB {dd: String} input Arg1 { pp: QR } input Arg2 { pp: Arg2 } input QR { mm: String }";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_B").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    List<ServiceProvider> providerList = Arrays
        .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider());
    RuntimeGraph runtimeGraph = XtextStitcher.newBuilder().build().stitch(providerList);
    GraphQLObjectType aType = (GraphQLObjectType) runtimeGraph.getGraphQLtypes().get("A");
    assertThat(aType.getFieldDefinition("bbc")).satisfies(bbcFieldDef -> {
      assertThat(bbcFieldDef.getArgument("arg1")).isNotNull();
      assertThat(bbcFieldDef.getArgument("arg2")).isNotNull();
      GraphQLObjectType bbcType = (GraphQLObjectType) bbcFieldDef.getType();
      assertThat(bbcType.getFieldDefinition("cc")).isNotNull();
      assertThat(bbcType.getFieldDefinition("dd")).isNotNull();
    });
  }

  @Test
  public void NestedTypeUnEqualDirectivesExceptionTest() {
    String schema1 = "schema { query: Query } type Query { a: A } type A {  bbc : BB "
        + "@addExternalFields(source: \"profiles\") @excludeField(name: \"photo\") } type BB {cc: String} "
        + "directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE "
        + "directive @excludeField(name: String!) on FIELD_DEFINITION | ENUM_VALUE";

    String schema2 = "schema { query: Query } type Query { a: A } type A {  bbc : BB "
        + "@addExternalFields(source: \"profiles\") } type BB {dd: String} "
        + "directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE "
        + "directive @someTest on FIELD_DEFINITION ";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_B").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    List<ServiceProvider> providerList = Arrays
        .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider());
    exceptionRule.expect(FieldMergeException.class);
    exceptionRule.expectMessage(startsWith("Nested fields (parentType:A, field:bbc) are not eligible to merge"));
    exceptionRule.expectMessage(containsString("Unequal directives: 1 is not same as 2"));
    XtextStitcher.newBuilder().build().stitch(providerList);
  }

  @Test
  public void NestedMismatchedDirectivesExceptionTest() {
    String schema1 = "schema { query: Query } type Query { a: A } type A {  bbc : BB "
        + "@addExternalFields(source: \"profiles\") @excludeField(name: \"photo\") } type BB {cc: String} "
        + "directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE "
        + "directive @excludeField(name: String!) on FIELD_DEFINITION | ENUM_VALUE";

    String schema2 = "schema { query: Query } type Query { a: A } type A {  bbc : BB "
        + "@addExternalFields(source: \"profiles\") @ignoreField(name: \"photo\") } type BB {dd: String} "
        + "directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE "
        + "directive @someTest on FIELD_DEFINITION "
        + "directive @ignoreField(name: String!) on FIELD_DEFINITION | ENUM_VALUE";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_B").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    List<ServiceProvider> providerList = Arrays
        .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider());
    exceptionRule.expect(FieldMergeException.class);
    exceptionRule.expectMessage(startsWith("Nested fields (parentType:A, field:bbc) are not eligible to merge"));
    exceptionRule.expectMessage(containsString("Missing directive"));
    XtextStitcher.newBuilder().build().stitch(providerList);
  }

  @Test
  public void NestedUnEqualDirectiveLocationsExceptionTest() {
    String schema1 = "schema { query: Query } type Query { a: A } type A {  bbc : BB "
        + "@addExternalFields(source: \"profiles\") @excludeField(name: \"photo\") } type BB {cc: String} "
        + "directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE "
        + "directive @excludeField(name: String!) on FIELD_DEFINITION";

    String schema2 = "schema { query: Query } type Query { a: A } type A {  bbc : BB "
        + "@addExternalFields(source: \"profiles\") @excludeField(name: \"photo\") } type BB {dd: String} "
        + "directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE "
        + "directive @excludeField(name: String!) on FIELD_DEFINITION | ENUM_VALUE";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_B").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    List<ServiceProvider> providerList = Arrays
        .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider());
    exceptionRule.expect(FieldMergeException.class);
    exceptionRule.expectMessage(startsWith("Nested fields (parentType:A, field:bbc) are not eligible to merge"));
    exceptionRule.expectMessage(containsString("Unequal directive locations"));
    XtextStitcher.newBuilder().build().stitch(providerList);
  }

  @Test
  public void NestedMismatchedDirectiveLocationsExceptionTest() {
    String schema1 = "schema { query: Query } type Query { a: A } type A {  bbc : BB "
        + "@addExternalFields(source: \"profiles\") @excludeField(name: \"photo\") } type BB {cc: String} "
        + "directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE "
        + "directive @excludeField(name: String!) on FIELD_DEFINITION | FIELD";

    String schema2 = "schema { query: Query } type Query { a: A } type A {  bbc : BB "
        + "@addExternalFields(source: \"profiles\") @excludeField(name: \"photo\") } type BB {dd: String} "
        + "directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE "
        + "directive @excludeField(name: String!) on FIELD_DEFINITION | ENUM_VALUE";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_B").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    List<ServiceProvider> providerList = Arrays
        .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider());
    exceptionRule.expect(FieldMergeException.class);
    exceptionRule.expectMessage(startsWith("Nested fields (parentType:A, field:bbc) are not eligible to merge"));
    exceptionRule.expectMessage(containsString("Missing directive location"));
    XtextStitcher.newBuilder().build().stitch(providerList);
  }

  @Test
  public void NestedMatchedDirectivesNoExceptionTest() {
    String schema1 = "schema { query: Query } type Query { a: A } type A {  bbc : BB "
        + "@addExternalFields(source: \"profiles\") @excludeField(name: \"photo\") } type BB {cc: String} "
        + "directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE "
        + "directive @excludeField(name: String!) on FIELD_DEFINITION | ENUM_VALUE";

    String schema2 = "schema { query: Query } type Query { a: A } type A {  bbc : BB "
        + "@addExternalFields(source: \"profiles\") @excludeField(name: \"photo\") } type BB {dd: String} "
        + "directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE "
        + "directive @someTest on FIELD_DEFINITION "
        + "directive @excludeField(name: String!) on FIELD_DEFINITION | ENUM_VALUE";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_B").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    List<ServiceProvider> providerList = Arrays
        .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider());
    RuntimeGraph runtimeGraph = XtextStitcher.newBuilder().build().stitch(providerList);
    GraphQLObjectType aType = (GraphQLObjectType) runtimeGraph.getGraphQLtypes().get("A");
    assertThat(aType.getFieldDefinition("bbc")).satisfies(bbcFieldDef -> {
      assertThat(bbcFieldDef.getDirective("addExternalFields")).isNotNull();
      assertThat(bbcFieldDef.getDirective("excludeField")).isNotNull();
      GraphQLObjectType bbcType = (GraphQLObjectType) bbcFieldDef.getType();
      assertThat(bbcType.getFieldDefinition("cc")).isNotNull();
      assertThat(bbcType.getFieldDefinition("dd")).isNotNull();
    });
  }


  @Test
  public void IsAGraphQLObjectType_ValidationExceptionTest() {
    String schema = "schema { query: Query } type Query { a: String }";

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    exceptionRule.expect(FieldMergeException.class);
    exceptionRule.expectMessage(endsWith("is not an ObjectType"));
    FieldDefinition fieldDefinition = xtextGraph.getOperationType(Operation.QUERY).getFieldDefinition().get(0);
    FieldMergeValidations
        .checkMergeEligibility("Query", fieldDefinition, fieldDefinition);
  }

  @Test
  public void HasTheSameTypeName_ValidationExceptionTest() {
    String schema = "schema { query: Query } type Query { a: String, b: Int }";

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    exceptionRule.expect(FieldMergeException.class);
    FieldMergeValidations
        .checkMergeEligibility("Query", xtextGraph.getOperationType(Operation.QUERY).getFieldDefinition().get(0),
            xtextGraph.getOperationType(Operation.QUERY).getFieldDefinition().get(1));
  }
}
