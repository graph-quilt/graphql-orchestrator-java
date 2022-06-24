package com.intuit.graphql.orchestrator.schema.fold

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.schema.RuntimeGraph
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher
import graphql.ExecutionInput
import graphql.GraphQLContext
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import helpers.BaseIntegrationTestSpecification

import java.util.concurrent.CompletableFuture

class XtextGraphFolderSpec extends BaseIntegrationTestSpecification {

    void testScalarTypeConflictsGettingIgnoredXtextStrategy() {
        given:
        ServiceProvider serviceProvider1 = new GenericTestService("SomeService1",
                "schema { query: Query } type Query { a: Date } scalar Date")

        ServiceProvider serviceProvider2 = new GenericTestService("SomeService2",
                "schema { query: Query } type Query { b: Date } scalar Date")

        RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder()
                .services([serviceProvider1, serviceProvider2]).build().stitchGraph()

        when:
        GraphQLObjectType query = runtimeGraph.getOperationMap().get(Operation.QUERY)

        then:
        query != null
        query.getFieldDefinition("b") != null
        ((GraphQLNamedType) query.getFieldDefinition("b").getType()).getName() == "Date"
    }

    void testGoldenTypeSignatureConflictMissingNonNullFieldThrowsException() {
        given:
        ServiceProvider serviceProvider1 = new GenericTestService("SomeService1",
                "schema { query: Query } type Query { a: PageInfo } type PageInfo { foo: String! bar:String! }")

        ServiceProvider serviceProvider2 = new GenericTestService("SomeService2",
                "schema { query: Query } type Query { b: PageInfo } type PageInfo { foo: String! }")

        when:
        SchemaStitcher schemaStitcher = SchemaStitcher.newBuilder()
                .services([ serviceProvider1, serviceProvider2 ]).build()

        schemaStitcher.stitchGraph();

        then:
        thrown(TypeConflictException)
    }

    void testGoldenTypeSignatureConflictThrowsException() {
        given:
        ServiceProvider serviceProvider1 = new GenericTestService("SomeService1",
                "schema { query: Query } type Query { a: PageInfo } type PageInfo { id: ID }")

        ServiceProvider serviceProvider2 = new GenericTestService("SomeService2",
                "schema { query: Query } type Query { b: PageInfo } type PageInfo { id: String! }")

        when:
        SchemaStitcher schemaStitcher = SchemaStitcher.newBuilder()
                .services([ serviceProvider1, serviceProvider2 ]).build()

        schemaStitcher.stitchGraph()

        then:
        thrown(TypeConflictException)
    }

    void testGoldenInterfaceConflictThrowsException() {
        given:
        ServiceProvider serviceProvider1 = new GenericTestService("SomeService1",
                "schema { query: Query } type Query { a: Node } interface Node { id: String! }")

        ServiceProvider serviceProvider2 = new GenericTestService("SomeService2",
                "schema { query: Query } type Query { b: Node } interface Node { id: ID }")

        when:
        SchemaStitcher schemaStitcher = SchemaStitcher.newBuilder()
                .services([ serviceProvider1, serviceProvider2 ]).build()

        schemaStitcher.stitchGraph()

        then:
        thrown(TypeConflictException)
    }

    static class GenericTestService implements ServiceProvider {

        private final String schema
        private final String namespace
        private final Set<String> domainTypes

        GenericTestService(String namespace, String schema) {
            this.namespace = namespace
            this.schema = schema
            this.domainTypes = null
        }

        GenericTestService(String namespace, Set<String> domainTypes, String schema) {
            this.namespace = namespace
            this.schema = schema
            this.domainTypes = domainTypes
        }

        @Override
        String getNameSpace() {
            return namespace
        }

        @Override
        Map<String, String> sdlFiles() {
            return ImmutableMap.of("schema.graphqls", this.schema)
        }

        @Override
        Set<String> domainTypes() {
            return this.domainTypes
        }

        @Override
        CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput, GraphQLContext context) {
            throw new RuntimeException("This should not be called in this test case.")
        }
    }
}
