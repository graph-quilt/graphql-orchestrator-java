package com.intuit.graphql.orchestrator;

import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.fold.FieldMergeValidations;
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException;
import com.intuit.graphql.orchestrator.stitching.XtextStitcher;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder;
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
    String schema2 = "schema { query: Query } type Query { a: A } type A {  bbc: String } type String {cc: String}";

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
  public void IsAGraphQLObjectType_ValidationExceptionTest() {
    String schema = "schema { query: Query } type Query { a: String }";

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    exceptionRule.expect(FieldMergeException.class);
    exceptionRule.expectMessage(endsWith("is not an ObjectType"));
    FieldDefinition fieldDefinition = xtextGraph.getOperationType(Operation.QUERY).getFieldDefinition().get(0);
    FieldMergeValidations
        .checkMergeEligiblity("Query", fieldDefinition, fieldDefinition);
  }

  @Test
  public void HasTheSameTypeName_ValidationExceptionTest() {
    String schema = "schema { query: Query } type Query { a: String, b: Int }";

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_A").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    exceptionRule.expect(FieldMergeException.class);
    FieldMergeValidations
        .checkMergeEligiblity("Query", xtextGraph.getOperationType(Operation.QUERY).getFieldDefinition().get(0),
            xtextGraph.getOperationType(Operation.QUERY).getFieldDefinition().get(1));
  }
}
