package com.intuit.graphql.orchestrator.federation

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.batch.EntityFetcherBatchResultTransformer
import graphql.ErrorType
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.batch.EntityFetcherBatchResultTransformer.NO_ENTITY_FIELD;

class EntityFetcherBatchResultTransformerSpec extends Specification {
    private final String serviceProviderName = "MockProvider"
    private final String entityTypeName = "MockEntity"
    private final String requestedField = "ExtEntityField"
    private EntityFetcherBatchResultTransformer specUnderTest

    def setup() {
        specUnderTest = new EntityFetcherBatchResultTransformer(serviceProviderName, entityTypeName, requestedField)
    }

    def "get errored fetch throws exception with mapped errors"() {
        given:
        Map<String, Object> batchResultData = ImmutableMap.of(
                "keyField", "mockKeyResult",
                "__typename", "TestEntityType"
        )

        String errorMsg1 = "Provider Generated Error Message."

        List<GraphQLError> errors = Arrays.asList(
                GraphqlErrorBuilder.newError().message(errorMsg1).build()
        )

        DataFetcherResult<Map<String, Object>> providerResultData = DataFetcherResult.<Map<String, Object>>newResult()
                .errors(errors)
                .build()

        DataFetchingEnvironment keyFieldDFEMock = Mock(DataFetchingEnvironment.class)
        List<DataFetchingEnvironment> dfeList = Arrays.asList(keyFieldDFEMock)

        when:
        specUnderTest.toBatchResult(providerResultData, dfeList)

        then:
        def exception = thrown(RuntimeException)

        exception in EntityFetchingException
        ((EntityFetchingException) exception).getErrorType() in ErrorType.DataFetchingException
        exception.getMessage().contains(errorMsg1)
    }

    def "get null entity field throws exception"() {
        given:
        Map<String, Object> batchResultData = new HashMap<>()
        batchResultData.put("_entities",null)

        DataFetcherResult<Map<String, Object>> providerResultData = DataFetcherResult.newResult()
                .data(batchResultData)
                .errors(Collections.EMPTY_LIST)
                .build()

        DataFetchingEnvironment keyFieldDFEMock = Mock(DataFetchingEnvironment.class)

        List<DataFetchingEnvironment> dfeList = Arrays.asList(keyFieldDFEMock)

        when:
        specUnderTest.toBatchResult(providerResultData, dfeList)

        then:
        def exception = thrown(RuntimeException)

        exception in EntityFetchingException
        ((EntityFetchingException) exception).getErrorType() in ErrorType.DataFetchingException
        exception.getMessage().contains(NO_ENTITY_FIELD)
    }

    def "get empty data with no errors throws exception"() {
        when:
        specUnderTest.toBatchResult(DataFetcherResult.<Map<String, Object>>newResult().build(), Collections.EMPTY_LIST)

        then:
        def exception = thrown(RuntimeException)
        exception in EntityFetchingException
        ((EntityFetchingException) exception).getErrorType() in ErrorType.DataFetchingException
        exception.getMessage().contains(NO_ENTITY_FIELD)
    }

    def "get no entity field exception"() {
        given:
        Map<String, Object> batchResultData = ImmutableMap.of(
                "keyField","mockKeyResult",
                "__typename","TestEntityType"
        )

        DataFetcherResult<Map<String, Object>> providerResultData = DataFetcherResult.newResult()
                .data(batchResultData)
                .errors(Collections.EMPTY_LIST)
                .build()

        DataFetchingEnvironment keyFieldDFEMock = Mock(DataFetchingEnvironment.class)

        List<DataFetchingEnvironment> dfeList = Arrays.asList(keyFieldDFEMock)

        when:
        specUnderTest.toBatchResult(providerResultData, dfeList)

        then:
        def exception = thrown(RuntimeException)

        exception in EntityFetchingException
        ((EntityFetchingException) exception).getErrorType() in ErrorType.DataFetchingException
        exception.getMessage().contains(NO_ENTITY_FIELD)
    }

    def "get successful single entity result"() {
        given:
        List<Map<String, Object>> _entityData = Arrays.asList(
            ImmutableMap.of(
            "ExtEntityField","entityFieldResult",
            "__typename","TestEntityType"
            )
        )

        Map<String, Object> providerResultData = ImmutableMap.of(
                "_entities", _entityData
        )

        DataFetcherResult<Map<String, Object>> providerResult = DataFetcherResult.<Map<String, Object>>newResult()
                .data(providerResultData)
                .build()

        DataFetchingEnvironment keyFieldDFEMock = Mock(DataFetchingEnvironment.class)

        List<DataFetchingEnvironment> dfeList = Arrays.asList(keyFieldDFEMock)

        when:
        List<DataFetcherResult<Object>> batchResult = specUnderTest.toBatchResult(providerResult, dfeList)

        then:
        batchResult.size() == 1
        batchResult.get(0).getData() == "entityFieldResult"
    }

    def "get successful multiple entity results"() {
        given:
        List<Map<String, Object>> _entityData = Arrays.asList(
            ImmutableMap.of(
            "ExtEntityField","entityFieldResult1",
            "__typename","TestEntityType"
            ),
            ImmutableMap.of(
                    "ExtEntityField","entityFieldResult2",
                    "__typename","TestEntityType"
            ),
            ImmutableMap.of(
                    "ExtEntityField","entityFieldResult3",
                    "__typename","TestEntityType"
            )
        )

        Map<String, Object> providerResultData = ImmutableMap.of(
                "_entities", _entityData
        )

        DataFetcherResult<Map<String, Object>> providerResult = DataFetcherResult.<Map<String, Object>>newResult()
                .data(providerResultData)
                .build()

        DataFetchingEnvironment keyFieldDFEMock = Mock(DataFetchingEnvironment.class)

        List<DataFetchingEnvironment> dfeList = Arrays.asList(keyFieldDFEMock)

        when:
        List<DataFetcherResult<Object>> batchResult = specUnderTest.toBatchResult(providerResult, dfeList)

        then:
        batchResult.size() == 3
        batchResult.get(0).getData() == ("entityFieldResult1")
        batchResult.get(1).getData() == ("entityFieldResult2")
        batchResult.get(2).getData() == ("entityFieldResult3")
    }
}
