package com.intuit.graphql.orchestrator.authorization

import com.intuit.graphql.orchestrator.common.FieldPosition
import com.intuit.graphql.orchestrator.testutils.SelectionSetUtil
import graphql.GraphQLContext
import graphql.analysis.QueryTransformer
import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.parser.Parser
import helpers.SchemaTestUtil
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import spock.lang.Specification

class QueryRedactorDeepNestedFieldSpec extends Specification {

    static final String TEST_CLIENT_ID = "testClientId"
    static final Pair<String, Object> TEST_CLAIM_DATA = ImmutablePair.of("testClaimData", "ClaimDataValue")

    AuthorizationContext mockAuthorizationContext = Mock()
    GraphQLContext mockGraphQLContext = Mock()
    FieldAuthorization mockFieldAuthorization = Mock()

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
                s1(p1: String, p2: Boolean, p3: [String]): String
            }
            type B2 {
                i1: Int
            }
    """)

    def setup() {
        mockAuthorizationContext.getFieldAuthorization() >> mockFieldAuthorization
        mockAuthorizationContext.getClientId() >> TEST_CLIENT_ID
    }

    def "redact query deepest level field access is denied"() {
        given:
        Document document = new Parser().parseDocument("""
            query (\$var1: String, \$var2: [String]){ 
                a { 
                    b1 { c1 { s1(p1: \$var1, p2: true, p3: \$var2) } } 
                    b2 { i1 } 
                } 
            }
        """)

        def variables = [
                var1: "StringValue1",
                var2: ["svar1", "svar2", "svar3"]
        ]

        Map<String, Object> authData = [
            testClaimData: TEST_CLAIM_DATA.getRight(),
            fieldArguments : [
                p1: "StringValue1",
                p2: true,
                p3: ["svar1", "svar2", "svar3"]
            ]
        ]

        and:
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())

        FieldAuthorizationRequest expectedS1AuthorizationRequest = createFieldAuthorizationRequest(new FieldPosition("C1", "s1"), authData)

        QueryRedactor specUnderTest = QueryRedactor.builder()
                .claimData(TEST_CLAIM_DATA)
                .authorizationContext(mockAuthorizationContext)
                .graphQLContext(mockGraphQLContext)
                .build()

        and:
        QueryTransformer queryTransformer = QueryTransformer.newQueryTransformer()
                .schema(testGraphQLSchema)
                .variables(variables)
                .fragmentsByName(Collections.emptyMap())
                .rootParentType(testGraphQLSchema.getQueryType())
                .root(rootField)
                .build()

        when:
        Field transformedField =  (Field) queryTransformer.transform(specUnderTest)

        then:
        transformedField.getName() == "a"
        specUnderTest.getDeclinedFields().get(0).getPath() == "a/b1/c1/s1"

        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("Query", "a")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("A", "b1")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("B1", "c1")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(new FieldPosition("C1", "s1")) >> true
        1 * mockFieldAuthorization.isAccessAllowed(expectedS1AuthorizationRequest) >> false

    }

    FieldAuthorizationRequest createFieldAuthorizationRequest(FieldPosition fieldPosition,  Map<String, Object> authData) {
        FieldAuthorizationRequest.builder()
            .fieldPosition(fieldPosition)
            .authData(authData)
            .clientId(TEST_CLIENT_ID)
            .graphQLContext(mockGraphQLContext)
            .build()
    }
}
