package com.intuit.graphql.orchestrator.schema.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.SchemaTransformationException;
import com.intuit.graphql.orchestrator.schema.fold.XtextGraphFolder;
import com.intuit.graphql.orchestrator.schema.transform.GraphQLAdapterTransformer.AdapterDirectiveVisitor;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder;
import java.util.Arrays;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class GraphqlAdapterTransformerTest {

  public static String directive = "directive @adapter(service:String!) on FIELD_DEFINITION";

  @Test
  public void testAdapterDirectiveNested() {

    String schema = "schema { query: Query } type Query { a: A } "
        + "type A { b: B } type B {c: C}"
        + "type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter(service: 'bar') }"
        + "type D { field: String}"
        + directive;

    String schema2 = "schema { query: Query } type Query { a: A } "
        + "type A {  bb: BB }  type BB {cc: String}";

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_b").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_bb").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    XtextGraph stitchedGraph = new XtextGraphFolder()
        .fold(XtextGraph.emptyGraph(), Arrays.asList(xtextGraph, xtextGraph2));

    XtextGraph adapterGraph = new GraphQLAdapterTransformer().transform(stitchedGraph);

    ObjectTypeDefinition query = adapterGraph.getOperationMap().get(Operation.QUERY);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("Query", "a")).getDataFetcherType()).isEqualTo(
        DataFetcherType.STATIC);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("A", "b")).getDataFetcherType()).isEqualTo(
        DataFetcherType.STATIC);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("A", "bb")).getDataFetcherType()).isEqualTo(
        DataFetcherType.SERVICE);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("A", "bb")).getNamespace()).isEqualTo(
        "SVC_bb");
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("B", "c")).getDataFetcherType()).isEqualTo(
        DataFetcherType.STATIC);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter1")).getDataFetcherType()).isEqualTo(
        DataFetcherType.SERVICE);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter2")).getDataFetcherType()).isEqualTo(
        DataFetcherType.SERVICE);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter1")).getNamespace()).isEqualTo(
        "SVC_b");
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter2")).getNamespace()).isEqualTo(
        "SVC_b");
  }

  @Test
  public void testAdapterDirectiveAtSameLevelAsRest() {

    String schema = "schema { query: Query } type Query { a: A } "
        + "type A { b: B @adapter(service: 'foo') } type B {d: D}"
        + "type D { field: String}"
        + directive;

    String schema2 = "schema { query: Query } type Query { a: A } "
        + "type A {  bb: BB }  type BB {cc: String}";

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_b").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_bb").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    XtextGraph stitchedGraph = new XtextGraphFolder()
        .fold(XtextGraph.emptyGraph(), Arrays.asList(xtextGraph, xtextGraph2));

    XtextGraph adapterGraph = new GraphQLAdapterTransformer().transform(stitchedGraph);

    ObjectTypeDefinition query = adapterGraph.getOperationMap().get(Operation.QUERY);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("Query", "a")).getDataFetcherType()).isEqualTo(
        DataFetcherType.STATIC);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("A", "b")).getDataFetcherType()).isEqualTo(
        DataFetcherType.SERVICE);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("A", "b")).getNamespace()).isEqualTo(
        "SVC_b");
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("A", "bb")).getDataFetcherType()).isEqualTo(
        DataFetcherType.SERVICE);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("A", "bb")).getNamespace()).isEqualTo(
        "SVC_bb");
  }

  @Test
  public void testAdapterDirectiveOnlyOneRestService() {

    String schema = "schema { query: Query } type Query { a: A } "
        + "type A { b: B } type B {c: C} "
        + "type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter(service: 'bar') }"
        + "type D { field: String}"
        + directive;

    FieldContext fieldContext = new FieldContext("Query", "a");
    DataFetcherContext dataFetcherContext = DataFetcherContext.newBuilder().dataFetcherType(DataFetcherType.SERVICE)
        .namespace("SVC1")
        .serviceType(ServiceType.REST).build();

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    xtextGraph.transform(builder -> builder.codeRegistry(ImmutableMap.of(fieldContext, dataFetcherContext)));

    XtextGraph stitchedGraph = new XtextGraphFolder()
        .fold(XtextGraph.emptyGraph(), Collections.singletonList(xtextGraph));
    XtextGraph adapterGraph = new GraphQLAdapterTransformer().transform(stitchedGraph);

    ObjectTypeDefinition query = adapterGraph.getOperationMap().get(Operation.QUERY);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("Query", "a")).getDataFetcherType()).isEqualTo(
        DataFetcherType.STATIC);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("A", "b")).getDataFetcherType()).isEqualTo(
        DataFetcherType.STATIC);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("B", "c")).getDataFetcherType()).isEqualTo(
        DataFetcherType.STATIC);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter1")).getDataFetcherType()).isEqualTo(
        DataFetcherType.SERVICE);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter2")).getDataFetcherType()).isEqualTo(
        DataFetcherType.SERVICE);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter1")).getNamespace()).isEqualTo(
        "SVC1");
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter2")).getNamespace()).isEqualTo(
        "SVC1");
  }

  @Test
  public void testAdapterTransformerWithoutDirective() {

    String schema = "schema { mutation: Mutation query: Query} "
        + "type Mutation { a: A } "
        + "type Query { a: A } "
        + "type A { b: B } type B {c: C} "
        + "type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter(service: 'bar') }"
        + "type D { field: String }"
        + directive;

    FieldContext fieldContext = new FieldContext("Query", "a");
    DataFetcherContext dataFetcherContext = DataFetcherContext.newBuilder()
        .dataFetcherType(DataFetcherType.SERVICE)
        .namespace("SVC1")
        .serviceType(ServiceType.REST)
        .build();

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    xtextGraph.transform(builder -> builder.codeRegistry(ImmutableMap.of(fieldContext, dataFetcherContext)));

    XtextGraph stitchedGraph = new XtextGraphFolder()
        .fold(XtextGraph.emptyGraph(), Collections.singletonList(xtextGraph));
    XtextGraph adapterGraph = new GraphQLAdapterTransformer().transform(stitchedGraph);

    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("Mutation", "a"))).isNotNull();
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("Mutation", "a")).getNamespace()).isEqualTo("SVC1");
  }

  @Test
  public void testAdapterTransformerForNonRest() {

    String schema = "schema { query: Query } type Query { a: A } "
        + "type A { b: B } type B {c: C} "
        + "type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter(service: 'bar') }"
        + "type D { field: String }"
        + directive;

    FieldContext fieldContext = new FieldContext("Query", "a");
    DataFetcherContext dataFetcherContext = DataFetcherContext.newBuilder()
        .dataFetcherType(DataFetcherType.SERVICE)
        .namespace("SVC1")
        .serviceType(ServiceType.GRAPHQL)
        .build();

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    xtextGraph.transform(builder -> builder.codeRegistry(ImmutableMap.of(fieldContext, dataFetcherContext)));

    XtextGraph stitchedGraph = new XtextGraphFolder()
        .fold(XtextGraph.emptyGraph(), Collections.singletonList(xtextGraph));
    XtextGraph adapterGraph = new GraphQLAdapterTransformer().transform(stitchedGraph);

    ObjectTypeDefinition query = adapterGraph.getOperationMap().get(Operation.QUERY);
    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("Mutation", "a"))).isNull();
  }

  @Test
  public void testAdapterTransformerWithoutField() {

    String schema = "schema { query: Query } type Query { a: A } "
        + "type A { b: B } type B {c: C} "
        + "type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter(service: 'bar') }"
        + "type D { field: String }"
        + directive;

    FieldContext fieldContext = new FieldContext("Query", "foo");
    DataFetcherContext dataFetcherContext = DataFetcherContext.newBuilder().dataFetcherType(DataFetcherType.SERVICE)
        .namespace("SVC1")
        .serviceType(ServiceType.REST).build();

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    xtextGraph.transform(builder -> builder.codeRegistry(ImmutableMap.of(fieldContext, dataFetcherContext)));

    assertThatThrownBy(() -> new GraphQLAdapterTransformer().transform(xtextGraph)).isInstanceOf(
        SchemaTransformationException.class)
        .hasMessage(String.format(GraphQLAdapterTransformer.FIELD_NULL_ERROR, "foo"));

  }

  @Test
  public void testAdapterTransformerForMutation() {

    String schema = "schema { mutation: Mutation } type Mutation { a: A } "
        + "type A { b: B } type B {c: C} "
        + "type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter(service: 'bar') }"
        + "type D { field: String }"
        + directive;

    FieldContext fieldContext = new FieldContext("Mutation", "a");
    DataFetcherContext dataFetcherContext = DataFetcherContext.newBuilder().dataFetcherType(DataFetcherType.SERVICE)
        .namespace("SVC1")
        .serviceType(ServiceType.REST).build();

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    xtextGraph.transform(builder -> builder.codeRegistry(ImmutableMap.of(fieldContext, dataFetcherContext)));

    XtextGraph stitchedGraph = new XtextGraphFolder()
        .fold(XtextGraph.emptyGraph(), Collections.singletonList(xtextGraph));
    XtextGraph adapterGraph = new GraphQLAdapterTransformer().transform(stitchedGraph);

    assertThat(adapterGraph.getCodeRegistry().get(new FieldContext("Mutation", "a")).getNamespace()).isEqualTo(
        "SVC1");

    assertThat(adapterGraph.getCodeRegistry().size()).isEqualTo(1);

  }

  @Test
  public void testAdapterTransformerWithoutDirectiveArgument() {

    String schema = "schema { query: Query } type Query { a: A } "
        + "type A { b: B } type B {c: C} "
        + "type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter }"
        + "type D { field: String }"
        + directive;

    FieldContext fieldContext = new FieldContext("Query", "a");
    DataFetcherContext dataFetcherContext = DataFetcherContext.newBuilder().dataFetcherType(DataFetcherType.SERVICE)
        .namespace("SVC1")
        .serviceType(ServiceType.REST).build();

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    xtextGraph.transform(builder -> builder.codeRegistry(ImmutableMap.of(fieldContext, dataFetcherContext)));

    assertThatThrownBy(() -> new GraphQLAdapterTransformer().transform(xtextGraph)).isInstanceOf(
        SchemaTransformationException.class).hasMessage(AdapterDirectiveVisitor.ERROR_MSG);

  }
}
