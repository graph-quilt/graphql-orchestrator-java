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

class FieldAuthorizationNestedFieldsSpec extends BaseIntegrationTestSpecification {

    def testSchemaA = """
        type Query {
            topField : TopField            
        }
        
        type TopField {
            fieldA(stringArgA: String) : String
        }        
    """

    def mockServiceResponseA = [
            data: [
                    topField: [
                            fieldA: "SomeStringA"
                    ]
            ]
    ]

    def testSchemaB = """
        type Query {
            topField : TopField               
        }
        
        type TopField {
            fieldB(stringArgB: String) : String
        } 
    """

    def mockServiceResponseB = [
            data: [
                    topField: [
                            fieldB: "SomeStringB"
                    ]
            ]
    ]


    def testSchemaC = """
        type Query {
            topField : TopField               
        }
        
        type TopField {
            subField : SubField
        }
        
        type SubField {
            fieldC(stringArgC: String) : String
        } 
    """

    def mockServiceResponseC = [
            data: [
                    topField: [
                            subField: [
                                    fieldC: "SomeStringC"
                            ]

                    ]
            ]
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
        mockGraphQLContext.getOrDefault("useDefer", false) >> false

        testServiceA = createSimpleMockService("testServiceA", testSchemaA, mockServiceResponseA)
        testServiceB = createSimpleMockService("testServiceB", testSchemaB, mockServiceResponseB)
        testServiceC = createSimpleMockService("testServiceC", testSchemaC, mockServiceResponseC)
        specUnderTest = createGraphQLOrchestrator([testServiceA, testServiceB, testServiceC])
    }

    def "Single nested field selected denied access, null data is returned"() {
        given:
        String expectedErrorMessage = "access to fieldA denied.";
        List<Object> topFieldPath = Arrays.asList("topField");
        List<Object> fieldAPath = Arrays.asList("topField", "fieldA");

        FieldAuthorizationEnvironment expectedFieldAuthEnvTopField = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("Query", "topField"))
                .field(Field.newField("topField").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(topFieldPath)
                .build()

        FieldAuthorizationEnvironment expectedFieldAuthEnvFieldA = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("TopField", "fieldA"))
                .field(Field.newField("fieldA").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldAPath)
                .build()

        mockGraphQLContext.get(FieldAuthorization.class) >> mockFieldAuthorization
        mockFieldAuthorization.getFutureAuthData() >> CompletableFuture.completedFuture(authData);
        mockFieldAuthorization.authorize(expectedFieldAuthEnvTopField) >> FieldAuthorizationResult
                .ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(expectedFieldAuthEnvFieldA) >> FieldAuthorizationResult
                .createDeniedResult(
                        GraphqlErrorException.newErrorException()
                                .message(expectedErrorMessage)
                                .path(fieldAPath)
                                .build()
                )

        def graphqlQuery = '''
            query TestQuery($varA: String){
                topField {
                    fieldA(stringArgA: $varA)
                }
            }
        '''

