package com.intuit.graphql.orchestrator.batch

import graphql.execution.DataFetcherResult
import spock.lang.Specification

class QueryResponseModifierSpec extends Specification {

    def "default Response Modifier Returns Data Fetcher Result"() {
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
        result.getData().get("field") == "value"
        result.getErrors().size() == 1
    }

    def "default Response Modifier Returns Default Collections"() {
        when:
        final DataFetcherResult<Map<String, Object>> result = new DefaultQueryResponseModifier().modify(new HashMap<>())

        then:
        result.getData() != null
        result.getErrors() != null
    }
}