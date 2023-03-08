package com.intuit.graphql.orchestrator.visitors

import com.intuit.graphql.orchestrator.deferDirective.DeferOptions
import com.intuit.graphql.orchestrator.visitors.queryVisitors.DeferDirectiveQueryModifier
import com.intuit.graphql.orchestrator.visitors.queryVisitors.DeferQueryCreatorVisitor
import com.intuit.graphql.orchestrator.visitors.queryVisitors.EIAggregateVisitor
import com.intuit.graphql.orchestrator.visitors.queryVisitors.QueryCreatorResult
import graphql.ExecutionInput
import graphql.GraphQLException
import graphql.analysis.QueryTransformer
import graphql.language.Document
import graphql.language.OperationDefinition
import graphql.parser.Parser
import helpers.BaseIntegrationTestSpecification
import lombok.extern.slf4j.Slf4j

import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.parser

/**
 * Covers test for ObjectTypeExtension, InterfaceTypeExtension, UnionTypeExtension, EnumTypeExtension,
 * InputObjectTypeExtension TODO ScalarTypeExtension.
 */
@Slf4j
class DeferDirectiveQueryModifierSpec extends BaseIntegrationTestSpecification {
    private static final DeferOptions deferOptions = DeferOptions.builder()
            .nestedDefersAllowed(true)
            .build()
    private static final DeferOptions disabledDeferOptions = DeferOptions.builder()
            .nestedDefersAllowed(false)
            .build()

