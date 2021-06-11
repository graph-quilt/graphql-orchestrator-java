package com.intuit.graphql.orchestrator.stitching;

import static com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType.RESOLVER_ARGUMENT;
import static graphql.schema.FieldCoordinates.coordinates;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.graphql.orchestrator.TestHelper;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.batch.GraphQLServiceBatchLoader;
import com.intuit.graphql.orchestrator.datafetcher.ResolverArgumentDataFetcher;
import com.intuit.graphql.orchestrator.datafetcher.RestDataFetcher;
import com.intuit.graphql.orchestrator.datafetcher.ServiceDataFetcher;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.schema.transform.Transformer;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import java.util.Arrays;
import java.util.Collections;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.junit.Test;

public class XtextStitcherTest {

  private static final String schema = "type TestType { test_field(arg: Int @resolver(field: \"a.b.c\")): Int }"
      + " directive @resolver(field: String) on ARGUMENT_DEFINITION";


  @Test
  public void buildsResolverArgumentDataFetchers() {
    FieldContext testFieldContext = new FieldContext("TestType", "test_field");

    final XtextResourceSet testSchema = XtextResourceSetBuilder.newBuilder()
        .file("test_schema", schema).build();

    final ObjectTypeDefinition testType = XtextUtils.getObjectType("TestType", testSchema);
    final FieldDefinition testField = testType.getFieldDefinition().get(0);

    Transformer<XtextGraph, XtextGraph> transformer = source -> {
      final XtextGraph graphWithResolver = XtextGraph.emptyGraph();
      graphWithResolver.getCodeRegistry().put(testFieldContext, DataFetcherContext.newBuilder()
          .namespace("test_namespace")
          .dataFetcherType(RESOLVER_ARGUMENT)
          .build());
      graphWithResolver.getResolverArgumentFields().put(testFieldContext, testField.getArgumentsDefinition());

      return graphWithResolver;
    };

    XtextStitcher xtextStitcher = XtextStitcher.newBuilder()
        .preMergeTransformers(Collections.emptyList())
        .postMergeTransformers(Collections.singletonList(transformer))
        .build();

    final RuntimeGraph runtimeGraph = xtextStitcher.stitch(Collections.emptyList());

    assertThat(
        runtimeGraph.getCodeRegistry().build()
            .getDataFetcher(coordinates("TestType", "test_field"), mock(GraphQLFieldDefinition.class)))
        .isInstanceOf(ResolverArgumentDataFetcher.class);
  }

  @Test
  public void testBatchLoadersArePresentInRuntimeGraph() {
    ServiceProvider sp1 = TestServiceProvider.newBuilder()
        .serviceType(ServiceType.REST)
        .namespace("PERSON")
        .sdlFiles(TestHelper.getFileMapFromList("top_level/person/schema1.graphqls"))
        .build();

    ServiceProvider sp2 = TestServiceProvider.newBuilder()
        .serviceType(ServiceType.GRAPHQL)
        .namespace("EPS")
        .sdlFiles(TestHelper.getFileMapFromList("top_level/eps/schema2.graphqls"))
        .build();

    XtextStitcher stitcher = XtextStitcher.newBuilder().build();
    RuntimeGraph runtimeGraph = stitcher.stitch(Arrays.asList(sp1, sp2));
    assertThat(runtimeGraph.getBatchLoaderMap().size()).isEqualTo(1);

    GraphQLObjectType query = runtimeGraph.getOperation(Operation.QUERY);
    GraphQLObjectType mutation = runtimeGraph.getOperation(Operation.MUTATION);

    assertThat(runtimeGraph.getCodeRegistry()
        .getDataFetcher(
            FieldCoordinates.coordinates("Query", "person"),
            query.getFieldDefinition("person"))).isInstanceOf(RestDataFetcher.class);
    assertThat(runtimeGraph.getCodeRegistry()
        .getDataFetcher(
            FieldCoordinates.coordinates("Query", "personById"),
            query.getFieldDefinition("personById"))).isInstanceOf(RestDataFetcher.class);
    assertThat(runtimeGraph.getCodeRegistry()
        .getDataFetcher(
            FieldCoordinates.coordinates("Query", "Profile"),
            query.getFieldDefinition("Profile"))).isInstanceOf(ServiceDataFetcher.class);
    assertThat(runtimeGraph.getCodeRegistry()
        .getDataFetcher(
            FieldCoordinates.coordinates("Query", "ExpertDetails"),
            query.getFieldDefinition("ExpertDetails"))).isInstanceOf(ServiceDataFetcher.class);
    assertThat(runtimeGraph.getCodeRegistry()
        .getDataFetcher(
            FieldCoordinates.coordinates("Query", "searchProfile"),
            query.getFieldDefinition("person"))).isInstanceOf(ServiceDataFetcher.class);
    assertThat(runtimeGraph.getCodeRegistry()
        .getDataFetcher(
            FieldCoordinates.coordinates("Mutation", "upsertProfile"),
            mutation.getFieldDefinition("upsertProfile"))).isInstanceOf(ServiceDataFetcher.class);

//    assertThat(runtimeGraph.getBatchLoaderMap().get("PERSON")).isInstanceOf(RestExecutorBatchLoader.class);
    assertThat(runtimeGraph.getBatchLoaderMap().get("EPS")).isInstanceOf(GraphQLServiceBatchLoader.class);
  }

  @Test
  public void testThrowsExceptionOnDuplicateNamespace() {
    ServiceProvider sp1 = TestServiceProvider.newBuilder()
        .serviceType(ServiceType.REST)
        .namespace("PERSON")
        .sdlFiles(TestHelper.getFileMapFromList("top_level/person/schema1.graphqls"))
        .build();

    ServiceProvider sp2 = TestServiceProvider.newBuilder()
        .serviceType(ServiceType.GRAPHQL)
        .namespace("PERSON")
        .sdlFiles(TestHelper.getFileMapFromList("top_level/eps/schema2.graphqls"))
        .build();

    XtextStitcher stitcher = XtextStitcher.newBuilder().build();
    assertThatThrownBy(() -> stitcher.stitch(Arrays.asList(sp1, sp2)))
        .isInstanceOf(StitchingException.class)
        .hasMessageContainingAll("Duplicate Namespace", "PERSON");
  }
}
