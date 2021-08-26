package com.intuit.graphql.orchestrator.authorization

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.common.ArgumentValueResolver
import com.intuit.graphql.orchestrator.common.FieldPosition
import com.intuit.graphql.orchestrator.testutils.SelectionSetUtil
import graphql.GraphQLContext
import graphql.language.AstTransformer
import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.parser.Parser
import graphql.schema.GraphQLFieldsContainer
import helpers.SchemaTestUtil
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import spock.lang.Specification

class SelectionSetRedactorMidLevelFieldSpec extends Specification {

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

    static final String TEST_CLIENT_ID = "testClientId"
    static final Pair TEST_CLAIM_DATA = ImmutablePair.of("testClaimData", "ClaimDataValue")

    AuthorizationContext mockAuthorizationContext = Mock()
    GraphQLContext mockGraphQLContext = Mock()
    FieldAuthorization mockFieldAuthorization = Mock()
    ArgumentValueResolver argumentValueResolver = Mock()

    AstTransformer astTransformer = new AstTransformer()

    def setup() {
        mockAuthorizationContext.getFieldAuthorization() >> mockFieldAuthorization
        mockAuthorizationContext.getClientId() >> TEST_CLIENT_ID

        argumentValueResolver.resolve(_, _, _) >> Collections.emptyMap()
    }

    def "redact query mid-level field with no sibling access is denied"() {
        given:
        Document document = new Parser().parseDocument("{ a { b1 { s1 } } }")
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldType = (GraphQLFieldsContainer) testGraphQLSchema.getType("A")
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

        FieldAuthorizationRequest expectedFieldAuthorizationRequest = FieldAuthorizationRequest.builder()
                .fieldPosition(new FieldPosition("A", "b1"))
                .authData(ImmutableMap.of(
                        (String)TEST_CLAIM_DATA.getLeft(), TEST_CLAIM_DATA.getRight(),
                        "fieldArguments", Collections.emptyMap()
                ))
                .clientId(TEST_CLIENT_ID)
                .graphQLContext(mockGraphQLContext)
                .build()

        SelectionSetRedactor specUnderTest = new SelectionSetRedactor(rootFieldType, rootFieldParentType, TEST_CLAIM_DATA,
                mockAuthorizationContext, mockGraphQLContext, argumentValueResolver, Collections.emptyMap())

        when:
        Field transformedField = (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        transformedField.getName() == "a"
        CollectionUtils.size(specUnderTest.getProcessedSelectionSets()) == 1
        BooleanUtils.isTrue(specUnderTest.isResultAnEmptySelection())

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("Query", "a")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b1")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B1", "s1")) >> false
        1 * mockFieldAuthorization.isAccessAllowed(expectedFieldAuthorizationRequest) >> false
    }

    def "redact query mid-level field with sibling access is denied"() {
        given:
        Document document = new Parser().parseDocument("{ a { b1 { s1 } b2 { s2 } } }")
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldType = (GraphQLFieldsContainer) testGraphQLSchema.getType("A")
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")
        FieldAuthorizationRequest expectedB1AuthorizationRequest = FieldAuthorizationRequest.builder()
                .fieldPosition(new FieldPosition("A", "b1"))
                .authData(ImmutableMap.of(
                        (String)TEST_CLAIM_DATA.getLeft(), TEST_CLAIM_DATA.getRight(),
                        "fieldArguments", Collections.emptyMap()
                ))
                .clientId(TEST_CLIENT_ID)
                .graphQLContext(mockGraphQLContext)
                .build()

        SelectionSetRedactor specUnderTest = new SelectionSetRedactor(rootFieldType, rootFieldParentType, TEST_CLAIM_DATA,
                mockAuthorizationContext, mockGraphQLContext, argumentValueResolver, Collections.emptyMap())

        when:
        Field transformedField = (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        transformedField.getName() == "a"
        CollectionUtils.size(specUnderTest.getProcessedSelectionSets()) == 2
        BooleanUtils.isFalse(specUnderTest.isResultAnEmptySelection())

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("Query", "a")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b1")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B1", "s1")) >> false
        1 * mockFieldAuthorization.isAccessAllowed(expectedB1AuthorizationRequest) >> false

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b2")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B2", "s2")) >> false

    }

    def "redact query all mid-level field access is denied"() {
        given:
        Document document = new Parser().parseDocument("{ a { b1 { s1 } b2 { s2 } } }")
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldType = (GraphQLFieldsContainer) testGraphQLSchema.getType("A")
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

        Map<String, Object> authData = ImmutableMap.of(
                (String)TEST_CLAIM_DATA.getLeft(), TEST_CLAIM_DATA.getRight(),
                "fieldArguments", Collections.emptyMap()
        )

        FieldAuthorizationRequest expectedB1AuthorizationRequest = FieldAuthorizationRequest.builder()
                .fieldPosition(new FieldPosition("A", "b1"))
                .authData(authData)
                .clientId(TEST_CLIENT_ID)
                .graphQLContext(mockGraphQLContext)
                .build()

        FieldAuthorizationRequest expectedB2AuthorizationRequest = FieldAuthorizationRequest.builder()
                .fieldPosition(new FieldPosition("A", "b2"))
                .authData(authData)
                .clientId(TEST_CLIENT_ID)
                .graphQLContext(mockGraphQLContext)
                .build()

        SelectionSetRedactor specUnderTest = new SelectionSetRedactor(rootFieldType, rootFieldParentType, TEST_CLAIM_DATA,
                mockAuthorizationContext, mockGraphQLContext, argumentValueResolver, Collections.emptyMap())

        when:
        Field transformedField = (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        transformedField.getName() == "a"
        CollectionUtils.size(specUnderTest.getProcessedSelectionSets()) == 1
        BooleanUtils.isTrue(specUnderTest.isResultAnEmptySelection())

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("Query", "a")) >> false

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b1")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B1", "s1")) >> false
        1 * mockFieldAuthorization.isAccessAllowed(expectedB1AuthorizationRequest) >> false

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b2")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B2", "s2")) >> false
        1 * mockFieldAuthorization.isAccessAllowed(expectedB2AuthorizationRequest) >> false
    }

}
