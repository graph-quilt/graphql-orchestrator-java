package com.intuit.graphql.orchestrator.schema.transform

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.graphQL.ObjectTypeDefinition
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.schema.SchemaTransformationException
import com.intuit.graphql.orchestrator.schema.fold.UnifiedXtextGraphFolder
import com.intuit.graphql.orchestrator.schema.transform.GraphQLAdapterTransformer.AdapterDirectiveVisitor
import com.intuit.graphql.orchestrator.testhelpers.UnifiedXtextGraphBuilder
import com.intuit.graphql.orchestrator.xtext.*
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType
import lombok.extern.slf4j.Slf4j
import spock.lang.Specification

@Slf4j
class GraphqlAdapterTransformerSpec extends Specification {

    public static String directive = "directive @adapter(service:String!) on FIELD_DEFINITION"

    def "test Adapter Directive Nested"() {
        given:
        String schema = """
            schema { query: Query }
            type Query { a: A }
            type A { b: B }
            type B {c: C}
            type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter(service: 'bar') }
            type D { field: String }
            $directive
        """

        String schema2 = '''
            schema { query: Query }
            type Query { a: A } 
            type A {  bb: BB } 
            type BB {cc: String }
        '''

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC_b").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC_bb").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        UnifiedXtextGraph stitchedGraph = new UnifiedXtextGraphFolder()
                .fold(UnifiedXtextGraph.emptyGraph(), Arrays.asList(xtextGraph, xtextGraph2))

        when:
        UnifiedXtextGraph adapterGraph = new GraphQLAdapterTransformer().transform(stitchedGraph)

