package com.intuit.graphql.orchestrator.authorization

import com.intuit.graphql.orchestrator.testutils.SelectionSetUtil
import graphql.analysis.QueryTransformer
import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.parser.Parser
import helpers.SchemaTestUtil
import org.apache.commons.lang3.BooleanUtils
import spock.lang.Specification
import graphql.schema.FieldCoordinates

class QueryRedactorDeepNestedFieldSpec extends Specification {

    static final String TEST_AUTH_DATA = "Can Be Any Object AuthData"

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

        and:
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())

        def fieldArguments = [
                p1: "StringValue1",
                p2: true,
                p3: ["svar1", "svar2", "svar3"]
        ]
        FieldAuthorizationEnvironment s1FieldAuthorizationEnvironment = createFieldAuthorizationRequest(FieldCoordinates.coordinates("C1", "s1"), fieldArguments, TEST_AUTH_DATA)

        QueryRedactor specUnderTest = QueryRedactor.builder()
                .authData(TEST_AUTH_DATA)
                .fieldAuthorization(mockFieldAuthorization)
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
        BooleanUtils.isTrue(specUnderTest.isFieldAccessDeclined())

        1 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("Query", "a")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("A", "b1")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("B1", "c1")) >> false
        1 * mockFieldAuthorization.requiresAccessControl(FieldCoordinates.coordinates("C1", "s1")) >> true
        1 * mockFieldAuthorization.isAccessAllowed(s1FieldAuthorizationEnvironment) >> false

    }

    FieldAuthorizationEnvironment createFieldAuthorizationRequest(FieldCoordinates fieldCoordinates, Map<String, Object> fieldArguments, Object authData) {
        FieldAuthorizationEnvironment.builder()
            .fieldCoordinates(fieldCoordinates)
            .fieldArguments(fieldArguments)
            .authData(authData)
            .build()
    }
}
