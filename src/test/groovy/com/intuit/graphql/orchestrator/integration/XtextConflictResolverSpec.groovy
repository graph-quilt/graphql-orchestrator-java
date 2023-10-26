package com.intuit.graphql.orchestrator.integration

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import graphql.ExecutionInput
import graphql.ExecutionResult
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

import static com.intuit.graphql.orchestrator.testhelpers.JsonTestUtils.jsonToMap

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

    def alphaObjectSchema = """
            schema {
                query: Query
            }

            type Query {
                a: MyType1
            }

            type MyType1 {
                test: String
                alpha: String
            }
        """

    def alphaObjectResponse = """
            {
              "data": {
                "a": {
                  "test": "1..2..3",
                  "alpha": "Alpha"
                }
              }
            }
        """

    def betaObjectSchema = """
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE

            schema {
                query: Query
            }

            type Query {
                b: MyType1
            }

            type MyType1 @rename(to: "beta") {
                test: String
                beta: String
            }
        """

    def betaObjectResponse = """
            {
              "data": {
                "b": {
                  "test": "One..Two..Three",
                  "beta": "Beta"
                }
              }
            }
        """

    def "Query Service A"() {
        given:


        SimpleMockServiceProvider serviceA = createMockService(
            "SVC_A", alphaObjectSchema, alphaObjectResponse)

        SimpleMockServiceProvider serviceB = createMockService(
            "SVC_B", betaObjectSchema, betaObjectResponse)

        specUnderTest = createGraphQLOrchestrator([serviceA, serviceB])

        def graphqlQuery = """
            query {
              a { 
                test
                alpha
              }
            }
        """

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        compareQueryToExecutionInput(null,
                "query QUERY { a { test alpha } }", (SimpleMockServiceProvider) serviceA)
        compareQueryToExecutionInput(null,
                null, (SimpleMockServiceProvider) serviceB)
        executionResult.getErrors().isEmpty()

        specUnderTest.runtimeGraph?.getType("MyType1") != null
        specUnderTest.runtimeGraph?.getType("beta") != null

        Map<String, Object> data = executionResult.getData()
        data.a?.test instanceof String && data.a?.test == "1..2..3"
        data.a?.alpha instanceof String && data.a?.alpha == "Alpha"

    }

    def "Query Service B With Renamed Object"() {
        given:

        SimpleMockServiceProvider serviceA = createMockService(
            "SVC_A", alphaObjectSchema, alphaObjectResponse)

        SimpleMockServiceProvider serviceB = createMockService(
            "SVC_B", betaObjectSchema, betaObjectResponse)

        specUnderTest = createGraphQLOrchestrator([serviceA, serviceB])

        def graphqlQuery = """
            query {
              b { 
                test
                beta
              }
            }
        """

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        compareQueryToExecutionInput(null,
                null, (SimpleMockServiceProvider) serviceA)
        compareQueryToExecutionInput(null,
                "query QUERY { b { test beta } }", (SimpleMockServiceProvider) serviceB)
        executionResult.getErrors().isEmpty()

        specUnderTest.runtimeGraph?.getType("MyType1") != null
        specUnderTest.runtimeGraph?.getType("beta") != null

        Map<String, Object> data = executionResult.getData()
        data.b?.test instanceof String && data.b?.test == "One..Two..Three"
        data.b?.beta instanceof String && data.b?.beta == "Beta"
    }

    def alphaFieldSchema = """
            schema {
                query: Query
            }

            type Query {
                a: MyTypeA
            }

            type MyTypeA {
                test: String
                alpha: String
            }
        """

    def alphaFieldResponse = """
            {
              "data": {
                "a": {
                  "test": "1..2..3",
                  "alpha": "Alpha"
                }
              }
            }
        """

    def betaFieldSchema = """
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE

            schema {
                query: Query
            }

            type Query {
                b: MyTypeB @rename(to: "bb")
            }

            type MyTypeB {
                test: String
                beta: String
            }
        """

    def betaFieldResponse = """
            {
              "data": {
                "bb": {
                  "test": "One..Two..Three",
                  "beta": "Beta"
                }
              }
            }
        """

    def "Query Service A With Field"() {
        given:

        SimpleMockServiceProvider serviceA = createMockService(
            "SVC_A", alphaFieldSchema, alphaFieldResponse)

        SimpleMockServiceProvider serviceB = createMockService(
            "SVC_B", betaFieldSchema, betaFieldResponse)

        specUnderTest = createGraphQLOrchestrator([serviceA, serviceB])

        def graphqlQuery = """
            query {
              a { 
                test
                alpha
              }
            }
        """

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        compareQueryToExecutionInput(null,
                "query QUERY { a { test alpha } }", (SimpleMockServiceProvider) serviceA)
        compareQueryToExecutionInput(null,
                null, (SimpleMockServiceProvider) serviceB)
        executionResult.getErrors().isEmpty()

        specUnderTest.runtimeGraph?.getExecutableSchema().getType("Query").getFieldDefinition("b") == null
        specUnderTest.runtimeGraph?.getExecutableSchema().getType("Query").getFieldDefinition("bb") != null
        specUnderTest.runtimeGraph?.getType("MyTypeB") != null

        Map<String, Object> data = executionResult.getData()
        data.a?.test instanceof String && data.a?.test == "1..2..3"
        data.a?.alpha instanceof String && data.a?.alpha == "Alpha"
    }

    def "Query Service B With Renamed Field"() {
        given:

        SimpleMockServiceProvider serviceA = createMockService(
            "SVC_A", alphaFieldSchema, alphaFieldResponse)

        SimpleMockServiceProvider serviceB = createMockService(
            "SVC_B", betaFieldSchema, betaFieldResponse)

        specUnderTest = createGraphQLOrchestrator([serviceA, serviceB])

        def graphqlQuery = """
            query {
              bb {
                test
                beta
              }
            }
        """

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        compareQueryToExecutionInput(null,
                null, (SimpleMockServiceProvider) serviceA)
        compareQueryToExecutionInput(null,
                "query QUERY { bb { test beta } }", (SimpleMockServiceProvider) serviceB)
        executionResult.getErrors().isEmpty()

        specUnderTest.runtimeGraph?.getExecutableSchema().getType("Query").getFieldDefinition("b") == null
        specUnderTest.runtimeGraph?.getExecutableSchema().getType("Query").getFieldDefinition("bb") != null
        specUnderTest.runtimeGraph?.getType("MyTypeB") != null

        Map<String, Object> data = executionResult.getData()
        data.bb?.test instanceof String && data.bb?.test == "One..Two..Three"
        data.bb?.beta instanceof String && data.bb?.beta == "Beta"
    }

    def alphaInterfaceSchema = """
            schema {
                query: Query
            }

            interface IName {
                name: String!
            }

            type Query {
                a: MyTypeA
            }

            type MyTypeA implements IName {
                name: String!
                test: String
                alpha: String
            }
        """

    def alphaInterfaceResponse = """
            {
              "data": {
                "a": {
                  "name": "A A",
                  "test": "1..2..3",
                  "alpha": "Alpha"
                }
              }
            }
        """

    def betaInterfaceSchema = """
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE

            schema {
                query: Query
            }

            interface IName @rename(to: "ITheName") {
                name: String!
            }

            type Query {
                b: MyType
            }

            type MyType implements IName {
                name: String!
                test: String
                beta: String
            }
        """

    def betaInterfaceRepsonse = """
            {
              "data": {
                "b": {
                  "name": "B B",
                  "test": "One..Two..Three",
                  "beta": "Beta"
                }
              }
            }
        """

    def "Query Service I With Interface"() {
        given:

        SimpleMockServiceProvider serviceA = createMockService(
            "SVC_A", alphaInterfaceSchema, alphaInterfaceResponse)


        SimpleMockServiceProvider serviceB = createMockService(
            "SVC_B", betaInterfaceSchema, betaInterfaceRepsonse )

        specUnderTest = createGraphQLOrchestrator([serviceA, serviceB])

        def graphqlQuery = """
            fragment NameFragment on IName {
                name
            }
            query {
                a {
                    ...NameFragment
                    test
                    alpha
                }
            }
        """

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        compareQueryToExecutionInput(null,
                "fragment NameFragment on IName { name } query QUERY { a {...NameFragment test alpha}}", (SimpleMockServiceProvider) serviceA)
        compareQueryToExecutionInput(null,
                null, (SimpleMockServiceProvider) serviceB)
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.a?.name instanceof String && data.a?.name == "A A"
        data.a?.test instanceof String && data.a?.test == "1..2..3"
        data.a?.alpha instanceof String && data.a?.alpha == "Alpha"
    }

    def "Query Service B With Renamed Interface"() {
        given:

        SimpleMockServiceProvider serviceA = createMockService(
            "SVC_A", alphaInterfaceSchema, alphaInterfaceResponse)

        SimpleMockServiceProvider serviceB = createMockService(
            "SVC_B", betaInterfaceSchema, betaInterfaceRepsonse)

        specUnderTest = createGraphQLOrchestrator([serviceA, serviceB])

        def graphqlQuery = """
            fragment NameFragment on ITheName {
                name
            }
            query {
                b {
                    ...NameFragment
                    test
                    beta
                }
            }
        """

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        compareQueryToExecutionInput(null,
                null, (SimpleMockServiceProvider) serviceA)
        compareQueryToExecutionInput(null,
                "fragment NameFragment on ITheName { name } query QUERY { b {...NameFragment test beta}}", (SimpleMockServiceProvider) serviceB)
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.b?.name instanceof String && data.b?.name == "B B"
        data.b?.test instanceof String && data.b?.test == "One..Two..Three"
        data.b?.beta instanceof String && data.b?.beta == "Beta"
    }

    SimpleMockServiceProvider createMockService(String namespace, String schema, String response) {
        return (SimpleMockServiceProvider)SimpleMockServiceProvider.builder()
            .namespace(namespace)
            .sdlFiles(Collections.singletonMap("schema.graphqls", schema))
            .mockResponse(jsonToMap(response))
            .build();
    }

}
