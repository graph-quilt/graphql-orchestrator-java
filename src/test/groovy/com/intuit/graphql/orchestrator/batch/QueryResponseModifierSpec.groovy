package com.intuit.graphql.orchestrator.batch

import graphql.execution.DataFetcherResult
import spock.lang.Specification

import static org.assertj.core.api.Assertions.assertThat

class QueryResponseModifierSpec extends Specification {

    void defaultResponseModifierReturnsDataFetcherResult() {
        given:
        final HashMap<String, Object> queryResponse = new HashMap<>()
        final HashMap<Object, Object> internalData = new HashMap<>()
        final ArrayList<Object> listOfErrors = new ArrayList<>()

        listOfErrors.add(new HashMap<>())
        internalData.put("field", "value")
        queryResponse.put("data", internalData)
        queryResponse.put("errors", listOfErrors)

        when:
        final DataFetcherResult<Map<String, Object>> result = new DefaultQueryResponseModifier().modify(queryResponse)

        then:
        assertThat(result.getData().get("field")).isEqualTo("value")
        assertThat(result.getErrors()).hasSize(1)
    }

    void defaultResponseModifierReturnsDefaultCollections() {
        when:
        final DataFetcherResult<Map<String, Object>> result = new DefaultQueryResponseModifier().modify(new HashMap<>())

        then:
        assertThat(result.getData()).isNotNull()
        assertThat(result.getErrors()).isNotNull()
    }
}