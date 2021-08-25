package com.intuit.graphql.orchestrator.authorization

import com.google.common.collect.ImmutableMap
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

class SelectionSetRedactorSpec extends Specification {

    def testGraphQLSchema = SchemaTestUtil.createGraphQLSchema("""
            type Query {
                a1: A1
                a2: A2
                a3: A3
            }
            
            # a1 tree
            type A1 {
                b1: B1
                b2: B2
            }
            type B1 {
                s1: String
            }
            type B2 {
                c2: C2
            }
            type C2 {
                s2: String
            }
            
            # a2 tree
            type A2 {
                b3: B3
            }
            type B3 {
                c3: C3
            }
            type C3 {
                s3: String
            }
            
            # a3 tree
            type A3 {
                b4: B4
            }
            type B4 {
                s4: String
            }
    """)

    static final String TEST_CLIENT_ID = "testClientId"
    static final Pair TEST_CLAIM_DATA = ImmutablePair.of("testClaimData", "ClaimDataValue")

    AuthorizationContext mockAuthorizationContext = Mock()
    GraphQLContext mockGraphQLContext = Mock()
    FieldAuthorization mockFieldAuthorization = Mock()

    def setup() {
        mockAuthorizationContext.getFieldAuthorization() >> mockFieldAuthorization
        mockAuthorizationContext.getClientId() >> TEST_CLIENT_ID
    }

    def "top-level field access is not allowed"() {
        given:
        Document document = new Parser().parseDocument("{ a3 { b4 { s4 } } }")
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field a3Field = SelectionSetUtil.getFieldByPath(Arrays.asList("a3"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldType = (GraphQLFieldsContainer) testGraphQLSchema.getType("A3")
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

        and:
        FieldAuthorizationRequest fieldAuthorizationRequest = FieldAuthorizationRequest.builder()
                .fieldPosition(new FieldPosition("Query", "a3"))
                .authData(ImmutableMap.of(
                        (String)TEST_CLAIM_DATA.getLeft(), TEST_CLAIM_DATA.getRight(),
                        "fieldArguments", a3Field.getArguments()
                ))
                .clientId(TEST_CLIENT_ID)
                .graphQLContext(mockGraphQLContext)
                .build()
        mockFieldAuthorization.isAccessAllowed(fieldAuthorizationRequest) >> false
        SelectionSetRedactor specUnderTest = new SelectionSetRedactor(rootFieldType, rootFieldParentType, TEST_CLAIM_DATA,
                mockAuthorizationContext, mockGraphQLContext)

        when:
        AstTransformer astTransformer = new AstTransformer()
        Field transformedField = (Field) astTransformer.transform(a3Field, specUnderTest)

        then:
        transformedField == null
        CollectionUtils.size(specUnderTest.getProcessedSelectionSets()) == 0
        BooleanUtils.isTrue(specUnderTest.isResultAnEmptySelection())
    }

    def "top-level field access is allowed"() {
        given:
        Document document = new Parser().parseDocument("{ a3 { b4 { s4 } } }")
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field a3Field = SelectionSetUtil.getFieldByPath(Arrays.asList("a3"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldType = (GraphQLFieldsContainer) testGraphQLSchema.getType("A3")
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

        and:
        mockFieldAuthorization.isAccessAllowed(_ as FieldAuthorizationRequest) >> true
        SelectionSetRedactor specUnderTest = new SelectionSetRedactor(rootFieldType, rootFieldParentType, TEST_CLAIM_DATA,
                mockAuthorizationContext, mockGraphQLContext)

        when:
        AstTransformer astTransformer = new AstTransformer()
        Field transformedField = (Field) astTransformer.transform(a3Field, specUnderTest)

        then:
        transformedField.getName() == "a3"
        CollectionUtils.size(specUnderTest.getProcessedSelectionSets()) == 2
        BooleanUtils.isFalse(specUnderTest.isResultAnEmptySelection())
    }

}
