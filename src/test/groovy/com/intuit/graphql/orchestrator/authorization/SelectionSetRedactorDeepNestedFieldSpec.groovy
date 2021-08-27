package com.intuit.graphql.orchestrator.authorization

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.common.ArgumentValueResolver
import com.intuit.graphql.orchestrator.common.FieldPosition
import com.intuit.graphql.orchestrator.testutils.SelectionSetUtil
import graphql.GraphQLContext
import graphql.analysis.QueryTransformer
import graphql.language.AstTransformer
import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.language.SelectionSet
import graphql.parser.Parser
import graphql.schema.GraphQLFieldsContainer
import helpers.SchemaTestUtil
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import spock.lang.Specification

class SelectionSetRedactorDeepNestedFieldSpec extends Specification {

    def testGraphQLSchema = SchemaTestUtil.createGraphQLSchema("""
            type Query {
                a: A
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

    def "redact query deepest level field access is denied"() {
        given:
        Document document = new Parser().parseDocument("{ a { b1 { c1 { s1 } } b2 { i1 } } }")
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldType = (GraphQLFieldsContainer) testGraphQLSchema.getType("A")
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

        Map<String, Object> authData = ImmutableMap.of(
                (String)TEST_CLAIM_DATA.getLeft(), TEST_CLAIM_DATA.getRight(),
                "fieldArguments", Collections.emptyMap()
        )

        FieldAuthorizationRequest expectedS1AuthorizationRequest = FieldAuthorizationRequest.builder()
                .fieldPosition(new FieldPosition("C1", "s1"))
                .authData(authData)
                .clientId(TEST_CLIENT_ID)
                .graphQLContext(mockGraphQLContext)
                .build()

        SelectionSetRedactor specUnderTest = new SelectionSetRedactor(rootFieldType, rootFieldParentType, TEST_CLAIM_DATA,
                mockAuthorizationContext, mockGraphQLContext, argumentValueResolver, Collections.emptyMap())

        when:
//        QueryTransformer transformer = QueryTransformer.newQueryTransformer()
//                .schema(testGraphQLSchema)
//                .variables(Collections.emptyMap())
//                .fragmentsByName(Collections.emptyMap())
//                .rootParentType(testGraphQLSchema.getQueryType())
//                .root(rootField)
//                .build();

        Field transformedField =  (Field) astTransformer.transform(rootField, specUnderTest);

        then:
        transformedField.getName() == "a"
        CollectionUtils.size(specUnderTest.getProcessedSelectionSets()) == 4
        BooleanUtils.isFalse(specUnderTest.isResultAnEmptySelection())

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("Query", "a")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b1")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B1", "c1")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("C1", "s1")) >> true
        1 * mockFieldAuthorization.isAccessAllowed(expectedS1AuthorizationRequest) >> false

    }

}
