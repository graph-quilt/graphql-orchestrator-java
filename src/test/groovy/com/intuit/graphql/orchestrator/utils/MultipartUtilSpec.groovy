package com.intuit.graphql.orchestrator.utils

import com.intuit.graphql.orchestrator.deferDirective.DeferOptions
import com.intuit.graphql.orchestrator.visitors.queryVisitors.QueryCreatorResult
import com.intuit.graphql.orchestrator.visitors.queryVisitors.AggregateQueryModifier
import graphql.ExecutionInput
import graphql.language.AstTransformer
import graphql.language.Document
import graphql.parser.Parser
import lombok.extern.slf4j.Slf4j
import spock.lang.Specification 
/**
 * Covers test for ObjectTypeExtension, InterfaceTypeExtension, UnionTypeExtension, EnumTypeExtension,
 * InputObjectTypeExtension TODO ScalarTypeExtension.
 */
@Slf4j
class MultipartUtilSpec extends Specification {
    private static final AstTransformer AST_TRANSFORMER = new AstTransformer()
    private static final DeferOptions deferOptions = DeferOptions.builder()
            .nestedDefersAllowed(true)
            .build()

    def "can split Execution input"() {
        given:
        String query = "query { queryA { fieldA fieldB fieldC @defer } }"

        when:
        AggregateQueryModifier visitor = new AggregateQueryModifier(ExecutionInput.newExecutionInput(query).build(), deferOptions)
        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.generateResults()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
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
        AggregateQueryModifier visitor = new AggregateQueryModifier(ExecutionInput.newExecutionInput(query).build(), deferOptions)
        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.generateResults()

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
        AggregateQueryModifier visitor = new AggregateQueryModifier(ExecutionInput.newExecutionInput(query).build(), deferOptions)
        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.generateResults()

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
        AggregateQueryModifier visitor = new AggregateQueryModifier(ExecutionInput.newExecutionInput(query).build(), deferOptions)
        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.generateResults()

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
        AggregateQueryModifier visitor = new AggregateQueryModifier(ExecutionInput.newExecutionInput(query).build(), deferOptions)
        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.generateResults()

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
        AggregateQueryModifier visitor = new AggregateQueryModifier(ExecutionInput.newExecutionInput(query).build(), deferOptions)
        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.generateResults()

        then:
        List<ExecutionInput> splitSet = creatorResult.getForkedDeferEIs()
        splitSet.size() == 0
    }

    def "prunes selections sets without fields after removing deferred fields"() {
        given:
        String query = "query { queryA { fieldA objectField { fieldB nestedObject @defer { fieldC @defer} } } }"

        when:
        AggregateQueryModifier visitor = new AggregateQueryModifier(ExecutionInput.newExecutionInput(query).build(), deferOptions)
        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.generateResults()

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
        AggregateQueryModifier visitor = new AggregateQueryModifier(ExecutionInput.newExecutionInput(query).build(), deferOptions)
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
        AggregateQueryModifier visitor = new AggregateQueryModifier(ExecutionInput.newExecutionInput(query).build(), deferOptions)
        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.generateResults()

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
        AggregateQueryModifier visitor = new AggregateQueryModifier(ExecutionInput.newExecutionInput(query).build(), deferOptions)
        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.generateResults()

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
        AggregateQueryModifier visitor = new AggregateQueryModifier(ExecutionInput.newExecutionInput(query).build(), deferOptions)
        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.generateResults()

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
        AggregateQueryModifier visitor = new AggregateQueryModifier(ExecutionInput.newExecutionInput(query).build(), deferOptions)
        Document document = new Parser().parseDocument(query)
        AST_TRANSFORMER.transform(document, visitor)
        QueryCreatorResult creatorResult = visitor.generateResults()

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

    // end to end tests
}
