package com.intuit.graphql.orchestrator.visitors

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.deferDirective.DeferOptions
import com.intuit.graphql.orchestrator.visitors.queryVisitors.DeferQueryExtractor
import graphql.ExecutionInput
import graphql.GraphQLException
import graphql.analysis.QueryTransformer
import graphql.language.Document
import graphql.language.Field
import graphql.language.FragmentDefinition
import graphql.language.OperationDefinition
import helpers.BaseIntegrationTestSpecification
import lombok.extern.slf4j.Slf4j

import java.util.function.Function
import java.util.stream.Collectors

import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.parser
/**
 * Covers test for ObjectTypeExtension, InterfaceTypeExtension, UnionTypeExtension, EnumTypeExtension,
 * InputObjectTypeExtension TODO ScalarTypeExtension.
 */
@Slf4j
class DeferQueryExtractorSpec extends BaseIntegrationTestSpecification {
    private static final DeferOptions deferOptions = DeferOptions.builder()
            .nestedDefersAllowed(true)
            .build()
    private static final DeferOptions disabledDeferOptions = DeferOptions.builder()
            .nestedDefersAllowed(false)
            .build()

    private deferTestSchema = """
        type Query {
            queryA: NestedObjectA
            argQuery(id: String): NestedObjectA
        }
        
        type NestedObjectA {
            fieldA: String
            fieldB: String
            fieldC: String
            objectField: TopLevelObject
        }
        
        type TopLevelObject {
            fieldD: String
            fieldE: String
            fieldF: String
            nestedObject: NestedObject
        }
        
        type NestedObject {
            fieldG: String
            fieldH: String
            fieldI: String
        }
    """

    private deferService = createSimpleMockService("DEFER", deferTestSchema, new HashMap<String, Object>())
    private GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(deferService)

