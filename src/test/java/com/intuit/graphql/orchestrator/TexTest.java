package com.intuit.graphql.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.BooleanValue;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import graphql.language.Value;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * Covers test for ObjectTypeExtension, InterfaceTypeExtension, UnionTypeExtension, EnumTypeExtension,
 * InputObjectTypeExtension TODO ScalarTypeExtension.
 */
@Slf4j
public class TexTest {

  @Test
  public void canMakeQuery() throws Exception {
    // person is of type PersonInterface which is extended to add field address
    TestCase testCase = TestCase.newTestCase()
        .service(new TexService())
        .query("{ consumer { experiences { "
            + "CSGoalExperimentData { cs_target { goal timestamp } notification { opt_in timestamp } steps_complete { name timestamp isComplete }} "
            + "CSSimulationExperimentData { simulation_reference { id timestamp } hook { discarded timestamp } } "
            + "}}}")
        .build();

    testCase.run();
    testCase.assertHashNoErrors();
    testCase.assertHasData();

    Map<String, Object> consumer = (Map<String, Object>) testCase.getDataField("consumer");
    Map<String, Object> experiences = (Map<String, Object>) consumer.get("experiences");
    assertThat(experiences.keySet()).hasSize(2);

    Map<String, Object> CSGoalExperimentData = (Map<String, Object>) experiences.get("CSGoalExperimentData");
    assertThat(CSGoalExperimentData.keySet()).hasSize(3);

    Map<String, Object> CSSimulationExperimentData = (Map<String, Object>) experiences
        .get("CSSimulationExperimentData");
    assertThat(CSSimulationExperimentData.keySet()).hasSize(2);
  }

  @Test
  public void canMakeMutationWithMergeDirectiveOnField() throws Exception {
    // person is of type PersonInterface which is extended to add field address
    TestCase testCase = TestCase.newTestCase()
        .service(new TexService())
        .query("mutation test($expData: TestExperience_TEST_Input) {  "
            + "updateTestExperienceData(input:$expData) @merge(if : true) {    test_content {      test_name      test_number      test_type    }  }}")
        .variables(ImmutableMap
            .of("expData", ImmutableMap.of("test_content", ImmutableMap.of("test_name", "perf-test-case-3"))))
        .build();

    testCase.run();
    testCase.assertHashNoErrors();
    testCase.assertHasData();

    Map<String, Object> updateTestExperienceData = (Map<String, Object>) testCase
        .getDataField("updateTestExperienceData");
    Map<String, Object> test_content = (Map<String, Object>) updateTestExperienceData.get("test_content");
    assertThat(test_content.keySet()).hasSize(3);
    assertThat(test_content.get("test_name")).isEqualTo("perf-test-case-3");
    assertThat(test_content.get("test_number")).isNull();
    assertThat(test_content.get("test_type")).isNull();
  }

  class TexService implements ServiceProvider {

    @Override
    public String getNameSpace() {
      return "TEX";
    }

    @Override
    public Map<String, String> sdlFiles() {
      return TestHelper.getFileMapFromList("nested/tex/schema.graphqls");
    }

    @Override
    public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
        GraphQLContext context) {
      Document document = (Document) executionInput.getRoot();
      OperationDefinition opDep = (OperationDefinition) document.getDefinitions().get(0);

      if (opDep.getOperation().equals(Operation.QUERY)) {
        Map<String, Object> experiences = new HashMap<>();
        experiences.put("CSGoalExperimentData", CSGoalExperimentData());
        experiences.put("CSSimulationExperimentData", createCSSimulationExperimentData());

        Map<String, Object> consumer = new HashMap<>();
        consumer.put("experiences", experiences);

        Map<String, Object> data = new HashMap<>();
        data.put("consumer", consumer);
        return CompletableFuture.completedFuture(ImmutableMap.of("data", data));
      }

      if (opDep.getOperation().equals(Operation.MUTATION)) {
        Field updateTestExperienxeDataField = (Field) opDep.getSelectionSet().getSelections().get(0);
        assertThat(updateTestExperienxeDataField.getDirectives().get(0).getName()).isEqualTo("merge");

        Value val = updateTestExperienxeDataField.getDirectives().get(0).getArguments().get(0).getValue();
        BooleanValue booleanValue = (BooleanValue) val;
        assertThat(booleanValue.isValue()).isEqualTo(true);

        Map<String, Object> test_content = new HashMap<>();
        test_content.put("test_name", "perf-test-case-3");

        Map<String, Object> updateTestExperienceData = new HashMap<>();
        updateTestExperienceData.put("test_content", test_content);

        Map<String, Object> data = new HashMap<>();
        data.put("updateTestExperienceData", updateTestExperienceData);

        return CompletableFuture.completedFuture(ImmutableMap.of("data", data));
      }

      return CompletableFuture.completedFuture(ImmutableMap.of("data", null));
    }

    private Map<String, Object> CSGoalExperimentData() {
      Map<String, Object> map = new HashMap<>();
      map.put("cs_target", ImmutableMap.of("goal", new Integer(1), "timestamp", "1111111111"));
      map.put("notification", ImmutableMap.of("opt_in", new Integer(1), "timestamp", "2222222222"));
      map.put("steps_complete", Arrays.asList(
          ImmutableMap.of("isComplete", new Boolean(true), "name", "CREDIT_UTILIZATION", "timestamp", "3333333333")
      ));
      return map;
    }

    private Map<String, Object> createCSSimulationExperimentData() {
      Map<String, Object> map = new HashMap<>();
      map.put("hook", ImmutableMap.of("discarded", new Boolean(false), "timestamp", "4444444444"));
      map.put("simulation_reference", ImmutableMap.of("id", "SOMEID", "timestamp", "5555555555"));
      return map;
    }
  }

}
