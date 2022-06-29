package com.intuit.graphql.orchestrator.batch

import spock.lang.Specification

import static com.intuit.graphql.orchestrator.TestHelper.document
import static com.intuit.graphql.orchestrator.TestHelper.fragmentDefinitions
import static com.intuit.graphql.orchestrator.batch.GraphQLTestUtil.buildCompleteExecutionStepInfo
import static com.intuit.graphql.orchestrator.batch.GraphQLTestUtil.printPreOrder
import static graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment

import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.batch.MergedFieldModifier.MergedFieldModifierResult
import graphql.execution.ExecutionStepInfo
import graphql.language.Document
import graphql.language.FragmentDefinition
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLSchema

class MergedFieldModifierSpec extends Specification {

    private String schema = '''
        schema {
          query: QueryType
        }
        
        type QueryType {
          consumer: ConsumerType
        }
        
        type ConsumerType {
          finance: FinanceType
          experiences: ExperiencesType
          financialProfile: FinancialProfileType
        }
        
        type ReturnsType {
          returnHeader: ReturnHeaderType
        }
        
        type ReturnHeaderType {
          taxYr: String
          someField(some_field_arg: Int): String
        }
        
        type FakeObjectType {
          fakeField: String
        }
        
        type FinanceType {
          tax: TaxType
          fakeObject: FakeObjectType
        }
        
        type TaxType {
          returns: ReturnsType
        }
        
        type ExperiencesType {
          TestExperienceData: TestExperienceDataType
        }
        
        type TestExperienceDataType {
          test_content: TestContentType
          test_values: String
        }
        
        type TestContentType {
          test_name: String
          test_number: Int
          test_type: String
        }
        
        type FinancialProfileType {
          health: HealthType
          liabilities: LiabilitiesType
        }
        
        type HealthType {
          creditScore: Int
        }
        
        type LiabilitiesType {
          totalDebt(debt_arg_1: Int debt_arg_2: Int debt_arg_3: String): Int
        }
    '''

    private String query = '''
        query($debt_arg_1: Int $debt_arg_2: Int $debt_arg_3: String $some_field_arg: Int) {
          consumer {
            finance {
              tax {
                returns {
                  returnHeader {
                    taxYr
                    ...on ReturnHeaderType {
                      someField(some_field_arg: $some_field_arg)
                    }
                  }
                }
              }
              fakeObject {
                fakeField
              }
            }
            experiences {
              TestExperienceData {
                test_content {
                  test_name
                  test_number
                  test_type
                }
                test_values
              }
            }
            financialProfile {
              ...TestFragment
            }
          }
        }
        
        fragment TestFragment on FinancialProfileType {
        health {
            creditScore
          }
          liabilities {
            totalDebt(debt_arg_1: $debt_arg_1 debt_arg_2: $debt_arg_2 debt_arg_3: $debt_arg_3)
          }
        }
    '''

    private Document document
    private GraphQLSchema graphQLSchema
    private Map<String, FragmentDefinition> fragmentDefinitions

    void setup() {
        graphQLSchema = TestHelper.schema(schema)
        document = document(query)
        fragmentDefinitions = fragmentDefinitions(query)
    }

    void edgeCases() {
        when:
        new MergedFieldModifier(null)

        then:
        thrown(NullPointerException)
    }

    void filtersFragmentDefinitionsOnObject() {
        given:
        ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "financialProfile",
                "health")

        final DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
                .executionStepInfo(executionStepInfo)
                .fragmentsByName(fragmentDefinitions)
                .mergedField(executionStepInfo.getField())
                .build()

        when:
        final MergedFieldModifierResult result = new MergedFieldModifier(dataFetchingEnvironment)
                .getFilteredRootField()

        then:
        printPreOrder(result.getMergedField().getSingleField(), graphQLSchema, result.getFragmentDefinitions()) == [ "consumer", "financialProfile", "spread:health", "spread:creditScore" ]
    }

    void filtersInlineFragments() {
        given:
        ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(
                document, "consumer", "finance", "tax", "returns", "returnHeader", "taxYr")

        final DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
                .executionStepInfo(executionStepInfo)
                .mergedField(executionStepInfo.getField())
                .build()

        when:
        final MergedFieldModifierResult result = new MergedFieldModifier(dataFetchingEnvironment).getFilteredRootField()

        then:
        printPreOrder(result.getMergedField().getSingleField(), graphQLSchema, result.getFragmentDefinitions()) == ["consumer", "finance", "tax", "returns", "returnHeader", "taxYr"]
    }

    void retainsInlineFragments() {
        given:
        ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "finance", "tax",
                "returns", "returnHeader", "someField")

        final DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
                .executionStepInfo(executionStepInfo)
                .mergedField(executionStepInfo.getField())
                .build()

        when:
        final MergedFieldModifierResult result = new MergedFieldModifier(dataFetchingEnvironment).getFilteredRootField()

        then:
        printPreOrder(result.getMergedField().getSingleField(), graphQLSchema, result.getFragmentDefinitions()) == [ "consumer", "finance", "tax", "returns", "returnHeader", "inline:someField" ]
    }

    void filtersNonRelevantFields() {
        given:
        ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "finance", "tax")

        DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
                .executionStepInfo(executionStepInfo)
                .mergedField(executionStepInfo.getField())
                .build()

        when:
        final MergedFieldModifierResult result = new MergedFieldModifier(dataFetchingEnvironment).getFilteredRootField()

        then:
        printPreOrder(result.getMergedField().getSingleField(), graphQLSchema, result.getFragmentDefinitions()) == ["consumer", "finance", "tax", "returns", "returnHeader", "taxYr", "inline:someField"]
    }

//  @Test
//  public void producesVariableReferences() {
//    ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "financialProfile", "liabilities")
//
//    DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
//        .executionStepInfo(executionStepInfo)
//        .fragmentsByName(fragmentDefinitions)
//        .mergedField(executionStepInfo.getField())
//        .build()
//
//    final MergedFieldModifierResult result = new MergedFieldModifier(dataFetchingEnvironment).getFilteredRootField()
//
//    assertThat(result.getVariableReferences()).hasSize(3)
//    assertThat(result.getVariableReferences()).extracting(VariableReference::getName)
//        .containsOnly("debt_arg_1", "debt_arg_2", "debt_arg_3")
//
//  }

}
