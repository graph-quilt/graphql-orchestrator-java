package com.intuit.graphql.orchestrator.integration.authorization

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.authorization.FieldAuthorization
import com.intuit.graphql.orchestrator.authorization.FieldAuthorizationEnvironment
import com.intuit.graphql.orchestrator.authorization.FieldAuthorizationResult
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQLContext
import graphql.GraphqlErrorException
import graphql.language.Field
import graphql.schema.FieldCoordinates
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

import java.util.concurrent.CompletableFuture

class FieldAuthorizationTopLevelFieldsSpec extends BaseIntegrationTestSpecification {

    def testSchemaA = """
        type Query {
            fieldA(objectArgA: InputA) : String
        }
        
        input InputA {
            s: String
        }
    """

    def mockServiceResponseA = [
            data: [
                    fieldA: "SomeStringA"
            ]
    ]

    def testSchemaB = """
        type Query {
            fieldB(stringArgB: String) : String
        }
    """

    def mockServiceResponseB = [
            data: [
                    fieldB: "SomeStringB"
            ]
    ]

    def testSchemaC = """
        type Query {
            fieldC1 : String
            fieldC2 : String
        }
    """

    def QUERY_C2_ONLY = "query TestQuery {fieldC2}"
    def QUERY_C1C2 = "query TestQuery {fieldC1 fieldC2}"
    def mockServiceResponseC = [
            (QUERY_C2_ONLY): [data: [
                    fieldC2: "SomeStringC2"
            ]],
            (QUERY_C1C2): [data: [
                    fieldC1: "SomeStringC1",
                    fieldC2: "SomeStringC2"
            ]]

    ]


    @Subject
    GraphQLOrchestrator specUnderTest

    def testServiceA
    def testServiceB
    def testServiceC

    GraphQLContext mockGraphQLContext = Mock()
    FieldAuthorization mockFieldAuthorization = Mock()
    Map<String, Object> authData = Collections.emptyMap();
    Map<String, Object> argumentValues = Collections.emptyMap();

    void setup() {
        testServiceA = createSimpleMockService("testServiceA", testSchemaA, mockServiceResponseA)
        testServiceB = createSimpleMockService("testServiceB", testSchemaB, mockServiceResponseB)
        testServiceC = createQueryMatchingService("testServiceC", testSchemaC, mockServiceResponseC)
        specUnderTest = createGraphQLOrchestrator([testServiceA, testServiceB, testServiceC])
    }

    def "Single field selected denied access, empty data is returned"() {
        given:
        String expectedErrorMessage = "access to fieldA denied.";
        List<Object> fieldAPath = Arrays.asList("fieldA");

        FieldAuthorizationEnvironment expectedFieldAuthorizationEnvironment = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("Query", "fieldA"))
                .field(Field.newField("fieldA").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldAPath)
                .build()

        mockGraphQLContext.get(FieldAuthorization.class) >> mockFieldAuthorization
        mockFieldAuthorization.getFutureAuthData() >> CompletableFuture.completedFuture(authData);
        mockFieldAuthorization.authorize(expectedFieldAuthorizationEnvironment) >> FieldAuthorizationResult
                .createDeniedResult(
                        GraphqlErrorException.newErrorException()
                                .message(expectedErrorMessage)
                                .path(fieldAPath)
                                .build()
                )

        def graphqlQuery = '''
            query TestQuery($objectVarA: InputA){
                fieldA(objectArgA: $objectVarA)
            }
        '''

