package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.TestHelper.document;
import static com.intuit.graphql.orchestrator.TestHelper.fragmentDefinitions;
import static com.intuit.graphql.orchestrator.batch.GraphQLTestUtil.buildCompleteExecutionStepInfo;
import static com.intuit.graphql.orchestrator.batch.GraphQLTestUtil.printPreOrder;
import static graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.intuit.graphql.orchestrator.TestHelper;
import com.intuit.graphql.orchestrator.batch.MergedFieldModifier.MergedFieldModifierResult;
import graphql.execution.ExecutionStepInfo;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class MergedFieldModifierTest {

  private String schema = "schema {\n"
      + "  query: QueryType\n"
      + "}\n"
      + "\n"
      + "type QueryType {\n"
      + "  consumer: ConsumerType\n"
      + "}\n"
      + "\n"
      + "type ConsumerType {\n"
      + "  finance: FinanceType\n"
      + "  experiences: ExperiencesType\n"
      + "  financialProfile: FinancialProfileType\n"
      + "}\n"
      + "\n"
      + "type ReturnsType {\n"
      + "  returnHeader: ReturnHeaderType\n"
      + "}\n"
      + "\n"
      + "type ReturnHeaderType {\n"
      + "  taxYr: String\n"
      + "  someField(some_field_arg: Int): String\n"
      + "}\n"
      + "\n"
      + "type FakeObjectType {\n"
      + "  fakeField: String\n"
      + "}\n"
      + "\n"
      + "type FinanceType {\n"
      + "  tax: TaxType\n"
      + "  fakeObject: FakeObjectType\n"
      + "}\n"
      + "\n"
      + "type TaxType {\n"
      + "  returns: ReturnsType\n"
      + "}\n"
      + "\n"
      + "type ExperiencesType {\n"
      + "  TestExperienceData: TestExperienceDataType\n"
      + "}\n"
      + "\n"
      + "type TestExperienceDataType {\n"
      + "  test_content: TestContentType\n"
      + "  test_values: String\n"
      + "}\n"
      + "\n"
      + "type TestContentType {\n"
      + "  test_name: String\n"
      + "  test_number: Int\n"
      + "  test_type: String\n"
      + "}\n"
      + "\n"
      + "type FinancialProfileType {\n"
      + "  health: HealthType\n"
      + "  liabilities: LiabilitiesType\n"
      + "}\n"
      + "\n"
      + "type HealthType {\n"
      + "  creditScore: Int\n"
      + "}\n"
      + "\n"
      + "type LiabilitiesType {\n"
      + "  totalDebt(debt_arg_1: Int debt_arg_2: Int debt_arg_3: String): Int\n"
      + "}";

  private String query = "query($debt_arg_1: Int $debt_arg_2: Int $debt_arg_3: String $some_field_arg: Int) {\n"
      + "  consumer {\n"
      + "    finance {\n"
      + "      tax {\n"
      + "        returns {\n"
      + "          returnHeader {\n"
      + "            taxYr\n"
      + "            ...on ReturnHeaderType {\n"
      + "              someField(some_field_arg: $some_field_arg)\n"
      + "            }\n"
      + "          }\n"
      + "        }\n"
      + "      }\n"
      + "      fakeObject {\n"
      + "        fakeField\n"
      + "      }\n"
      + "    }\n"
      + "    experiences {\n"
      + "      TestExperienceData {\n"
      + "        test_content {\n"
      + "          test_name\n"
      + "          test_number\n"
      + "          test_type\n"
      + "        }\n"
      + "        test_values\n"
      + "      }\n"
      + "    }\n"
      + "    financialProfile {\n"
      + "      ...TestFragment\n"
      + "    }\n"
      + "  }\n"
      + "}\n"
      + "\n"
      + "fragment TestFragment on FinancialProfileType {\n"
      + "\thealth {\n"
      + "    creditScore\n"
      + "  }\n"
      + "  liabilities {\n"
      + "    totalDebt(debt_arg_1: $debt_arg_1 debt_arg_2: $debt_arg_2 debt_arg_3: $debt_arg_3)\n"
      + "  }\n"
      + "}";

  private Document document;
  private GraphQLSchema graphQLSchema;
  private Map<String, FragmentDefinition> fragmentDefinitions;

  @Before
  public void setUp() {
    graphQLSchema = TestHelper.schema(schema);
    document = document(query);
    fragmentDefinitions = fragmentDefinitions(query);
  }

  @Test
  public void edgeCases() {
    assertThatThrownBy(() -> new MergedFieldModifier(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void filtersFragmentDefinitionsOnObject() {
    ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "financialProfile",
        "health");

    final DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
        .executionStepInfo(executionStepInfo)
        .fragmentsByName(fragmentDefinitions)
        .mergedField(executionStepInfo.getField())
        .build();

    final MergedFieldModifierResult result = new MergedFieldModifier(dataFetchingEnvironment)
        .getFilteredRootField();

    assertThat(printPreOrder(result.getMergedField().getSingleField(), graphQLSchema, result.getFragmentDefinitions()))
        .containsExactly("consumer", "financialProfile", "spread:health", "spread:creditScore");
  }

  @Test
  public void filtersInlineFragments() {
    ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "finance", "tax",
        "returns", "returnHeader", "taxYr");

    final DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
        .executionStepInfo(executionStepInfo)
        .mergedField(executionStepInfo.getField())
        .build();

    final MergedFieldModifierResult result = new MergedFieldModifier(dataFetchingEnvironment).getFilteredRootField();

    assertThat(printPreOrder(result.getMergedField().getSingleField(), graphQLSchema, result.getFragmentDefinitions()))
        .containsExactly("consumer", "finance", "tax", "returns", "returnHeader", "taxYr");
  }

  @Test
  public void retainsInlineFragments() {
    ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "finance", "tax",
        "returns", "returnHeader", "someField");

    final DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
        .executionStepInfo(executionStepInfo)
        .mergedField(executionStepInfo.getField())
        .build();

    final MergedFieldModifierResult result = new MergedFieldModifier(dataFetchingEnvironment).getFilteredRootField();

    assertThat(printPreOrder(result.getMergedField().getSingleField(), graphQLSchema, result.getFragmentDefinitions()))
        .containsExactly("consumer", "finance", "tax", "returns", "returnHeader", "inline:someField");
  }

  @Test
  public void filtersNonRelevantFields() {
    ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "finance", "tax");

    DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
        .executionStepInfo(executionStepInfo)
        .mergedField(executionStepInfo.getField())
        .build();

    final MergedFieldModifierResult result = new MergedFieldModifier(dataFetchingEnvironment).getFilteredRootField();

    assertThat(printPreOrder(result.getMergedField().getSingleField(), graphQLSchema, result.getFragmentDefinitions()))
        .containsExactly("consumer", "finance", "tax", "returns", "returnHeader", "taxYr", "inline:someField");
  }

//  @Test
//  public void producesVariableReferences() {
//    ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "financialProfile", "liabilities");
//
//    DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
//        .executionStepInfo(executionStepInfo)
//        .fragmentsByName(fragmentDefinitions)
//        .mergedField(executionStepInfo.getField())
//        .build();
//
//    final MergedFieldModifierResult result = new MergedFieldModifier(dataFetchingEnvironment).getFilteredRootField();
//
//    assertThat(result.getVariableReferences()).hasSize(3);
//    assertThat(result.getVariableReferences()).extracting(VariableReference::getName)
//        .containsOnly("debt_arg_1", "debt_arg_2", "debt_arg_3");
//
//  }
}
