package com.intuit.graphql.orchestrator.batch

import graphql.language.Document
import graphql.language.FragmentDefinition
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.parser

class VariableDefinitionFilterSpec extends Specification {

    private String schema = '''
        schema {
            query: QueryType
        }
        type QueryType {
            consumer: ConsumerType
        }
        type ConsumerType {
            liabilities: LiabilitiesType
            income(arg: Int): Int
        }
        type LiabilitiesType {
            totalDebt(arg: Int): Int
        }
        directive @fragment_definition_directive(arg: Int) on FRAGMENT_DEFINITION 
        directive @inline_fragment_directive(arg: Int) on INLINE_FRAGMENT 
        directive @fragment_spread_directive(arg: Int) on FRAGMENT_SPREAD
    '''

    private String nestedArgumentObjectValueSchema = '''
        schema { 
            query: QueryType 
        } 
        type QueryType { 
            field: Int 
            requires_input(arg: InputObject!): Int 
            requires_input_array(arg: [InputObject!]!): Int 
        } 
        input InputObject { 
            field_a: Int 
            field_b: String 
        } 
        directive @field_directive_argument(arg: InputObject) on FIELD_DEFINITION
    '''

    private GraphQLSchema graphQLSchema

    private VariableDefinitionFilter variableDefinitionFilter

    def setup() {
        graphQLSchema = new SchemaGenerator()
                .makeExecutableSchema(new SchemaParser().parse(schema), RuntimeWiring.newRuntimeWiring().build())
        this.variableDefinitionFilter = new VariableDefinitionFilter()
    }

    private GraphQLSchema inputArgumentSchema() {
        return new SchemaGenerator()
                .makeExecutableSchema(new SchemaParser().parse(nestedArgumentObjectValueSchema),
                        RuntimeWiring.newRuntimeWiring().build())
    }

    private Map<String, FragmentDefinition> getFragmentsByName(Document document) {
        return document.getDefinitionsOfType(FragmentDefinition.class).stream()
                .inject([:]) {map, it -> map << [(it.getName()): it]}
    }

    def "produces Variable References In Arguments And Directive Arguments"() {
        given:
        String query = '''
            query($debt_arg: Int $income_arg: Int $inline_arg: Int $spread_arg: Int $definition_arg: Int $operation_arg: Int) {
                consumer {
                    income(arg: $income_arg) 
                    liabilities {
                        ...TestFragment @fragment_spread_directive(arg: $spread_arg) 
                        ...NoDirectiveFragment
                    }
                    ...on ConsumerType @inline_fragment_directive(arg: $inline_arg) {
                        income(arg: $income_arg)
                        }
                    }
                }
                fragment TestFragment on LiabilitiesType @fragment_definition_directive(arg: $definition_arg) {
                    totalDebt(arg: $debt_arg)
                }
                fragment NoDirectiveFragment on LiabilitiesType {
                    totalDebt(arg: $debt_arg)
            }
        '''

        Document document = parser.parseDocument(query)
        final Map<String, FragmentDefinition> fragmentsByName = getFragmentsByName(document)

        when:
        final Set<String> variableReferences = variableDefinitionFilter
                .getVariableReferencesFromNode(graphQLSchema, graphQLSchema.getQueryType(), fragmentsByName,
                        Collections.emptyMap(), document)

        then:
        variableReferences.size() == 5
        variableReferences.containsAll("income_arg", "debt_arg", "inline_arg", "spread_arg", "definition_arg")
    }

    def "variable References In Argument Values"() {
        given:
        String query = '''
            query($int_arg: Int $string_arg: String) {
                requires_input(arg: { 
                    field_a: $int_arg 
                    field_b: $string_arg 
                })
            }
        '''

        graphQLSchema = inputArgumentSchema()

        Document document = parser.parseDocument(query)

        when:
        final Set<String> results = variableDefinitionFilter
                .getVariableReferencesFromNode(graphQLSchema, graphQLSchema.getQueryType(), Collections.emptyMap(),
                        Collections.emptyMap(), document)

        then:
        results.size() == 2
        results.containsAll("int_arg", "string_arg")
    }

    def "variable References In Arrays"() {
        given:
        String query = '''
            query($int_arg: Int $string_arg: String) { 
                requires_input_array(arg: [ {
                    field_a: $int_arg 
                    field_b: $string_arg 
                }])
            }
        '''

        graphQLSchema = inputArgumentSchema()

        Document document = parser.parseDocument(query)

        when:
        final Set<String> results = variableDefinitionFilter
                .getVariableReferencesFromNode(graphQLSchema, graphQLSchema.getQueryType(), Collections.emptyMap(),
                        Collections.emptyMap(), document)

        then:
        results.size() == 2
        results.containsAll("int_arg", "string_arg")
    }

    def "variable References In Query Directive"() {
        given:
        String query = '''
            query($int_arg: Int $string_arg: String) @field_directive_argument(arg: {
                field_a: $int_arg 
                field_b: $string_arg 
            }) { 
                field 
            }
        '''

        graphQLSchema = inputArgumentSchema()

        Document document = parser.parseDocument(query)

        when:
        final Set<String> results = variableDefinitionFilter
                .getVariableReferencesFromNode(graphQLSchema, graphQLSchema.getQueryType(), Collections.emptyMap(),
                        Collections.emptyMap(), document)

        then:
        results.size() == 2
        results.containsAll("int_arg", "string_arg")
    }

    def "test Negative Cases"() {
        given:
        final String negativeTestCaseQuery = "query { consumer { liabilities { totalDebt(arg: 1234) } } }"
        Document document = parser.parseDocument(negativeTestCaseQuery)

        when:
        final Set<String> results = new VariableDefinitionFilter()
                .getVariableReferencesFromNode(graphQLSchema, graphQLSchema.getQueryType(), Collections.emptyMap(),
                        Collections.emptyMap(), document)

        then:
        results.isEmpty()
    }
}