package com.intuit.graphql.orchestrator.authorization


import com.intuit.graphql.orchestrator.common.FieldPosition
import graphql.GraphQLContext
import graphql.analysis.QueryTransformer
import graphql.language.Document
import graphql.language.Field
import graphql.parser.Parser
import helpers.DocumentTestUtil
import helpers.SchemaTestUtil
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import spock.lang.Specification

class SelectionSetRedactorMidLevelFieldSpec extends Specification {

    static final String TEST_CLIENT_ID = "testClientId"
    static final Pair<String, Object> TEST_CLAIM_DATA = ImmutablePair.of("testClaimData", "ClaimDataValue")

    AuthorizationContext mockAuthorizationContext = Mock()
    GraphQLContext mockGraphQLContext = Mock()
    FieldAuthorization mockFieldAuthorization = Mock()

    def testAuthDataMap = [
            testClaimData : TEST_CLAIM_DATA.getRight(),
            fieldArguments : Collections.emptyMap()
    ]

    QueryRedactor specUnderTest = QueryRedactor.builder()
            .claimData(TEST_CLAIM_DATA)
            .authorizationContext(mockAuthorizationContext)
            .graphQLContext(mockGraphQLContext)
            .build()

    def testGraphQLSchema = SchemaTestUtil.createGraphQLSchema("""
            type Query {
                a: A
            }            
            type A {
                b1: B1
                b2: B2
            }
            type B1 {
                s1: String
            }
            type B2 {
                s2: String
            }
    """)


    def setup() {
        mockAuthorizationContext.getFieldAuthorization() >> mockFieldAuthorization
        mockAuthorizationContext.getClientId() >> TEST_CLIENT_ID
    }


    def "top-level field access is not allowed"() {
        given:
        Document document = new Parser().parseDocument("{ a { b1 { s1 } } }")
        Field rootField = DocumentTestUtil.getField(["a"], document)

        FieldAuthorizationRequest expectedFieldAuthorizationRequest = createFieldAuthorizationRequest(
                new FieldPosition("Query", "a"), testAuthDataMap
        )

        and:
        QueryTransformer queryTransformer = QueryTransformer.newQueryTransformer()
                .schema(testGraphQLSchema)
                .variables(Collections.emptyMap())
                .fragmentsByName(Collections.emptyMap())
                .rootParentType(testGraphQLSchema.getQueryType())
                .root(rootField)
                .build()

        when:
        Field transformedField =  (Field) queryTransformer.transform(specUnderTest)

        then:
        transformedField == null

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("Query", "a")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b1")) >> false
        0 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B", "s1")) >> false
        1 * mockFieldAuthorization.isAccessAllowed(expectedFieldAuthorizationRequest) >> false
    }


    def "top-level field access is allowed"() {
        given:
        Document document = new Parser().parseDocument("{ a { b1 { s1 } } }")
        Field rootField = DocumentTestUtil.getField(["a"], document)

        and:
        QueryTransformer queryTransformer = QueryTransformer.newQueryTransformer()
                .schema(testGraphQLSchema)
                .variables(Collections.emptyMap())
                .fragmentsByName(Collections.emptyMap())
                .rootParentType(testGraphQLSchema.getQueryType())
                .root(rootField)
                .build()

        when:
        Field transformedField =  (Field) queryTransformer.transform(specUnderTest)

        then:
        transformedField.getName() == "a"

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("Query", "a")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b1")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B1", "s1")) >> false
    }


    def "redact query mid-level field access is denied"() {
        given:
        Document document = new Parser().parseDocument("{ a { b1 { s1 } } }")
        Field rootField = DocumentTestUtil.getField(["a"], document)

        FieldAuthorizationRequest expectedFieldAuthorizationRequest = createFieldAuthorizationRequest(
                new FieldPosition("A", "b1"), testAuthDataMap
        )

        and:
        QueryTransformer queryTransformer = QueryTransformer.newQueryTransformer()
                .schema(testGraphQLSchema)
                .variables(Collections.emptyMap())
                .fragmentsByName(Collections.emptyMap())
                .rootParentType(testGraphQLSchema.getQueryType())
                .root(rootField)
                .build()

        when:
        Field transformedField =  (Field) queryTransformer.transform(specUnderTest)

        then:
        transformedField.getName() == "a"
        specUnderTest.getDeclinedFields().get(0).getPath() == "a/b1"

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("Query", "a")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b1")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B1", "s1")) >> false
        1 * mockFieldAuthorization.isAccessAllowed(expectedFieldAuthorizationRequest) >> false
    }


    def "redact query all mid-level field access is denied"() {
        given:
        Document document = new Parser().parseDocument("{ a { b1 { s1 } b2 { s2 } } }")
        Field rootField = DocumentTestUtil.getField(["a"], document)

        FieldAuthorizationRequest expectedB1AuthorizationRequest = createFieldAuthorizationRequest(
                new FieldPosition("A", "b1"), testAuthDataMap
        )

        FieldAuthorizationRequest expectedB2AuthorizationRequest = createFieldAuthorizationRequest(
                new FieldPosition("A", "b2"), testAuthDataMap
        )

        and:
        QueryTransformer queryTransformer = QueryTransformer.newQueryTransformer()
                .schema(testGraphQLSchema)
                .variables(Collections.emptyMap())
                .fragmentsByName(Collections.emptyMap())
                .rootParentType(testGraphQLSchema.getQueryType())
                .root(rootField)
                .build()

        when:
        Field transformedField =  (Field) queryTransformer.transform(specUnderTest)

        then:
        transformedField.getName() == "a"

        specUnderTest.getDeclinedFields().get(0).getPath() == "a/b1"
        specUnderTest.getDeclinedFields().get(1).getPath() == "a/b2"

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("Query", "a")) >> false

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b1")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B1", "s1")) >> false
        1 * mockFieldAuthorization.isAccessAllowed(expectedB1AuthorizationRequest) >> false

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b2")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B2", "s2")) >> false
        1 * mockFieldAuthorization.isAccessAllowed(expectedB2AuthorizationRequest) >> false
    }

    FieldAuthorizationRequest createFieldAuthorizationRequest(FieldPosition fieldPosition, Map<String, Object> authDataMap) {
        return FieldAuthorizationRequest.builder()
                .fieldPosition(fieldPosition)
                .authData(authDataMap)
                .clientId(TEST_CLIENT_ID)
                .graphQLContext(mockGraphQLContext)
                .build()
    }

}
