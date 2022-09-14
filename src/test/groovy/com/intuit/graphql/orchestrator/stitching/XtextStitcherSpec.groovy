package com.intuit.graphql.orchestrator.stitching

import com.intuit.graphql.graphQL.FieldDefinition
import com.intuit.graphql.graphQL.ObjectTypeDefinition
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType
import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.TestHelper.DefaultTestServiceProvider
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.batch.GraphQLServiceBatchLoader
import com.intuit.graphql.orchestrator.datafetcher.ResolverArgumentDataFetcher
import com.intuit.graphql.orchestrator.datafetcher.RestDataFetcher
import com.intuit.graphql.orchestrator.datafetcher.ServiceDataFetcher
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.schema.RuntimeGraph
import com.intuit.graphql.orchestrator.schema.transform.Transformer
import com.intuit.graphql.orchestrator.utils.XtextUtils
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext
import com.intuit.graphql.orchestrator.xtext.FieldContext
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import org.eclipse.xtext.resource.XtextResourceSet
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType.RESOLVER_ARGUMENT
import static graphql.schema.FieldCoordinates.coordinates
import static java.util.Collections.singletonList

class XtextStitcherSpec extends Specification {

    private static final String schema = '''
        type TestType { 
            test_field(arg: Int @resolver(field: "a.b.c")): Int 
        }
        directive @resolver(field: String) on ARGUMENT_DEFINITION
    '''

    def "builds Resolver Argument Data Fetchers"() {
        given:
        FieldContext testFieldContext = new FieldContext("TestType", "test_field")

        final XtextResourceSet testSchema = XtextResourceSetBuilder.newBuilder()
                .file("test_schema", schema).build()

        final ObjectTypeDefinition testType = XtextUtils.getObjectType("TestType", testSchema)
        final FieldDefinition testField = testType.getFieldDefinition().get(0)

        Transformer<UnifiedXtextGraph, UnifiedXtextGraph> transformer = { source ->
            //final UnifiedXtextGraph graphWithResolver = UnifiedXtextGraph.emptyGraph()
            source.getCodeRegistry().put(testFieldContext, DataFetcherContext.newBuilder()
                    .namespace("test_namespace")
                    .dataFetcherType(RESOLVER_ARGUMENT)
                    .build())
            source.getResolverArgumentFields().put(testFieldContext, testField.getArgumentsDefinition())

            return source
        }

        XtextStitcher xtextStitcher = XtextStitcher.newBuilder()
                .preMergeTransformers(Collections.emptyList())
                .postMergeTransformers(Collections.singletonList(transformer))
                .build()

        when:
        final RuntimeGraph runtimeGraph = xtextStitcher.stitch(singletonList(new DefaultTestServiceProvider()))

        then:
        runtimeGraph.getCodeRegistry().build().getDataFetcher(coordinates(
                "TestType", "test_field"), Mock(GraphQLFieldDefinition.class)) in
                ResolverArgumentDataFetcher
    }

    def "test Batch Loaders Are Present In Runtime Graph"() {
        given:
        ServiceProvider sp1 = TestServiceProvider.newBuilder()
                .serviceType(ServiceType.REST)
                .namespace("PERSON")
                .sdlFiles(TestHelper.getFileMapFromList("top_level/person/schema1.graphqls"))
                .build()

        ServiceProvider sp2 = TestServiceProvider.newBuilder()
                .serviceType(ServiceType.GRAPHQL)
                .namespace("EPS")
                .sdlFiles(TestHelper.getFileMapFromList("top_level/eps/schema2.graphqls"))
                .build()

        when:
        XtextStitcher stitcher = XtextStitcher.newBuilder().build()
        RuntimeGraph runtimeGraph = stitcher.stitch(Arrays.asList(sp1, sp2))

        GraphQLObjectType query = runtimeGraph.getOperation(Operation.QUERY)
        GraphQLObjectType mutation = runtimeGraph.getOperation(Operation.MUTATION)

        then:
        runtimeGraph.getBatchLoaderMap().size() == 1

        runtimeGraph.getCodeRegistry().getDataFetcher(
                FieldCoordinates.coordinates("Query", "person"),
                        query.getFieldDefinition("person")) in RestDataFetcher
        runtimeGraph.getCodeRegistry().getDataFetcher(
                FieldCoordinates.coordinates("Query", "personById"),
                        query.getFieldDefinition("personById")) in RestDataFetcher
        runtimeGraph.getCodeRegistry().getDataFetcher(
                FieldCoordinates.coordinates("Query", "Profile"),
                        query.getFieldDefinition("Profile")) in ServiceDataFetcher
        runtimeGraph.getCodeRegistry().getDataFetcher(
                FieldCoordinates.coordinates("Query", "ExpertDetails"),
                        query.getFieldDefinition("ExpertDetails")) in ServiceDataFetcher
        runtimeGraph.getCodeRegistry().getDataFetcher(
                FieldCoordinates.coordinates("Query", "searchProfile"),
                        query.getFieldDefinition("person")) in ServiceDataFetcher
        runtimeGraph.getCodeRegistry().getDataFetcher(
                FieldCoordinates.coordinates("Mutation", "upsertProfile"),
                        mutation.getFieldDefinition("upsertProfile")) in ServiceDataFetcher.class

        //assertThat(runtimeGraph.getBatchLoaderMap().get("PERSON")).isInstanceOf(RestExecutorBatchLoader.class)
        runtimeGraph.getBatchLoaderMap().get("EPS") in GraphQLServiceBatchLoader
    }

    def "test Throws Exception On Duplicate Namespace"() {
        given:
        ServiceProvider sp1 = TestServiceProvider.newBuilder()
                .serviceType(ServiceType.REST)
                .namespace("PERSON")
                .sdlFiles(TestHelper.getFileMapFromList("top_level/person/schema1.graphqls"))
                .build()

        ServiceProvider sp2 = TestServiceProvider.newBuilder()
                .serviceType(ServiceType.GRAPHQL)
                .namespace("PERSON")
                .sdlFiles(TestHelper.getFileMapFromList("top_level/eps/schema2.graphqls"))
                .build()

        XtextStitcher stitcher = XtextStitcher.newBuilder().build()

        when:
        stitcher.stitch([sp1, sp2])

        then:
        def exception = thrown(StitchingException)
        exception.getMessage() ==~ /^.*Duplicate Namespace.*PERSON.*$/
    }
}