        def variables = [
                varA: "String Input"
        ]

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)
        executionInput = executionInput.transform { builder -> builder.context(mockGraphQLContext) }

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().size() == 1
        executionResult?.errors[0]?.message == expectedErrorMessage
        executionResult?.data?.topField?.fieldA == null
    }

    def "Two nested field selected, one denied access, null data is returned for field denied access"() {
        given:
        String expectedErrorMessage = "access to fieldB denied.";
        List<Object> topFieldPath = Arrays.asList("topField")
        List<Object> fieldAPath = Arrays.asList("topField", "fieldA")
        List<Object> fieldBPath = Arrays.asList("topField", "fieldB")

        FieldAuthorizationEnvironment expectedFieldAuthEnvTopField = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("Query", "topField"))
                .field(Field.newField("topField").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(topFieldPath)
                .build()

        FieldAuthorizationEnvironment expectedFieldAuthEnvFieldA = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("TopField", "fieldA"))
                .field(Field.newField("fieldA").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldAPath)
                .build()

        FieldAuthorizationEnvironment expectedFieldAuthEnvFieldB = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("TopField", "fieldB"))
                .field(Field.newField("fieldB").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldBPath)
                .build()

        mockGraphQLContext.get(FieldAuthorization.class) >> mockFieldAuthorization
        mockFieldAuthorization.getFutureAuthData() >> CompletableFuture.completedFuture(authData);
        mockFieldAuthorization.authorize(expectedFieldAuthEnvTopField) >> FieldAuthorizationResult
                .ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(expectedFieldAuthEnvFieldA) >> FieldAuthorizationResult
                .createDeniedResult(
                        GraphqlErrorException.newErrorException()
                                .message(expectedErrorMessage)
                                .path(fieldAPath)
                                .build()
                )
        mockFieldAuthorization.authorize(expectedFieldAuthEnvFieldB) >> FieldAuthorizationResult
                .ALLOWED_FIELD_AUTH_RESULT

        def graphqlQuery = '''
            query TestQuery($varA: String){
                topField {
                    fieldA(stringArgA: $varA)
                    fieldB(stringArgB: "abc")
                }
            }
        '''

        def variables = [
                varA: "String Input"
        ]

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)
        executionInput = executionInput.transform { builder -> builder.context(mockGraphQLContext) }

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().size() == 1
        executionResult?.errors[0]?.message == expectedErrorMessage
        executionResult?.data?.topField?.fieldA == null
        executionResult?.data?.topField?.fieldB == "SomeStringB"
    }

    def "Deeper level field selected and denied access, null data is returned for field denied access"() {
        given:
        String expectedErrorMessage = "access to fieldC denied.";
        List<Object> topFieldPath = Arrays.asList("topField")
        List<Object> fieldBPath = Arrays.asList("topField", "fieldB")
        List<Object> subFieldPath = Arrays.asList("topField", "subField")
        List<Object> fieldCPath = Arrays.asList("topField", "subField", "fieldC")

        FieldAuthorizationEnvironment expectedFieldAuthEnvTopField = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("Query", "topField"))
                .field(Field.newField("topField").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(topFieldPath)
                .build()

        FieldAuthorizationEnvironment expectedFieldAuthEnvFieldB = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("TopField", "fieldB"))
                .field(Field.newField("fieldB").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldBPath)
                .build()

        FieldAuthorizationEnvironment expectedFieldAuthEnvSubField = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("TopField", "subField"))
                .field(Field.newField("subField").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(subFieldPath)
                .build()

        FieldAuthorizationEnvironment expectedFieldAuthEnvFieldC = FieldAuthorizationEnvironment
                .builder()
                .fieldCoordinates(FieldCoordinates.coordinates("SubField", "fieldC"))
                .field(Field.newField("fieldC").build())
                .authData(authData)
                .argumentValues(argumentValues)
                .path(fieldCPath)
                .build()

        mockGraphQLContext.get(FieldAuthorization.class) >> mockFieldAuthorization
        mockFieldAuthorization.getFutureAuthData() >> CompletableFuture.completedFuture(authData);
        mockFieldAuthorization.authorize(expectedFieldAuthEnvTopField) >> FieldAuthorizationResult
                .ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(expectedFieldAuthEnvFieldB) >> FieldAuthorizationResult
                .ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(expectedFieldAuthEnvSubField) >> FieldAuthorizationResult
                .ALLOWED_FIELD_AUTH_RESULT

        mockFieldAuthorization.authorize(expectedFieldAuthEnvFieldC) >> FieldAuthorizationResult
                .createDeniedResult(
                        GraphqlErrorException.newErrorException()
                                .message(expectedErrorMessage)
                                .path(fieldCPath)
                                .build()
                )

        def graphqlQuery = '''
            query TestQuery($varA: String){
                topField {                    
                    fieldB(stringArgB: "abc")
                    subField {
                        fieldC(stringArgC: $varA)
                    }
                }
            }
        '''

        def variables = [
                varA: "String Input"
        ]

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)
        executionInput = executionInput.transform { builder -> builder.context(mockGraphQLContext) }

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().size() == 1
        executionResult?.errors[0]?.message == expectedErrorMessage
        executionResult?.data?.topField?.subField == null
        executionResult?.data?.topField?.fieldB == "SomeStringB"
    }

}
