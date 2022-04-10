package com.intuit.graphql.orchestrator.integration

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject


class XtextConflictResolverSpec extends BaseIntegrationTestSpecification {

    @Subject
    def specUnderTest

    def "Makes Correct Graph On Scalar Conflict"() {
        given:
        String schema1 = "type Query { foo : A } scalar A ";
        String schema2 = "type Query { bar : A } scalar A";

        when:
        specUnderTest = createGraphQLOrchestrator(Arrays.asList(
                TestServiceProvider.newBuilder().namespace("A").sdlFiles(ImmutableMap.of("schema1", schema1)).build(),
                TestServiceProvider.newBuilder().namespace("B").sdlFiles(ImmutableMap.of("schema2", schema2)).build()
        ))

        then:
        def queryType = specUnderTest?.runtimeGraph?.getOperationMap()?.get(Operation.QUERY);

        queryType != null
        queryType.getFieldDefinition("foo") != null
        queryType.getFieldDefinition("bar") != null
        queryType.getFieldDefinition("foo").type == queryType.getFieldDefinition("bar").type
    }

    def "Makes Correct Graph On In Built Scalar"() {
        given:
        String schema1 = "type Query { foo : Long }  ";
        String schema2 = "type Query { bar : Long } scalar Long";

        when:
        specUnderTest = createGraphQLOrchestrator(Arrays.asList(
                TestServiceProvider.newBuilder().namespace("A").sdlFiles(ImmutableMap.of("schema1", schema1)).build(),
                TestServiceProvider.newBuilder().namespace("B").sdlFiles(ImmutableMap.of("schema2", schema2)).build()
        ))

        then:
        def queryType = specUnderTest?.runtimeGraph?.getOperationMap()?.get(Operation.QUERY);

        queryType != null
        queryType.getFieldDefinition("foo") != null
        queryType.getFieldDefinition("bar") != null
        queryType.getFieldDefinition("foo").type == queryType.getFieldDefinition("bar").type

    }

    def "Makes Correct Graph On Golden Type"() {
        given:
        String schema1 = "type Query { foo: PageInfo } type PageInfo { id: String }";
        String schema2 = "type Query { bar: PageInfo } type PageInfo { id: String }";

        when:
        specUnderTest = createGraphQLOrchestrator(Arrays.asList(
                TestServiceProvider.newBuilder().namespace("A").sdlFiles(ImmutableMap.of("schema1", schema1)).build(),
                TestServiceProvider.newBuilder().namespace("B").sdlFiles(ImmutableMap.of("schema2", schema2)).build()
        ))

        then:
        def queryType = specUnderTest?.runtimeGraph?.getOperationMap()?.get(Operation.QUERY);

        queryType != null
        queryType.getFieldDefinition("foo") != null
        queryType.getFieldDefinition("bar") != null
        queryType.getFieldDefinition("foo").type == queryType.getFieldDefinition("bar").type
    }

    def "Makes Correct Graph On Golden Interface"() {
        given:
        String schema1 = "type Query { foo: Node } interface Node { id: String } type foo implements Node {id: String}";
        String schema2 = "type Query { bar: Node } interface Node { id: String } type bar implements Node {id: String}";

        when:
        specUnderTest = createGraphQLOrchestrator(Arrays.asList(
                TestServiceProvider.newBuilder().namespace("A").sdlFiles(ImmutableMap.of("schema1", schema1)).build(),
                TestServiceProvider.newBuilder().namespace("B").sdlFiles(ImmutableMap.of("schema2", schema2)).build()
        ))

        then:
        def queryType = specUnderTest?.runtimeGraph?.getOperationMap()?.get(Operation.QUERY);

        queryType != null
        queryType.getFieldDefinition("foo") != null
        queryType.getFieldDefinition("bar") != null
        queryType.getFieldDefinition("foo").type == queryType.getFieldDefinition("bar").type
    }
}