        then:
        ObjectTypeDefinition query = adapterGraph.getOperationMap().get(Operation.QUERY)
        adapterGraph.getCodeRegistry().get(new FieldContext("Query", "a")).getDataFetcherType() == DataFetcherType.STATIC
        adapterGraph.getCodeRegistry().get(new FieldContext("A", "b")).getDataFetcherType() == DataFetcherType.STATIC
        adapterGraph.getCodeRegistry().get(new FieldContext("A", "bb")).getDataFetcherType() == DataFetcherType.SERVICE
        adapterGraph.getCodeRegistry().get(new FieldContext("A", "bb")).getNamespace() ==  "SVC_bb"
        adapterGraph.getCodeRegistry().get(new FieldContext("B", "c")).getDataFetcherType() == DataFetcherType.STATIC
        adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter1")).getDataFetcherType() == DataFetcherType.SERVICE
        adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter2")).getDataFetcherType() == DataFetcherType.SERVICE
        adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter1")).getNamespace() == "SVC_b"
        adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter2")).getNamespace() == "SVC_b"
    }

    def "test Adapter Directive At Same Level As Rest"() {
        given:

        String schema = """
            schema { query: Query }
            type Query { a: A }
            type A { b: B @adapter(service: 'foo') }
            type B { d: D }
            type D { field: String}
            $directive
        """

        String schema2 = '''
            schema { query: Query }
            type Query { a: A } 
            type A {  bb: BB }
            type BB { cc: String }
        '''

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC_b").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC_bb").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        UnifiedXtextGraph stitchedGraph = new UnifiedXtextGraphFolder()
                .fold(UnifiedXtextGraph.emptyGraph(), Arrays.asList(xtextGraph, xtextGraph2))

        when:
        UnifiedXtextGraph adapterGraph = new GraphQLAdapterTransformer().transform(stitchedGraph)

        then:
        ObjectTypeDefinition query = adapterGraph.getOperationMap().get(Operation.QUERY)
        adapterGraph.getCodeRegistry().get(new FieldContext("Query", "a")).getDataFetcherType() == DataFetcherType.STATIC
        adapterGraph.getCodeRegistry().get(new FieldContext("A", "b")).getDataFetcherType() == DataFetcherType.SERVICE
        adapterGraph.getCodeRegistry().get(new FieldContext("A", "b")).getNamespace() == "SVC_b"
        adapterGraph.getCodeRegistry().get(new FieldContext("A", "bb")).getDataFetcherType() == DataFetcherType.SERVICE
        adapterGraph.getCodeRegistry().get(new FieldContext("A", "bb")).getNamespace() ==  "SVC_bb"
    }

    def "test Adapter Directive Only One Rest Service"() {
        given:
        String schema = """
            schema { query: Query }
            type Query { a: A } 
            type A { b: B }
            type B { c: C }
            type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter(service: 'bar') }
            type D { field: String}
            $directive
        """

        FieldContext fieldContext = new FieldContext("Query", "a")
        DataFetcherContext dataFetcherContext = DataFetcherContext.newBuilder().dataFetcherType(DataFetcherType.SERVICE)
                .namespace("SVC1")
                .serviceType(ServiceType.REST).build()

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        xtextGraph.transform({ builder -> builder.codeRegistry(ImmutableMap.of(fieldContext, dataFetcherContext)) })

        UnifiedXtextGraph stitchedGraph = new UnifiedXtextGraphFolder()
                .fold(UnifiedXtextGraph.emptyGraph(), Collections.singletonList(xtextGraph))

        when:
        UnifiedXtextGraph adapterGraph = new GraphQLAdapterTransformer().transform(stitchedGraph)

        then:
        ObjectTypeDefinition query = adapterGraph.getOperationMap().get(Operation.QUERY)
        adapterGraph.getCodeRegistry().get(new FieldContext("Query", "a")).getDataFetcherType() == DataFetcherType.STATIC
        adapterGraph.getCodeRegistry().get(new FieldContext("A", "b")).getDataFetcherType() == DataFetcherType.STATIC
        adapterGraph.getCodeRegistry().get(new FieldContext("B", "c")).getDataFetcherType() == DataFetcherType.STATIC
        adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter1")).getDataFetcherType() == DataFetcherType.SERVICE
        adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter2")).getDataFetcherType() == DataFetcherType.SERVICE
        adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter1")).getNamespace() == "SVC1"
        adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter2")).getNamespace() == "SVC1"
    }

    def "test Adapter Transformer Without Directive"() {
        given:
        String schema = """
            schema { query: Query }
            type Mutation { a: A }
            type Query { a: A }
            type A { b: B } type B { c: C }
            type C { noAdapter1: D  noAdapter2: D }
            type D { field: String }
        """

        FieldContext fieldContext = new FieldContext("Query", "a")
        DataFetcherContext dataFetcherContext = DataFetcherContext.newBuilder()
                .dataFetcherType(DataFetcherType.SERVICE)
                .namespace("SVC1")
                .serviceType(ServiceType.REST)
                .build()

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        xtextGraph.transform({ builder -> builder.codeRegistry(ImmutableMap.of(fieldContext, dataFetcherContext)) })

        UnifiedXtextGraph stitchedGraph = new UnifiedXtextGraphFolder()
                .fold(UnifiedXtextGraph.emptyGraph(), Collections.singletonList(xtextGraph))

        when:
        UnifiedXtextGraph adapterGraph = new GraphQLAdapterTransformer().transform(stitchedGraph)

        then:
        adapterGraph.getCodeRegistry().get(new FieldContext("Query", "a")) != null
        adapterGraph.getCodeRegistry().get(new FieldContext("Query", "a")).getNamespace() == "SVC1"
    }

    def "test Adapter Transformer For Non Rest"() {
        given:
        String schema = """
            schema { query: Query }
            type Query { a: A }
            type A { b: B }
            type B { c: C }
            type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter(service: 'bar') }
            type D { field: String }
            $directive
        """

        FieldContext fieldContext = new FieldContext("Query", "a")
        DataFetcherContext dataFetcherContext = DataFetcherContext.newBuilder()
                .dataFetcherType(DataFetcherType.SERVICE)
                .namespace("SVC1")
                .serviceType(ServiceType.GRAPHQL)
                .build()

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        xtextGraph.transform({ builder -> builder.codeRegistry(ImmutableMap.of(fieldContext, dataFetcherContext)) })

        UnifiedXtextGraph stitchedGraph = new UnifiedXtextGraphFolder()
                .fold(UnifiedXtextGraph.emptyGraph(), Collections.singletonList(xtextGraph))

        when:
        UnifiedXtextGraph adapterGraph = new GraphQLAdapterTransformer().transform(stitchedGraph)

        then:
        ObjectTypeDefinition query = adapterGraph.getOperationMap().get(Operation.QUERY)
        adapterGraph.getCodeRegistry().get(new FieldContext("Mutation", "a")) == null
    }

    def "test Adapter Transformer Without Field"() {
        given:
        String schema = """
            schema { query: Query }
            type Query { a: A }
            type A { b: B }
            type B { c: C }
            type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter(service: 'bar') }
            type D { field: String }
            $directive
        """

        FieldContext fieldContext = new FieldContext("Query", "foo")
        DataFetcherContext dataFetcherContext = DataFetcherContext.newBuilder().dataFetcherType(DataFetcherType.SERVICE)
                .namespace("SVC1")
                .serviceType(ServiceType.REST).build()

        UnifiedXtextGraph unifiedXtextGraph = UnifiedXtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        unifiedXtextGraph.transform({ builder -> builder.codeRegistry(ImmutableMap.of(fieldContext, dataFetcherContext)) })

        when:
        new GraphQLAdapterTransformer().transform(unifiedXtextGraph)

        then:
        def exception = thrown(SchemaTransformationException.class)
        exception.getMessage().contains(String.format(GraphQLAdapterTransformer.FIELD_NULL_ERROR, "foo"))
    }

    def "test Adapter Transformer For Mutation"() {
        given:
        String schema = """
            schema { mutation: Mutation }
            type Mutation { a: A } 
            type A { b: B }
            type B { c: C } 
            type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter(service: 'bar') }
            type D { field: String }
            $directive
        """

        FieldContext fieldContext = new FieldContext("Mutation", "a")
        DataFetcherContext dataFetcherContext = DataFetcherContext.newBuilder().dataFetcherType(DataFetcherType.SERVICE)
                .namespace("SVC1")
                .serviceType(ServiceType.REST).build()

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        xtextGraph.transform({ builder -> builder.codeRegistry(ImmutableMap.of(fieldContext, dataFetcherContext)) })

        UnifiedXtextGraph stitchedGraph = new UnifiedXtextGraphFolder()
                .fold(UnifiedXtextGraph.emptyGraph(), Collections.singletonList(xtextGraph))
        UnifiedXtextGraph adapterGraph = new GraphQLAdapterTransformer().transform(stitchedGraph)

        then:
        adapterGraph.getCodeRegistry().get(new FieldContext("Mutation", "a")).getDataFetcherType() == DataFetcherType.STATIC
        adapterGraph.getCodeRegistry().get(new FieldContext("A", "b")).getDataFetcherType() == DataFetcherType.STATIC
        adapterGraph.getCodeRegistry().get(new FieldContext("B", "c")).getDataFetcherType() == DataFetcherType.STATIC
        adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter1")).getDataFetcherType() == DataFetcherType.SERVICE
        adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter2")).getDataFetcherType() == DataFetcherType.SERVICE
        adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter1")).getNamespace() == "SVC1"
        adapterGraph.getCodeRegistry().get(new FieldContext("C", "adapter2")).getNamespace() == "SVC1"

        adapterGraph.getCodeRegistry().size() == 5
    }

    def "test Adapter Transformer Without Directive Argument"() {
        given:
        String schema = """
            schema { query: Query }
            type Query { a: A } 
            type A { b: B }
            type B { c: C } 
            type C { adapter1: D @adapter(service: 'foo'), adapter2: D @adapter }
            type D { field: String }
            $directive
        """

        FieldContext fieldContext = new FieldContext("Query", "a")
        DataFetcherContext dataFetcherContext = DataFetcherContext.newBuilder().dataFetcherType(DataFetcherType.SERVICE)
                .namespace("SVC1")
                .serviceType(ServiceType.REST).build()

        UnifiedXtextGraph unifiedXtextGraph = UnifiedXtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        unifiedXtextGraph.transform({ builder -> builder.codeRegistry(ImmutableMap.of(fieldContext, dataFetcherContext)) })
        new GraphQLAdapterTransformer().transform(unifiedXtextGraph)

        then:
        def exception = thrown(SchemaTransformationException)
        exception.getMessage().contains(AdapterDirectiveVisitor.ERROR_MSG)
    }
}
