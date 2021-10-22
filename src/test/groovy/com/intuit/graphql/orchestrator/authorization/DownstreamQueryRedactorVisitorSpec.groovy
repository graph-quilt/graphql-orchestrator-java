package com.intuit.graphql.orchestrator.authorization


import com.intuit.graphql.orchestrator.SelectionSetUtil
import com.intuit.graphql.orchestrator.common.ArgumentValueResolver
import graphql.GraphQLContext
import graphql.GraphqlErrorException
import graphql.language.AstTransformer
import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.parser.Parser
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldsContainer
import helpers.SchemaTestUtil
import org.apache.commons.collections4.CollectionUtils
import spock.lang.Specification

class DownstreamQueryRedactorVisitorSpec extends Specification {

    def testGraphQLSchema = SchemaTestUtil.createGraphQLSchema("""
        type Query {
            a: A
            a2: String
        }            
        type A {
            b1: B1
            b2: B2
        }
        type B1 {
            c1: C1
        }
        type C1 {
            s1: String
        }
        type B2 {
            i1: Int
        }
    """)

    def testQuery = """
        { 
            a { 
                b1 {                                     
                    ... on B1 {
                        c1 { 
                            s1 
                        }
                    }   
                } 
                b2 { i1 } 
            } 
            a2
        }
    """

    static final Object TEST_AUTH_DATA = "TestAuthDataCanBeAnyObject"

    Field mockField = Mock()
    FieldAuthorizationEnvironment queryA = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("Query","a"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a"])
            .build()

    FieldAuthorizationEnvironment queryA2 = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("Query","a2"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a2"])
            .build()

    FieldAuthorizationEnvironment aB1 = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("A","b1"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a", "b1"])
            .build()

    FieldAuthorizationEnvironment b1C1 = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("B1","c1"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a", "b1", "c1"])
            .build()

    FieldAuthorizationEnvironment c1S1 = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("C1","s1"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a", "b1", "c1", "s1"])
            .build()

    FieldAuthorizationEnvironment aB2 = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("A","b2"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a", "b2"])
            .build()

    FieldAuthorizationEnvironment b2i1 = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("B2","i1"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a","b2", "i1"])
            .build()

    GraphQLContext mockGraphQLContext = Mock()
    FieldAuthorization mockFieldAuthorization = Mock()
    ArgumentValueResolver argumentValueResolver = Mock()

    AstTransformer astTransformer = new AstTransformer()

    def setup() {
        argumentValueResolver.resolve(_, _, _) >> Collections.emptyMap()
    }

    def "redact query, results to empty selection set"() {
        given:

        Document document = new Parser().parseDocument(testQuery)
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

        GraphqlErrorException testGraphqlErrorException = GraphqlErrorException.newErrorException()
            .message("testError")
            .build()

        DownstreamQueryRedactorVisitor specUnderTest = DownstreamQueryRedactorVisitor.builder()
                .rootFieldParentType((GraphQLFieldsContainer) rootFieldParentType)
                .fieldAuthorization(mockFieldAuthorization)
                .authData(TEST_AUTH_DATA)
                .graphQLContext(mockGraphQLContext)
                .queryVariables(Collections.emptyMap())
                .graphQLSchema(testGraphQLSchema)
                .build()

        when:
        Field transformedField =  (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        transformedField.getName() == "a"
        CollectionUtils.size(specUnderTest.getEmptySelectionSets()) == 2

        1 * mockFieldAuthorization.authorize(queryA) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        1 * mockFieldAuthorization.authorize(aB1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        1 * mockFieldAuthorization.authorize(b1C1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        1 * mockFieldAuthorization.authorize(c1S1) >> FieldAuthorizationResult
                .createDeniedResult(testGraphqlErrorException)
        1 * mockFieldAuthorization.authorize(aB2) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        1 * mockFieldAuthorization.authorize(b2i1) >> FieldAuthorizationResult
                .createDeniedResult(testGraphqlErrorException)
    }

    def "root node access is denied"() {
        given:

        Document document = new Parser().parseDocument(testQuery)
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a2"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

        GraphqlErrorException testGraphqlErrorException = GraphqlErrorException.newErrorException()
                .message("testError")
                .build()

        DownstreamQueryRedactorVisitor specUnderTest = DownstreamQueryRedactorVisitor.builder()
                .rootFieldParentType((GraphQLFieldsContainer) rootFieldParentType)
                .fieldAuthorization(mockFieldAuthorization)
                .authData(TEST_AUTH_DATA)
                .graphQLContext(mockGraphQLContext)
                .queryVariables(Collections.emptyMap())
                .graphQLSchema(testGraphQLSchema)
                .build()

        when:
        Field transformedField =  (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        transformedField == null
        CollectionUtils.size(specUnderTest.getEmptySelectionSets()) == 0

        1 * mockFieldAuthorization.authorize(queryA2) >>  FieldAuthorizationResult
                .createDeniedResult(testGraphqlErrorException)
    }

}
