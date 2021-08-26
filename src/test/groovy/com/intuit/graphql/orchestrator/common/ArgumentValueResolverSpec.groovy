package com.intuit.graphql.orchestrator.common

import com.intuit.graphql.orchestrator.testutils.SelectionSetUtil
import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.parser.Parser
import graphql.schema.GraphQLFieldsContainer
import helpers.SchemaTestUtil
import spock.lang.Specification

class ArgumentValueResolverSpec extends Specification {

    def testGraphQLSchema = SchemaTestUtil.createGraphQLSchema("""
            type Query {
                a(objectInput: ObjectInput, string: String) : A
            }            
            type A {
                leaf1: String
                leaf2: Int
            }
            input ObjectInput {
                param1: [String]
                param2: Int
                param3: Boolean
            }            
    """)

    ArgumentValueResolver specUnderTest = new ArgumentValueResolver(testGraphQLSchema);

    def testInputVariables = [
            objVar: [
                    param1 : Arrays.asList("stringVal1","stringVal2","stringVal3"),
                    param2 : new Integer(100),
                    param3 : Boolean.TRUE
            ]
    ]

    def "resolves"() {
        given:
        Document document = new Parser().parseDocument("""
            query CallA(\$objVar : ObjectInput) { 
                a(objectInput : \$objVar, string : "StringArgumentValue") { 
                    leaf1 
                } 
            }
        """)
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

        when:
        def actualMap = specUnderTest.resolve(rootField, rootFieldParentType.getFieldDefinition(rootField.getName()), testInputVariables);

        then:
        actualMap != null
        actualMap.size() == 2
        Map<String, Object> actualObjectInput = (Map<String, Object>) actualMap.get("objectInput")
        List<Object> actualParam1 = (List<Object>) actualObjectInput.getAt("param1")
        actualParam1.get(0) == "stringVal1"
        actualParam1.get(1) == "stringVal2"
        actualParam1.get(2) == "stringVal3"
        actualMap.get("objectInput").getAt("param2") == 100
        actualMap.get("objectInput").getAt("param3") == true
        actualMap.get("string") == "StringArgumentValue"
    }

}
