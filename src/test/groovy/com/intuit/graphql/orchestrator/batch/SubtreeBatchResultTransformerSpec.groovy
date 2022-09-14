package com.intuit.graphql.orchestrator.batch

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherResult
import graphql.execution.ExecutionStepInfo
import graphql.language.Document
import graphql.schema.DataFetchingEnvironment
import spock.lang.Specification

import java.util.stream.Collectors

import static com.intuit.graphql.orchestrator.TestHelper.document
import static com.intuit.graphql.orchestrator.batch.GraphQLTestUtil.buildCompleteExecutionStepInfo
import static graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment

class SubtreeBatchResultTransformerSpec extends Specification {

    private String query = '''
        query {
            consumer {
                finance {
                    tax {
                        returns {
                            returnHeader {
                                taxYr
                            }
                        }
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
                    health {
                        creditScore
                    }
                }
            }
        }
    '''

    private Document document

    def setup() {
        document = document(query)
    }

    def "null Initial Data Returns Null"() {
        given:
        final ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "finance")

        DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
                .mergedField(executionStepInfo.getField())
                .executionStepInfo(executionStepInfo).build()

        DataFetcherResult<Map<String, Object>> dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
                .data(null)
                .build()

        when:
        final List<DataFetcherResult<Object>> results = new SubtreeBatchResultTransformer()
                .toBatchResult(dataFetcherResult, Collections.singletonList(dataFetchingEnvironment))

        then:
        results.size() == 1
        results.get(0).getData() == null
    }

    def "null Data Path Returns Null"() {
        given:
        Map<String, Object> data = new HashMap<>()

        data.put("consumer", new HashMap<String, String>() {{
            put("finance", null)
        }})

        final ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "finance")

        DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
                .mergedField(executionStepInfo.getField())
                .executionStepInfo(executionStepInfo).build()

        DataFetcherResult<Map<String, Object>> dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
                .data(data)
                .build()

        when:
        final List<DataFetcherResult<Object>> results = new SubtreeBatchResultTransformer()
                .toBatchResult(dataFetcherResult, Collections.singletonList(dataFetchingEnvironment))

        then:
        results.size() == 1
        results.get(0).getData() == null
    }

    def "produces Partitioned Result"() {
        given:
        Map<String, Object> data = new HashMap<>()

        data.put("consumer", new HashMap<String, String>() {{
            put("finance", "test")
            put("shouldBeIgnored", null)
        }})

        final ExecutionStepInfo executionStepInfo = buildCompleteExecutionStepInfo(document, "consumer", "finance")

        DataFetchingEnvironment dataFetchingEnvironment = newDataFetchingEnvironment()
                .mergedField(executionStepInfo.getField())
                .executionStepInfo(executionStepInfo).build()

        DataFetcherResult<Map<String, Object>> dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
                .data(data)
                .build()

        when:
        final List<DataFetcherResult<Object>> results = new SubtreeBatchResultTransformer()
                .toBatchResult(dataFetcherResult, Collections.singletonList(dataFetchingEnvironment))

        then:
        results.size() == 1
        results.get(0).getData() == "test"
    }

    def "add Errors To Partitioned Result"() {
        given:
        Map<String, Object> data = new HashMap<>()

        data.put("consumer", new HashMap<String, String>() {{
            put("finance", "test")
            put("experiences", "test")
            put("financialProfile", "test")
            put("shouldBeIgnored", null)
        }})

        List<GraphQLError> errors = new ArrayList<GraphQLError>() {{
            add(GraphqlErrorBuilder.newError()
                    .message("Exception while fetching data (/consumer/shouldBeIgnored)")
                    .build())
        }}

        final ExecutionStepInfo executionStepInfo1 = buildCompleteExecutionStepInfo(document, "consumer", "finance")
        final ExecutionStepInfo executionStepInfo2 = buildCompleteExecutionStepInfo(document, "consumer", "experiences")
        final ExecutionStepInfo executionStepInfo3 = buildCompleteExecutionStepInfo(document, "consumer",
                "financialProfile")

        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .mergedField(executionStepInfo1.getField())
                .executionStepInfo(executionStepInfo1).build()

        DataFetchingEnvironment dfe2 = newDataFetchingEnvironment()
                .mergedField(executionStepInfo2.getField())
                .executionStepInfo(executionStepInfo2).build()

        DataFetchingEnvironment dfe3 = newDataFetchingEnvironment()
                .mergedField(executionStepInfo3.getField())
                .executionStepInfo(executionStepInfo3).build()

        DataFetcherResult<Map<String, Object>> dataFetcherResult = DataFetcherResult.<Map<String, Object>>newResult()
                .data(data)
                .errors(errors)
                .build()

        when:
        final List<DataFetcherResult<Object>> results = new SubtreeBatchResultTransformer()
                .toBatchResult(dataFetcherResult, new ArrayList<>(Arrays.asList(dfe1, dfe2, dfe3)))

        then:
        results.size() == 3
        results.collect{ it -> it.getData() } == ["test", "test", "test" ]

        results.stream()
                .flatMap({ result -> result.getErrors().stream() })
                .collect(Collectors.toList()).size() == 1
    }

}
