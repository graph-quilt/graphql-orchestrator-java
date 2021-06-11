package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.TestHelper.document;
import static com.intuit.graphql.orchestrator.batch.GraphQLTestUtil.buildCompleteExecutionStepInfo;
import static graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment;
import static org.assertj.core.api.Assertions.assertThat;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.execution.ExecutionStepInfo;
import graphql.language.Document;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

public class SubtreeBatchResultTransformerTest {

  private String query = "query {\n"
      + "  consumer {\n"
      + "    finance {\n"
      + "      tax {\n"
      + "        returns {\n"
      + "          returnHeader {\n"
      + "            taxYr\n"
      + "          }\n"
      + "        }\n"
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
      + "      health {\n"
      + "        creditScore\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  private Document document;

  @Before
  public void setUp() {
    document = document(query);
  }

  @Test
  public void nullInitialdataReturnsNull() {
    final ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "finance");

    DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
        .mergedField(executionStepInfo.getField())
        .executionStepInfo(executionStepInfo).build();

    DataFetcherResult<Map<String, Object>> dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
        .data(null)
        .build();

    final List<DataFetcherResult<Object>> results = new SubtreeBatchResultTransformer()
        .toBatchResult(dataFetcherResult, Collections.singletonList(dataFetchingEnvironment));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getData()).isNull();
  }

  @Test
  public void nullDataPathReturnsNull() {
    Map<String, Object> data = new HashMap<>();

    data.put("consumer", new HashMap<String, String>() {{
      put("finance", null);
    }});

    final ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "finance");

    DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
        .mergedField(executionStepInfo.getField())
        .executionStepInfo(executionStepInfo).build();

    DataFetcherResult<Map<String, Object>> dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
        .data(data)
        .build();

    final List<DataFetcherResult<Object>> results = new SubtreeBatchResultTransformer()
        .toBatchResult(dataFetcherResult, Collections.singletonList(dataFetchingEnvironment));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getData()).isNull();
  }

  @Test
  public void producesPartitionedResult() {
    Map<String, Object> data = new HashMap<>();

    data.put("consumer", new HashMap<String, String>() {{
      put("finance", "test");
      put("shouldBeIgnored", null);
    }});

    final ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "finance");

    DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
        .mergedField(executionStepInfo.getField())
        .executionStepInfo(executionStepInfo).build();

    DataFetcherResult<Map<String, Object>> dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
        .data(data)
        .build();

    final List<DataFetcherResult<Object>> results = new SubtreeBatchResultTransformer()
        .toBatchResult(dataFetcherResult, Collections.singletonList(dataFetchingEnvironment));

    assertThat(results).hasSize(1)
        .extracting(DataFetcherResult::getData)
        .containsExactly("test");
  }

  @Test
  public void addErrorsToPartitionedResult() {
    Map<String, Object> data = new HashMap<>();

    data.put("consumer", new HashMap<String, String>() {{
      put("finance", "test");
      put("experiences", "test");
      put("financialProfile", "test");
      put("shouldBeIgnored", null);
    }});

    List<GraphQLError> errors = new ArrayList<GraphQLError>() {{
      add(GraphqlErrorBuilder.newError()
          .message("Exception while fetching data (/consumer/shouldBeIgnored)")
          .build());
    }};

    final ExecutionStepInfo executionStepInfo1 = buildCompleteExecutionStepInfo(document, "consumer", "finance");
    final ExecutionStepInfo executionStepInfo2 = buildCompleteExecutionStepInfo(document, "consumer", "experiences");
    final ExecutionStepInfo executionStepInfo3 = buildCompleteExecutionStepInfo(document, "consumer",
        "financialProfile");

    DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
        .mergedField(executionStepInfo1.getField())
        .executionStepInfo(executionStepInfo1).build();

    DataFetchingEnvironment dfe2 = newDataFetchingEnvironment()
        .mergedField(executionStepInfo2.getField())
        .executionStepInfo(executionStepInfo2).build();

    DataFetchingEnvironment dfe3 = newDataFetchingEnvironment()
        .mergedField(executionStepInfo3.getField())
        .executionStepInfo(executionStepInfo3).build();

    DataFetcherResult<Map<String, Object>> dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
        .data(data)
        .errors(errors)
        .build();

    final List<DataFetcherResult<Object>> results = new SubtreeBatchResultTransformer()
        .toBatchResult(dataFetcherResult, new ArrayList<>(Arrays.asList(dfe1, dfe2, dfe3)));

    assertThat(results).hasSize(3)
        .extracting(DataFetcherResult::getData)
        .containsExactly("test", "test", "test");

    assertThat(results.stream()
        .flatMap(result -> result.getErrors().stream())
        .collect(Collectors.toList()))
        .hasSize(1);
  }


}