        def variables = [
                objectVarA: [s: "String Input"]
        ]

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)
        executionInput = executionInput.transform { builder -> builder.context(mockGraphQLContext) }

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().size() == 1
        executionResult?.errors[0]?.message == expectedErrorMessage
        executionResult?.data?.fieldA == null
    }

    def "Two fields from difference services selected, allowed access, returns expected data without errors"() {
        given:
        List<Object> fieldAPath = Arrays.asList("fieldA");
        List<Object> fieldBPath = Arrays.asList("fieldB");

        FieldAuthorizationEnvironment expectedFieldAuthEnvA = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("Query", "fieldA"))
                .field(Field.newField("fieldA").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldAPath)
                .build()

        FieldAuthorizationEnvironment expectedFieldAuthEnvB = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("Query", "fieldB"))
                .field(Field.newField("fieldB").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldBPath)
                .build()

        mockGraphQLContext.get(FieldAuthorization.class) >> mockFieldAuthorization
        mockFieldAuthorization.getFutureAuthData() >> CompletableFuture.completedFuture(authData);
        mockFieldAuthorization.authorize(expectedFieldAuthEnvA) >> FieldAuthorizationResult
                .ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(expectedFieldAuthEnvB) >> FieldAuthorizationResult
                .ALLOWED_FIELD_AUTH_RESULT

        def graphqlQuery = '''
            query TestQuery($objectVarA: InputA, $stringVarB: String){
                fieldA(objectArgA: $objectVarA)
                fieldB(stringArgB: $stringVarB)
            }
        '''

        def variables = [
                objectVarA: [ s : "String Input" ],
                stringVarB: "StringVarValueB"
        ]

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)
        executionInput = executionInput.transform { builder -> builder.context(mockGraphQLContext) }

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().size() == 0
        executionResult?.data?.fieldA == "SomeStringA"
        executionResult?.data?.fieldB == "SomeStringB"
    }

    def "Two fields from different services selected, only one allowed access, returns expected data with one error"() {
        given:

        String expectedErrorMessage = "access to fieldA denied.";

        List<Object> fieldAPath = Arrays.asList("fieldA");
        List<Object> fieldBPath = Arrays.asList("fieldB");

        FieldAuthorizationEnvironment expectedFieldAuthEnvA = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("Query", "fieldA"))
                .field(Field.newField("fieldA").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldAPath)
                .build()

        FieldAuthorizationEnvironment expectedFieldAuthEnvB = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("Query", "fieldB"))
                .field(Field.newField("fieldB").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldBPath)
                .build()

        mockGraphQLContext.get(FieldAuthorization.class) >> mockFieldAuthorization
        mockFieldAuthorization.getFutureAuthData() >> CompletableFuture.completedFuture(authData);
        mockFieldAuthorization.authorize(expectedFieldAuthEnvA) >> FieldAuthorizationResult
                .createDeniedResult(
                        GraphqlErrorException.newErrorException()
                                .message(expectedErrorMessage)
                                .path(fieldAPath)
                                .build()
                )

        mockFieldAuthorization.authorize(expectedFieldAuthEnvB) >> FieldAuthorizationResult
                .ALLOWED_FIELD_AUTH_RESULT

        def graphqlQuery = '''
            query TestQuery($objectVarA: InputA, $stringVarB: String){
                fieldA(objectArgA: $objectVarA)
                fieldB(stringArgB: $stringVarB)
            }
        '''

        def variables = [
                objectVarA: [ s : "String Input" ],
                stringVarB: "StringVarValueB"
        ]

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)
        executionInput = executionInput.transform { builder -> builder.context(mockGraphQLContext) }

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().size() == 1
        executionResult?.errors[0]?.message == expectedErrorMessage
        executionResult?.data?.fieldA == null
        executionResult?.data?.fieldB == "SomeStringB"
    }

    def "Two fields from same service selected, only one allowed access, returns expected data with one error"() {
        given:
        String expectedErrorMessage = "access to fieldC1 denied.";

        List<Object> fieldAPath = Arrays.asList("fieldC1");
        List<Object> fieldBPath = Arrays.asList("fieldC2");

        FieldAuthorizationEnvironment expectedFieldAuthEnvC1 = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("Query", "fieldC1"))
                .field(Field.newField("fieldC1").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldAPath)
                .build()

        FieldAuthorizationEnvironment expectedFieldAuthEnvC2 = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("Query", "fieldC2"))
                .field(Field.newField("fieldC2").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldBPath)
                .build()

        mockGraphQLContext.get(FieldAuthorization.class) >> mockFieldAuthorization
        mockFieldAuthorization.getFutureAuthData() >> CompletableFuture.completedFuture(authData);
        mockFieldAuthorization.authorize(expectedFieldAuthEnvC1) >> FieldAuthorizationResult
                .createDeniedResult(
                        GraphqlErrorException.newErrorException()
                                .message(expectedErrorMessage)
                                .path(fieldAPath)
                                .build()
                )

        mockFieldAuthorization.authorize(expectedFieldAuthEnvC2) >> FieldAuthorizationResult
                .ALLOWED_FIELD_AUTH_RESULT

        def graphqlQuery = '''
            query TestQuery {
                fieldC1
                fieldC2
            }
        '''

        def variables = Collections.emptyMap()

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)
        executionInput = executionInput.transform { builder -> builder.context(mockGraphQLContext) }

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().size() == 1
        executionResult?.errors[0]?.message == expectedErrorMessage
        executionResult?.data?.fieldC1 == null
        executionResult?.data?.fieldC2 == "SomeStringC2"
    }

    def "Two fields from same service selected, both allowed access, returns expected data with one error"() {
        given:
        String expectedErrorMessage = "access to fieldC1 denied.";

        List<Object> fieldAPath = Arrays.asList("fieldC1");
        List<Object> fieldBPath = Arrays.asList("fieldC2");

        FieldAuthorizationEnvironment expectedFieldAuthEnvC1 = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("Query", "fieldC1"))
                .field(Field.newField("fieldC1").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldAPath)
                .build()

        FieldAuthorizationEnvironment expectedFieldAuthEnvC2 = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("Query", "fieldC2"))
                .field(Field.newField("fieldC2").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldBPath)
                .build()

        mockGraphQLContext.get(FieldAuthorization.class) >> mockFieldAuthorization
        mockFieldAuthorization.getFutureAuthData() >> CompletableFuture.completedFuture(authData);
        mockFieldAuthorization.authorize(expectedFieldAuthEnvC1) >> FieldAuthorizationResult
                .ALLOWED_FIELD_AUTH_RESULT

        mockFieldAuthorization.authorize(expectedFieldAuthEnvC2) >> FieldAuthorizationResult
                .ALLOWED_FIELD_AUTH_RESULT

        def graphqlQuery = '''
            query TestQuery {
                fieldC1
                fieldC2
            }
        '''

        def variables = Collections.emptyMap()

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)
        executionInput = executionInput.transform { builder -> builder.context(mockGraphQLContext) }

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().size() == 0
        executionResult?.data?.fieldC1 == "SomeStringC1"
        executionResult?.data?.fieldC2 == "SomeStringC2"
    }

}
