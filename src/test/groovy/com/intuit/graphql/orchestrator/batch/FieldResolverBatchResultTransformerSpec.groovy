package com.intuit.graphql.orchestrator.batch

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import graphql.ErrorType
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import helpers.BaseIntegrationTestSpecification

import org.apache.commons.collections4.MapUtils
import org.apache.commons.lang3.ArrayUtils

class FieldResolverBatchResultTransformerSpec extends BaseIntegrationTestSpecification {

    private static final String[] RESOLVER_SELECTION_SET = [ "a", "b", "c" ]

    private FieldResolverContext fieldResolverContextMock

    private DataFetchingEnvironment dataFetchingEnvironmentMock

    private GraphQLError testGraphQLError

    private FieldResolverBatchResultTransformer subjectUnderTest

    void setup() {
        fieldResolverContextMock = Mock(FieldResolverContext.class)
        dataFetchingEnvironmentMock = Mock(DataFetchingEnvironment.class)

        subjectUnderTest =
                new FieldResolverBatchResultTransformer(RESOLVER_SELECTION_SET, fieldResolverContextMock)

        testGraphQLError = GraphqlErrorBuilder.newError()
                .message("GraphQL Error")
                .errorType(ErrorType.DataFetchingException)
                .extensions(Collections.emptyMap())
                .build()
    }

    void constructorEmptyResolverSelectedFields() {
        when:
        new FieldResolverBatchResultTransformer(
                ArrayUtils.EMPTY_STRING_ARRAY, fieldResolverContextMock)

        then:
        thrown(IllegalArgumentException)
    }

    void toBatchResult_success() {
        given:
        DataFetcherResult<Map<String, Object>> dataFetcherResult =
                DataFetcherResult.<Map<String, Object>>newResult()
                        .errors(Collections.emptyList())
                        .data(ImmutableMap.of("a", ImmutableMap.of("b", ImmutableMap.of("c_0", "cValue"))))
                        .build()

        when:
        List<DataFetcherResult<Object>> actual = subjectUnderTest.toBatchResult(
                dataFetcherResult, Collections.singletonList(dataFetchingEnvironmentMock))
        DataFetcherResult<Object> actualDataFetcherResult = actual.get(0)

        then:
        actual.size() == 1
        actualDataFetcherResult.getData() == "cValue"
    }

    void toBatchResult_NoDataHasErrors_throwsException() {
        given:
        DataFetcherResult<Map<String, Object>> dataFetcherResult =
                DataFetcherResult.<Map<String, Object>>newResult()
                        .errors(Collections.singletonList(testGraphQLError))
                        .build()

        when:
        List<DataFetcherResult<Object>> actual = subjectUnderTest.toBatchResult(
                dataFetcherResult, Collections.singletonList(dataFetchingEnvironmentMock))
        DataFetcherResult<Object> actualDataFetcherResult = actual.get(0)

        then:
        actual.size() == 1
        MapUtils.isEmpty((Map<String,Object>)actualDataFetcherResult.getData())
        actualDataFetcherResult.getErrors().size() == 1
    }

    void toBatchResult_nullData_throwsException() {
        given:
        Map<String, Object> data = new HashMap<>()
        data.put("a", null)
        DataFetcherResult<Map<String, Object>> dataFetcherResult =
                DataFetcherResult.<Map<String, Object>>newResult()
                        .errors(Collections.singletonList(testGraphQLError))
                        .data(data)
                        .build()

        when:
        List<DataFetcherResult<Object>> actual =
                subjectUnderTest.toBatchResult(
                        dataFetcherResult, Collections.singletonList(dataFetchingEnvironmentMock))
        DataFetcherResult<Object> actualDataFetcherResult = actual.get(0)

        then:
        actual.size() == 1
        MapUtils.isEmpty((Map<String,Object>)actualDataFetcherResult.getData())
        actualDataFetcherResult.getErrors().size() == 1
    }

}