    def "can split Execution input"() {
        given:
        String query = "query { queryA { fieldA fieldB fieldC @defer } }"
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(new HashMap<String, FragmentDefinition>())
            .variables(ei.getVariables())
            .build()
            .transform(visitor)


        then:
        List<ExecutionInput> splitSet = visitor.getExtractedEIs()
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
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(new HashMap<String, FragmentDefinition>())
            .variables(ei.getVariables())
            .build()
            .transform(visitor)

        then:
        List<ExecutionInput> splitSet = visitor.getExtractedEIs()
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    aliasB: fieldB\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "can split execution input with arguments"() {
        given:
        String query = "query { argQuery(id: \"inputA\") { fieldA fieldB @defer } }"
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(new HashMap<String, FragmentDefinition>())
            .variables(ei.getVariables())
            .build()
            .transform(visitor)

        then:
        List<ExecutionInput> splitSet = visitor.getExtractedEIs()
        splitSet.size() == 1
        splitSet.get(0).query == "query {\n" +
                "  argQuery(id: \"inputA\") {\n" +
                "    fieldB\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "can split EI with multiple defer on same level"() {
        given:
        String query = "query { queryA { fieldA fieldB @defer fieldC @defer } }"
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(new HashMap<String, FragmentDefinition>())
            .variables(ei.getVariables())
            .build()
            .transform(visitor)

        then:
        List<ExecutionInput> splitSet = visitor.getExtractedEIs()
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
        String query = "query { queryA { fieldA objectField { fieldD fieldE @defer } } }"
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(new HashMap<String, FragmentDefinition>())
            .variables(ei.getVariables())
            .build()
            .transform(visitor)

        then:
        List<ExecutionInput> splitSet = visitor.getExtractedEIs()
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    objectField {\n" +
                "      fieldE" +
                "\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "Does not split EI when if arg is false"() {
        given:
        String query = "query { queryA { fieldA fieldB fieldC @defer(if: false) } }"
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(new HashMap<String, FragmentDefinition>())
            .variables(ei.getVariables())
            .build()
            .transform(visitor)

        then:
        List<ExecutionInput> splitSet = visitor.getExtractedEIs()
        splitSet.size() == 0
    }

    def "prunes selections sets without fields after removing deferred fields"() {
        given:
        String query = "query { queryA { fieldA objectField { fieldD nestedObject @defer { fieldH @defer} } } }"
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(new HashMap<String, FragmentDefinition>())
            .variables(ei.getVariables())
            .build()
            .transform(visitor)

        then:
        List<ExecutionInput> splitSet = visitor.getExtractedEIs()
        splitSet.size() == 1
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    objectField {\n" +
                "      nestedObject {\n" +
                "        fieldH\n" +
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
        String query = "query { queryA { fieldA objectField @defer { fieldD fieldE @defer } } }"
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(new HashMap<String, FragmentDefinition>())
            .variables(ei.getVariables())
            .build()
            .transform(visitor)

        then:
        List<ExecutionInput> splitSet = visitor.getExtractedEIs()
        splitSet.size() == 2
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    objectField {\n" +
                "      fieldD\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"

        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    objectField {\n" +
                "      fieldE\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "exception thrown for nested defer selections when option is off"() {
        given:
        String query = "query { queryA { fieldA objectField @defer { fieldD fieldE @defer } } }"
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(disabledDeferOptions)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(new HashMap<String, FragmentDefinition>())
            .variables(ei.getVariables())
            .build()
            .transform(visitor)

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
                                    fieldD
                                }
                                ... on TopLevelObject @defer {
                                    fieldE
                                }
                            }
                        }
        """
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(new HashMap<String, FragmentDefinition>())
            .variables(ei.getVariables())
            .build()
            .transform(visitor)

        then:
        List<ExecutionInput> splitSet = visitor.getExtractedEIs()
        splitSet.size() == 1
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    ... on TopLevelObject {\n" +
                "      fieldE\n" +
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
                                objectField {
                                    fieldD
                                }
                                ... on TopLevelObject @defer {
                                    fieldF
                                }
                                objectField {
                                    nestedObject {
                                        fieldH
                                    }
                                }
                                ... on TopLevelObject @defer {
                                    fieldE
                                }
                            }
                        }
        """
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(new HashMap<String, FragmentDefinition>())
            .variables(ei.getVariables())
            .build()
            .transform(visitor)

        then:
        List<ExecutionInput> splitSet = visitor.getExtractedEIs()
        splitSet.size() == 2
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    ... on TopLevelObject {\n" +
                "      fieldF\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    ... on TopLevelObject {\n" +
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
                                objectField {
                                    fieldD
                                }
                                ... on TopLevelObject @defer {
                                    fieldF
                                }
                                objectField {
                                    fieldE
                                }
                                ... on TopLevelObject @defer {
                                    fieldE
                                }
                            }
                        }
        """
        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(new HashMap<String, FragmentDefinition>())
            .variables(ei.getVariables())
            .build()
            .transform(visitor)

        then:
        List<ExecutionInput> splitSet = visitor.getExtractedEIs()
        splitSet.size() == 2
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    ... on TopLevelObject {\n" +
                "      fieldF\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    ... on TopLevelObject {\n" +
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
                                    fieldD
                                    ... deferredInfo @defer
                                }
                            }
                        }
                        fragment deferredInfo on TopLevelObject {
                            fieldE
                        }
        """

        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()
        Document rootDocument = parser.parseDocument(query)
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()
        Field selection = opDef.selectionSet.getSelections().get(0) as Field

        when:
        Map<String, FragmentDefinition> fragmentDefinitionMap = rootDocument.getDefinitionsOfType(FragmentDefinition.class)
            .stream()
            .collect(Collectors.toMap({ fragment -> ((FragmentDefinition)fragment).getName() }, Function.identity()));

        DeferQueryExtractor visitor = DeferQueryExtractor.builder()
            .originalEI(ei)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .fragmentDefinitionMap(fragmentDefinitionMap)
            .build()

        QueryTransformer.newQueryTransformer()
            .schema(orchestrator.getSchema())
            .rootParentType(orchestrator.getSchema().getQueryType())
            .root(selection)
            .fragmentsByName(fragmentDefinitionMap)
            .variables(ei.getVariables())
            .build()
            .transform(visitor)

        then:
        List<ExecutionInput> splitSet = visitor.getExtractedEIs()
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
                         "\nfragment deferredInfo on TopLevelObject {\n" +
                         "  fieldE\n" +
                         "}\n"
    }

    def "thrown exception when building with null defer options"(){
        given:
        ExecutionInput ei = ExecutionInput.newExecutionInput()
            .query("query { queryA { fieldA } }")
            .build()
        Document rootDocument = parser.parseDocument(ei.getQuery())
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()

        when:
        DeferQueryExtractor.builder()
                .originalEI(ei)
                .rootNode(rootDocument)
                .operationDefinition(opDef)
                .deferOptions(null)
                .build()

        then:
        thrown(NullPointerException)
    }

    def "thrown exception when building with null ei"(){
        given:
        ExecutionInput ei = ExecutionInput.newExecutionInput()
                .query("query { queryA { fieldA } }")
                .build()
        Document rootDocument = parser.parseDocument(ei.getQuery())
        OperationDefinition opDef = rootDocument.getFirstDefinitionOfType(OperationDefinition).get()

        when:
        DeferQueryExtractor.builder()
            .originalEI(null)
            .rootNode(rootDocument)
            .operationDefinition(opDef)
            .deferOptions(deferOptions)
            .build()

        then:
        thrown(NullPointerException)
    }
}
