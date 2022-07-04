package com.intuit.graphql.orchestrator.batch

import spock.lang.Specification

import static graphql.execution.MergedField.newMergedField
import static graphql.language.Field.newField

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingEnvironmentImpl

class BatchResultTransformerSpec extends Specification {

    private Map<String, Object> batchResults
    private List<DataFetchingEnvironment> environments
    private DataFetcherResult<Map<String, Object>> dataFetcherResult

    private static final BatchResultTransformer batchResultTransformer = new DefaultBatchResultTransformer()

    void setup() {
        batchResults = new HashMap<>()
        environments = new ArrayList<>()
    }

    void defaultBatchTransformerBatchResultWithError() {
        given:
        batchResults.put("field1", "value1")

        GraphQLError error = GraphqlErrorBuilder.newError()
                .message("boom")
                .build()

        final DataFetchingEnvironment dataFetchingEnvironment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .mergedField(newMergedField().addField(newField("field1").build()).build())
                .build()

        environments.add(dataFetchingEnvironment)

        dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
                .data(batchResults)
                .error(error)
                .build()

        when:
        final List<DataFetcherResult<Object>> results = batchResultTransformer
                .toBatchResult(dataFetcherResult, environments)

        then:
        results.size() == 1
        results.get(0).getData() == "value1"
        results.get(0).getErrors() == [error]
    }

    void defaultBatchTransformerTwoBatchResults() {
        given:
        batchResults.put("field1", "value1")
        batchResults.put("field2", "value2")

        final DataFetchingEnvironment field1Fetcher = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .mergedField(newMergedField().addField(newField("field1").build()).build())
                .build()
        final DataFetchingEnvironment field2Fetcher = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .mergedField(newMergedField().addField(newField("field2").build()).build())
                .build()

        environments.addAll(Arrays.asList(field1Fetcher, field2Fetcher))

        dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
                .data(batchResults)
                .build()

        when:
        final List<DataFetcherResult<Object>> results = batchResultTransformer
                .toBatchResult(dataFetcherResult, environments)

        then:
        results.size() == 2
        results.collect { it.getData() } == [ "value1", "value2"]
        results.collect{ it.getErrors() }.flatten() == []
    }

    void defaultBatchTransformerWithAlias() {
        given:
        batchResults.put("alias", "value1")

        final DataFetchingEnvironment aliasFetcher = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .mergedField(newMergedField().addField(newField("field1").alias("alias").build()).build())
                .build()

        environments.add(aliasFetcher)

        dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
                .data(batchResults)
                .build()

        when:
        final List<DataFetcherResult<Object>> results = batchResultTransformer
                .toBatchResult(dataFetcherResult, environments)

        then:
        results.size() == 1
        results.collect { it.getData() } == [ "value1" ]
    }

    void defaultBatchTransformerWithMatchingPathError() {
        given:
        final DataFetchingEnvironment environment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .mergedField(newMergedField().addField(newField("field1").build()).build())
                .build()

        environments.add(environment)

        GraphQLError correctError = GraphqlErrorBuilder.newError()
                .message("boom")
                .path(Arrays.asList("field1", 1, 2))
                .build()

        final GraphQLError shouldBeIgnoredError = GraphqlErrorBuilder.newError()
                .message("boom")
                .path(Arrays.asList("field2", 1, 2))
                .build()

        dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
                .data(batchResults)
                .error(correctError)
                .error(shouldBeIgnoredError)
                .build()

        when:
        final List<DataFetcherResult<Object>> results = batchResultTransformer
                .toBatchResult(dataFetcherResult, environments)

        then:
        results.size() == 1
        results.get(0).getErrors() == [ correctError ]
        !results.get(0).getErrors().contains(shouldBeIgnoredError)
    }

}
