package com.intuit.graphql.orchestrator.batch;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.execution.DataFetcherResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class QueryResponseModifierTest {

  @Test
  public void defaultResponseModifierReturnsDataFetcherResult() {
    final HashMap<String, Object> queryResponse = new HashMap<>();
    final HashMap<Object, Object> internalData = new HashMap<>();
    final ArrayList<Object> listOfErrors = new ArrayList<>();
    listOfErrors.add(new HashMap<>());
    internalData.put("field", "value");
    queryResponse.put("data", internalData);
    queryResponse.put("errors", listOfErrors);

    final DataFetcherResult<Map<String, Object>> result = new DefaultQueryResponseModifier().modify(queryResponse);

    assertThat(result.getData().get("field")).isEqualTo("value");
    assertThat(result.getErrors()).hasSize(1);
  }

  @Test
  public void defaultResponseModifierReturnsDefaultCollections() {
    final DataFetcherResult<Map<String, Object>> result = new DefaultQueryResponseModifier().modify(new HashMap<>());
    assertThat(result.getData()).isNotNull();
    assertThat(result.getErrors()).isNotNull();
  }
}