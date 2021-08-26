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

class SelectionSetRedactorTopLevelFieldSpec extends Specification {

    def testGraphQLSchema = SchemaTestUtil.createGraphQLSchema("""
            type Query {
                a: A
            }            
            type A {
                b: B
            }
            type B {
                s: String
            }
    """)

    static final String TEST_CLIENT_ID = "testClientId"
    static final Pair TEST_CLAIM_DATA = ImmutablePair.of("testClaimData", "ClaimDataValue")

    AuthorizationContext mockAuthorizationContext = Mock()
    GraphQLContext mockGraphQLContext = Mock()
    FieldAuthorization mockFieldAuthorization = Mock()
    ArgumentValueResolver argumentValueResolver = Mock()

    Document document = new Parser().parseDocument("{ a { b { s } } }")
    OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
    Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
    GraphQLFieldsContainer rootFieldType = (GraphQLFieldsContainer) testGraphQLSchema.getType("A")
    GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

    AstTransformer astTransformer = new AstTransformer()

    def setup() {
        mockAuthorizationContext.getFieldAuthorization() >> mockFieldAuthorization
        mockAuthorizationContext.getClientId() >> TEST_CLIENT_ID

        argumentValueResolver.resolve(_, _, _) >> Collections.emptyMap()
    }

    def "top-level field access is not allowed"() {
        given:
        FieldAuthorizationRequest expectedFieldAuthorizationRequest = FieldAuthorizationRequest.builder()
                .fieldPosition(new FieldPosition("Query", "a"))
                .authData(ImmutableMap.of(
                        (String)TEST_CLAIM_DATA.getLeft(), TEST_CLAIM_DATA.getRight(),
                        "fieldArguments",Collections.emptyMap()
                ))
                .clientId(TEST_CLIENT_ID)
                .graphQLContext(mockGraphQLContext)
                .build()

        SelectionSetRedactor specUnderTest = new SelectionSetRedactor(rootFieldType, rootFieldParentType, TEST_CLAIM_DATA,
                mockAuthorizationContext, mockGraphQLContext, argumentValueResolver, Collections.emptyMap())

        when:
        Field transformedField = (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        transformedField == null
        CollectionUtils.size(specUnderTest.getProcessedSelectionSets()) == 0
        BooleanUtils.isTrue(specUnderTest.isResultAnEmptySelection())

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("Query", "a")) >> true
        0 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b")) >> false
        0 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B", "s")) >> false
        1 * mockFieldAuthorization.isAccessAllowed(expectedFieldAuthorizationRequest) >> false
    }

    def "top-level field access is allowed"() {
        given:
        SelectionSetRedactor specUnderTest = new SelectionSetRedactor(rootFieldType, rootFieldParentType, TEST_CLAIM_DATA,
                mockAuthorizationContext, mockGraphQLContext, argumentValueResolver, Collections.emptyMap())

        when:
        Field transformedField = (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        transformedField.getName() == "a"
        CollectionUtils.size(specUnderTest.getProcessedSelectionSets()) == 2
        BooleanUtils.isFalse(specUnderTest.isResultAnEmptySelection())

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("Query", "a")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B", "s")) >> false
    }

}
