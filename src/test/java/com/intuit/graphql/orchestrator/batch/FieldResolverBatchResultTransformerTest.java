package com.intuit.graphql.orchestrator.batch;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FieldResolverBatchResultTransformerTest {

  private static final String RESOLVER_SELECTION_SET[] = {"a", "b", "c"};

  @Mock private FieldResolverContext fieldResolverContextMock;

  @Mock private DataFetchingEnvironment dataFetchingEnvironmentMock;

  private GraphQLError testGraphQLError;

  private FieldResolverBatchResultTransformer subjectUnderTest;

  @Before
  public void setup() {
    subjectUnderTest =
        new FieldResolverBatchResultTransformer(RESOLVER_SELECTION_SET, fieldResolverContextMock);

    testGraphQLError = GraphqlErrorBuilder.newError()
        .message("GraphQL Error")
        .errorType(ErrorType.DataFetchingException)
        .extensions(Collections.emptyMap())
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorEmptyResolverSelectedFields() {
    new FieldResolverBatchResultTransformer(
        ArrayUtils.EMPTY_STRING_ARRAY, fieldResolverContextMock);
  }

  @Test
  public void toBatchResult_success() {
    DataFetcherResult<Map<String, Object>> dataFetcherResult =
        DataFetcherResult.<Map<String, Object>>newResult()
            .errors(Collections.emptyList())
            .data(ImmutableMap.of("a", ImmutableMap.of("b", ImmutableMap.of("c_0", "cValue"))))
            .build();

    List<DataFetcherResult<Object>> actual =
        subjectUnderTest.toBatchResult(
            dataFetcherResult, Collections.singletonList(dataFetchingEnvironmentMock));

    assertThat(actual).hasSize(1);
    DataFetcherResult<Object> actualDataFetcherResult = actual.get(0);
    assertThat(actualDataFetcherResult.getData()).isEqualTo("cValue");
  }

  @Test
  public void toBatchResult_NoDataHasErrors_throwsException() {
    DataFetcherResult<Map<String, Object>> dataFetcherResult =
        DataFetcherResult.<Map<String, Object>>newResult()
            .errors(Collections.singletonList(testGraphQLError))
            .build();

    List<DataFetcherResult<Object>> actual =
        subjectUnderTest.toBatchResult(
            dataFetcherResult, Collections.singletonList(dataFetchingEnvironmentMock));

    assertThat(actual).hasSize(1);
    DataFetcherResult<Object> actualDataFetcherResult = actual.get(0);
    assertThat(MapUtils.isEmpty((Map<String,Object>)actualDataFetcherResult.getData())).isTrue();
    assertThat(actualDataFetcherResult.getErrors()).hasSize(1);
  }

  @Test
  public void toBatchResult_nullData_throwsException() {
    Map<String, Object> data = new HashMap<>();
    data.put("a", null);
    DataFetcherResult<Map<String, Object>> dataFetcherResult =
        DataFetcherResult.<Map<String, Object>>newResult()
            .errors(Collections.singletonList(testGraphQLError))
            .data(data)
            .build();

    List<DataFetcherResult<Object>> actual =
        subjectUnderTest.toBatchResult(
            dataFetcherResult, Collections.singletonList(dataFetchingEnvironmentMock));

    assertThat(actual).hasSize(1);
    DataFetcherResult<Object> actualDataFetcherResult = actual.get(0);
    assertThat(MapUtils.isEmpty((Map<String,Object>)actualDataFetcherResult.getData())).isTrue();
    assertThat(actualDataFetcherResult.getErrors()).hasSize(1);
  }
}