    def "can split Execution input"() {
        given:
        String query = "query { queryA { fieldA fieldB fieldC @defer } }"

        when:
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()

        DeferQueryCreatorVisitor visitor = DeferQueryCreatorVisitor.builder()
                .originalEI(ei)
                .operationDefinition(opDef)
                .deferOptions(deferOptions)
                .build()

        Document document = new Parser().parseDocument(query)

        QueryTransformer.newQueryTransformer().root()

        AST_TRANSFORMER.transform(document, visitor)

        then:
        List<ExecutionInput> splitSet = visitor.getGeneratedEIs()
        splitSet.size() == 1
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    fieldC\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "can split EI with alias selections"(){
        given:
        String query = "query { queryA { aliasA: fieldA  aliasB: fieldB @defer } }"

        when:
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        DeferDirectiveQueryModifier visitor = DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(deferOptions)
                .build()

        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.addResultsToBuilder(QueryCreatorResult.builder()).build()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    aliasB: fieldB\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "can split execution input with arguments"() {
        given:
        String query = "query { getFoo(id: \"inputA\") { fieldA fieldB @defer } }"

        when:
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        DeferDirectiveQueryModifier visitor = DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(deferOptions)
                .build()

        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.addResultsToBuilder(QueryCreatorResult.builder()).build()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
        splitSet.size() == 1
        splitSet.get(0).query == "query {\n" +
                "  getFoo(id: \"inputA\") {\n" +
                "    fieldB\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "can split EI with multiple defer on same level"() {
        given:
        String query = "query { queryA { fieldA fieldB @defer fieldC @defer } }"

        when:
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        DeferDirectiveQueryModifier visitor = DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(deferOptions)
                .build()

        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.addResultsToBuilder(QueryCreatorResult.builder()).build()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
        splitSet.size() == 2
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    fieldB\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    fieldC\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"


    }

    def "can split EI with deferred nested selections"() {
        given:
        String query = "query { queryA { fieldA objectField { fieldB fieldC @defer } } }"

        when:
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        DeferDirectiveQueryModifier visitor = DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(deferOptions)
                .build()

        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.addResultsToBuilder(QueryCreatorResult.builder()).build()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    objectField {\n" +
                "      fieldC\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "Does not split EI when if arg is false"() {
        given:
        String query = "query { queryA { fieldA fieldB fieldC @defer(if: false) } }"

        when:
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        DeferDirectiveQueryModifier visitor = DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(deferOptions)
                .build()

        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.addResultsToBuilder(QueryCreatorResult.builder()).build()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
        splitSet.size() == 0
    }

    def "prunes selections sets without fields after removing deferred fields"() {
        given:
        String query = "query { queryA { fieldA objectField { fieldB nestedObject @defer { fieldC @defer} } } }"

        when:
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        DeferDirectiveQueryModifier visitor = DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(deferOptions)
                .build()

        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.addResultsToBuilder(QueryCreatorResult.builder()).build()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
        splitSet.size() == 1
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    objectField {\n" +
                "      nestedObject {\n" +
                "        fieldC\n" +
                "        __typename\n" +
                "      }\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "can split nested defer selections"() {
        given:
        String query = "query { queryA { fieldA objectField @defer { fieldB fieldC @defer } } }"

        when:
        EIAggregateVisitor visitor = new EIAggregateVisitor(ExecutionInput.newExecutionInput(query).build(), deferOptions)
        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.generateResults()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
        splitSet.size() == 2
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    objectField {\n" +
                "      fieldB\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"

        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    objectField {\n" +
                "      fieldC\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "exception thrown for nested defer selections when option is off"() {
        given:
        String query = "query { queryA { fieldA objectField @defer { fieldB fieldC @defer } } }"

        when:
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        DeferDirectiveQueryModifier visitor = DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(disabledDeferOptions)
                .build()

        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)

        then:
        def exception = thrown(GraphQLException)
        exception.getMessage() ==~ "Nested defers are currently unavailable."
    }

    //todo
    def "can split EI with variables" () {}

    def "can split EI with inline fragment"() {
        given:
        String query = """
                        query {
                            queryA {
                                fieldA
                                objectField {
                                    fieldB
                                }
                                ... on ObjectType @defer {
                                    fieldC
                                }
                            }
                        }
        """

        when:
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        DeferDirectiveQueryModifier visitor = DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(deferOptions)
                .build()

        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.addResultsToBuilder(QueryCreatorResult.builder()).build()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
        splitSet.size() == 1
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    ... on ObjectType {\n" +
                "      fieldC\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "split EI has correct inline fragments different types"() {
        given:
        String query = """
                        query {
                            queryA {
                                fieldA
                                objectField1 {
                                    fieldB
                                }
                                ... on ObjectType1 @defer {
                                    fieldC
                                }
                                objectField2 {
                                    fieldD
                                }
                                ... on ObjectType2 @defer {
                                    fieldE
                                }
                            }
                        }
        """

        when:
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        DeferDirectiveQueryModifier visitor = DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(deferOptions)
                .build()

        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.addResultsToBuilder(QueryCreatorResult.builder()).build()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
        splitSet.size() == 2
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    ... on ObjectType1 {\n" +
                "      fieldC\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    ... on ObjectType2 {\n" +
                "      fieldE\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "split EI has correct inline fragments non merged types"() {
        given:
        String query = """
                        query {
                            queryA {
                                fieldA
                                objectField1 {
                                    fieldB
                                }
                                ... on ObjectType @defer {
                                    fieldC
                                }
                                objectField2 {
                                    fieldD
                                }
                                ... on ObjectType @defer {
                                    fieldE
                                }
                            }
                        }
        """

        when:
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        DeferDirectiveQueryModifier visitor = DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(deferOptions)
                .build()

        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.addResultsToBuilder(QueryCreatorResult.builder()).build()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
        splitSet.size() == 2
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    ... on ObjectType {\n" +
                "      fieldC\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    ... on ObjectType {\n" +
                "      fieldE\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "can split EI with fragment spread"() {
        given:
        String query = """
                         query {
                            queryA {
                                fieldA
                                objectField {
                                    fieldB
                                    ... deferredInfo @defer
                                }
                                
                            }
                        }
                        fragment deferredInfo on ObjectType {
                            fieldC
                        }
        """

        when:
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        DeferDirectiveQueryModifier visitor = DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(deferOptions)
                .build()

        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.addResultsToBuilder(QueryCreatorResult.builder()).build()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
        splitSet.size() == 1
        splitSet.get(0).query == "query {\n" +
                         "  queryA {\n" +
                         "    objectField {\n" +
                         "      ...deferredInfo\n" +
                         "      __typename\n" +
                         "    }\n" +
                         "    __typename\n" +
                         "  }\n" +
                         "}\n" +
                         "\nfragment deferredInfo on ObjectType {\n" +
                         "  fieldC\n" +
                         "}\n"
    }

    def "thrown exception when building with null defer options"(){
        given:
        ExecutionInput ei = ExecutionInput.newExecutionInput()
                .query("query { test }")
                .build()

        when:
        DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(null)
                .build()

        then:
        thrown(NullPointerException)
    }

    def "thrown exception when building with null ei"(){
        given:
        ExecutionInput ei = ExecutionInput.newExecutionInput()
                .query("query { test }")
                .build()

        when:
        DeferDirectiveQueryModifier.builder()
                .originalEI(null)
                .deferOptions(deferOptions)
                .build()

        then:
        thrown(NullPointerException)
    }

    def "sets rootNode and fragments when setting ei for builder"() {
        given:
        ExecutionInput ei = ExecutionInput.newExecutionInput()
                .query("query { ... TestFrag } fragment TestFrag on type { testField }")
                .build()

        when:
        DeferDirectiveQueryModifier modifier = DeferDirectiveQueryModifier.builder()
            .originalEI(ei)
            .deferOptions(deferOptions)
            .build()

        then:
        modifier.originalEI != null
        modifier.originalEI == ei

        modifier.fragmentDefinitionMap != null
        modifier.fragmentDefinitionMap.size() == 1
        modifier.fragmentDefinitionMap.containsKey("TestFrag")
    }

    def "sets child modifier when setting deferOptions for builder"() {
        given:
        ExecutionInput ei = ExecutionInput.newExecutionInput()
                .query("query { test }")
                .build()

        when:
        DeferDirectiveQueryModifier modifier = DeferDirectiveQueryModifier.builder()
                .originalEI(ei)
                .deferOptions(deferOptions)
                .build()

        then:
        modifier.deferOptions != null
        modifier.childModifier != null
    }
}
