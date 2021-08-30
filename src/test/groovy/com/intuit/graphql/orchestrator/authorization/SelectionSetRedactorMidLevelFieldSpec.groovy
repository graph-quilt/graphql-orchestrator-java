package com.intuit.graphql.orchestrator.authorization

import graphql.schema.FieldCoordinates
import graphql.analysis.QueryTransformer
import graphql.language.Document
import graphql.language.Field
import graphql.parser.Parser
import helpers.DocumentTestUtil
import helpers.SchemaTestUtil
import spock.lang.Specification

class SelectionSetRedactorMidLevelFieldSpec extends Specification {

    static final String TEST_AUTH_DATA = "Can Be Any Object AuthData"

    FieldAuthorization mockFieldAuthorization = Mock()

    QueryRedactor specUnderTest = QueryRedactor.builder()
            .authData(TEST_AUTH_DATA)
            .fieldAuthorization(mockFieldAuthorization)
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


    def "top-level field access is not allowed"() {
        given:
        Document document = new Parser().parseDocument("{ a { b1 { s1 } } }")
        Field rootField = DocumentTestUtil.getField(["a"], document)

        FieldAuthorizationEnvironment fieldAuthorizationEnvironment = createFieldAuthorizationRequest(
                FieldCoordinates.coordinates("Query", "a"), TEST_AUTH_DATA
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

        1 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("Query", "a")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("A", "b1")) >> false
        0 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("B", "s1")) >> false
        1 * mockFieldAuthorization.isAccessAllowed(fieldAuthorizationEnvironment) >> false
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

        1 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("Query", "a")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("A", "b1")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("B1", "s1")) >> false
    }


    def "redact query mid-level field access is denied"() {
        given:
        Document document = new Parser().parseDocument("{ a { b1 { s1 } } }")
        Field rootField = DocumentTestUtil.getField(["a"], document)

        FieldAuthorizationEnvironment expectedFieldAuthorizationRequest = createFieldAuthorizationRequest(
                FieldCoordinates.coordinates("A", "b1"), TEST_AUTH_DATA
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

        1 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("Query", "a")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("A", "b1")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("B1", "s1")) >> false
        1 * mockFieldAuthorization.isAccessAllowed(expectedFieldAuthorizationRequest) >> false
    }


    def "redact query all mid-level field access is denied"() {
        given:
        Document document = new Parser().parseDocument("{ a { b1 { s1 } b2 { s2 } } }")
        Field rootField = DocumentTestUtil.getField(["a"], document)

        FieldAuthorizationEnvironment expectedB1AuthorizationRequest = createFieldAuthorizationRequest(
                FieldCoordinates.coordinates("A", "b1"), TEST_AUTH_DATA
        )

        FieldAuthorizationEnvironment expectedB2AuthorizationRequest = createFieldAuthorizationRequest(
                FieldCoordinates.coordinates("A", "b2"), TEST_AUTH_DATA
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

        1 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("Query", "a")) >> false

        1 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("A", "b1")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("B1", "s1")) >> false
        1 * mockFieldAuthorization.isAccessAllowed(expectedB1AuthorizationRequest) >> false

        0 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("A", "b2")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("B2", "s2")) >> false
        0 * mockFieldAuthorization.isAccessAllowed(expectedB2AuthorizationRequest) >> false
    }

    FieldAuthorizationEnvironment createFieldAuthorizationRequest(FieldCoordinates fieldCoordinates, Object authData) {
        return FieldAuthorizationEnvironment.builder()
                .fieldCoordinates(fieldCoordinates)
                .fieldArguments(Collections.emptyMap())
                .authData(authData)
                .build()
    }

}
