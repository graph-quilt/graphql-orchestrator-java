package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.execution.AsyncExecutionStrategy
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.GraphQLOrchestratorTest.createGraphQLOrchestrator

class JavaPrimitiveScalarSpec extends Specification {

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

    ServiceProvider testService = new SimpleMockServiceProvider().builder()
            .sdlFiles(["schema.graphqls": testSchema])
            .mockResponse(mockServiceResponse)
            .build()

    def specUnderTest = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
            new AsyncExecutionStrategy(), testService)

    def "Extended Scalars for Primitive types are stitched and queried"() {
        given:
        ExecutionInput query = ExecutionInput.newExecutionInput()
                .query("{ a b c d e f g }")
                .build()

        when:
        ExecutionResult executionResult = specUnderTest.execute(query).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.a instanceof Long
        data.b instanceof Short
        data.c instanceof Byte
        data.d instanceof BigDecimal
        data.e instanceof BigInteger
        data.f instanceof Character
        data.g instanceof Integer

        data.a == Long.MAX_VALUE
        data.b == Short.MAX_VALUE
        data.c == Byte.MAX_VALUE
        data.d == BigDecimal.valueOf(Double.MAX_VALUE)
        data.e == new BigInteger("1234567890987654321")
        data.f == Character.MAX_VALUE
        data.g == Integer.MAX_VALUE
    }
}
