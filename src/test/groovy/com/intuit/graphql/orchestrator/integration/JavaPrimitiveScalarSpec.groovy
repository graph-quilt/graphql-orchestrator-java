package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import graphql.ExecutionInput
import graphql.ExecutionResult
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class JavaPrimitiveScalarSpec extends BaseIntegrationTestSpecification {

    def testSchema = """
        type Query {
            a: GraphQLLong
            b: GraphQLShort
            c: GraphQLByte
            d: GraphQLBigDecimal
            e: GraphQLBigInteger
            f: GraphQLChar
            g: Int # standard scalar
        }
        
        scalar GraphQLLong
        scalar GraphQLShort
        scalar GraphQLByte
        scalar GraphQLBigDecimal
        scalar GraphQLBigInteger
        scalar GraphQLChar
    """

    def mockServiceResponse = [
            data: [
                    a: Long.MAX_VALUE,
                    b: Short.MAX_VALUE,
                    c: Byte.MAX_VALUE,
                    d: BigDecimal.valueOf(Double.MAX_VALUE),
                    e: new BigInteger("1234567890987654321"),
                    f: Character.MAX_VALUE,
                    g: Integer.MAX_VALUE
            ]
    ]

    @Subject
    GraphQLOrchestrator specUnderTest

    void setup() {
        testService = createSimpleMockService(testSchema, mockServiceResponse)
        specUnderTest = createGraphQLOrchestrator(testService)
    }

    def "Extended Scalars for Primitive types are stitched and queried"() {
        given:
        def graphqlQuery = "{ a b c d e f g }"

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        compareQueryToExecutionInput(Operation.QUERY, graphqlQuery, (SimpleMockServiceProvider) testService);
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.a instanceof Long && data.a == Long.MAX_VALUE
        data.b instanceof Short && data.b == Short.MAX_VALUE
        data.c instanceof Byte && data.c == Byte.MAX_VALUE
        data.d instanceof BigDecimal && data.d == BigDecimal.valueOf(Double.MAX_VALUE)
        data.e instanceof BigInteger && data.e == new BigInteger("1234567890987654321")
        data.f instanceof Character && data.f == Character.MAX_VALUE
        data.g instanceof Integer && data.g == Integer.MAX_VALUE
    }
}
