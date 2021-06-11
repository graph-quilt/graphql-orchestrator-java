package com.intuit.graphql.orchestrator.batch;

import static graphql.execution.MergedField.newMergedField;
import static graphql.language.Field.newField;
import static org.assertj.core.api.Assertions.assertThat;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class BatchResultTransformerTest {

  private Map<String, Object> batchResults;
  private List<DataFetchingEnvironment> environments;
  private DataFetcherResult<Map<String, Object>> dataFetcherResult;

  private static final BatchResultTransformer batchResultTransformer = new DefaultBatchResultTransformer();

  @Before
  public void setUp() {
    batchResults = new HashMap<>();
    environments = new ArrayList<>();
  }

  @Test
  public void defaultBatchTransformerBatchResultWithError() {

    batchResults.put("field1", "value1");

    GraphQLError error = GraphqlErrorBuilder.newError()
        .message("boom")
        .build();

    final DataFetchingEnvironment dataFetchingEnvironment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
        .mergedField(newMergedField().addField(newField("field1").build()).build())
        .build();

    environments.add(dataFetchingEnvironment);

    dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
        .data(batchResults)
        .error(error)
        .build();

    final List<DataFetcherResult<Object>> results = batchResultTransformer
        .toBatchResult(dataFetcherResult, environments);

    assertThat(results).hasSize(1)
        .extracting(DataFetcherResult::getData)
        .containsOnly("value1");

    assertThat(results)
        .flatExtracting(DataFetcherResult::getErrors)
        .containsOnly(error);

  }

  @Test
  public void defaultBatchTransformerTwoBatchResults() {

    batchResults.put("field1", "value1");
    batchResults.put("field2", "value2");
    final DataFetchingEnvironment field1Fetcher = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
        .mergedField(newMergedField().addField(newField("field1").build()).build())
        .build();

    final DataFetchingEnvironment field2Fetcher = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
        .mergedField(newMergedField().addField(newField("field2").build()).build())
        .build();

    environments.addAll(Arrays.asList(field1Fetcher, field2Fetcher));

    dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
        .data(batchResults)
        .build();

    final List<DataFetcherResult<Object>> results = batchResultTransformer
        .toBatchResult(dataFetcherResult, environments);

    assertThat(results).hasSize(2)
        .extracting(DataFetcherResult::getData)
        .containsOnly("value1", "value2");

    assertThat(results).flatExtracting(DataFetcherResult::getErrors).isEmpty();
  }

  @Test
  public void defaultBatchTransformerWithAlias() {
    batchResults.put("alias", "value1");

    final DataFetchingEnvironment aliasFetcher = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
        .mergedField(newMergedField().addField(newField("field1").alias("alias").build()).build())
        .build();

    environments.add(aliasFetcher);

    dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
        .data(batchResults)
        .build();

    final List<DataFetcherResult<Object>> results = batchResultTransformer
        .toBatchResult(dataFetcherResult, environments);

    assertThat(results).hasSize(1)
        .extracting(DataFetcherResult::getData)
        .containsOnly("value1");
  }

  @Test
  public void defaultBatchTransformerWithMatchingPathError() {
    final DataFetchingEnvironment environment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
        .mergedField(newMergedField().addField(newField("field1").build()).build())
        .build();

    environments.add(environment);

    GraphQLError correctError = GraphqlErrorBuilder.newError()
        .message("boom")
        .path(Arrays.asList("field1", 1, 2))
        .build();

    final GraphQLError shouldBeIgnoredError = GraphqlErrorBuilder.newError()
        .message("boom")
        .path(Arrays.asList("field2", 1, 2))
        .build();

    dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
        .data(batchResults)
        .error(correctError)
        .error(shouldBeIgnoredError)
        .build();

    final List<DataFetcherResult<Object>> results = batchResultTransformer
        .toBatchResult(dataFetcherResult, environments);

    assertThat(results).hasSize(1)
        .flatExtracting(DataFetcherResult::getErrors)
        .containsOnly(correctError)
        .doesNotContain(shouldBeIgnoredError);
  }
}