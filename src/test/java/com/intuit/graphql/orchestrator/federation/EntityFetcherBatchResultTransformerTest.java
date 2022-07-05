package com.intuit.graphql.orchestrator.federation;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.batch.EntityFetcherBatchResultTransformer;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intuit.graphql.orchestrator.batch.EntityFetcherBatchResultTransformer.NO_ENTITY_FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class EntityFetcherBatchResultTransformerTest {
    private final String serviceProviderName = "MockProvider";
    private final String entityTypeName = "MockEntity";
    private final String requestedField = "ExtEntityField";
    private EntityFetcherBatchResultTransformer specUnderTest;

    @Before
    public void setup() {
        specUnderTest = new EntityFetcherBatchResultTransformer(serviceProviderName, entityTypeName, requestedField);
    }

    @Test
    public void get_errored_fetch_throws_exception_with_mapped_errors() {
        Map<String, Object> batchResultData = ImmutableMap.of(
                "keyField", "mockKeyResult",
                "__typename", "TestEntityType"
        );

        String errorMsg1 = "Provider Generated Error Message.";

        List<GraphQLError> errors = Arrays.asList(
                GraphqlErrorBuilder.newError().message(errorMsg1).build()
        );

        DataFetcherResult<Map<String, Object>> providerResultData = DataFetcherResult.<Map<String, Object>>newResult()
                .errors(errors)
                .build();

        DataFetchingEnvironment keyFieldDFEMock = mock(DataFetchingEnvironment.class);
        List<DataFetchingEnvironment> dfeList = Arrays.asList(keyFieldDFEMock);


        try {
            specUnderTest.toBatchResult(providerResultData, dfeList);
            Assert.fail();
        } catch (RuntimeException ex) {
            if (ex instanceof EntityFetchingException) {
                assertThat(((EntityFetchingException) ex).getErrorType()).isEqualTo(ErrorType.DataFetchingException);
                assertThat(ex.getMessage()).contains(errorMsg1);
            } else {
                throw ex;
            }
        }
    }

    @Test
    public void get_null_entity_field_throws_exception() {
        Map<String, Object> batchResultData = new HashMap<>();
        batchResultData.put("_entities",null);

        DataFetcherResult<Map<String, Object>> providerResultData = DataFetcherResult.newResult()
                .data(batchResultData)
                .errors(Collections.EMPTY_LIST)
                .build();

        DataFetchingEnvironment keyFieldDFEMock = mock(DataFetchingEnvironment.class);

        List<DataFetchingEnvironment> dfeList = Arrays.asList(keyFieldDFEMock);

        try {
            specUnderTest.toBatchResult(providerResultData, dfeList);
            Assert.fail();
        } catch (RuntimeException ex) {
            if(ex instanceof EntityFetchingException) {
                assertThat(((EntityFetchingException) ex).getErrorType()).isEqualTo(ErrorType.DataFetchingException);
                assertThat(ex.getMessage()).contains(NO_ENTITY_FIELD);
            } else {
                throw ex;
            }
        }
    }

    @Test
    public void get_empty_data_with_no_errors_throws_exception() {
        try {
            specUnderTest.toBatchResult(DataFetcherResult.<Map<String, Object>>newResult().build(), Collections.EMPTY_LIST);
            Assert.fail("Exception should be thrown due to no _entities field");
        } catch (RuntimeException ex) {
            if(ex instanceof EntityFetchingException) {
                assertThat(((EntityFetchingException) ex).getErrorType()).isEqualTo(ErrorType.DataFetchingException);
                assertThat(ex.getMessage()).contains(NO_ENTITY_FIELD);
            } else {
                throw ex;
            }
        }
    }

    @Test
    public void get_no_entity_field_exception() {
        Map<String, Object> batchResultData = ImmutableMap.of(
                "keyField","mockKeyResult",
                "__typename","TestEntityType"
        );

        DataFetcherResult<Map<String, Object>> providerResultData = DataFetcherResult.newResult()
                .data(batchResultData)
                .errors(Collections.EMPTY_LIST)
                .build();

        DataFetchingEnvironment keyFieldDFEMock = mock(DataFetchingEnvironment.class);

        List<DataFetchingEnvironment> dfeList = Arrays.asList(keyFieldDFEMock);

        try {
            specUnderTest.toBatchResult(providerResultData, dfeList);
            Assert.fail("Exception should be thrown due to no _entities field");
        } catch (RuntimeException ex) {
            if(ex instanceof EntityFetchingException) {
                assertThat(((EntityFetchingException) ex).getErrorType()).isEqualTo(ErrorType.DataFetchingException);
                assertThat(ex.getMessage()).contains(NO_ENTITY_FIELD);
            } else {
                throw ex;            }
        }
    }

    @Test
    public void get_successful_single_entity_result() {
        List<Map<String, Object>> _entityData = Arrays.asList(
            ImmutableMap.of(
            "ExtEntityField","entityFieldResult",
            "__typename","TestEntityType"
            )
        );

        Map<String, Object> providerResultData = ImmutableMap.of(
                "_entities", _entityData
        );

        DataFetcherResult<Map<String, Object>> providerResult = DataFetcherResult.<Map<String, Object>>newResult()
                .data(providerResultData)
                .build();

        DataFetchingEnvironment keyFieldDFEMock = mock(DataFetchingEnvironment.class);

        List<DataFetchingEnvironment> dfeList = Arrays.asList(keyFieldDFEMock);

        List<DataFetcherResult<Object>> batchResult = specUnderTest.toBatchResult(providerResult, dfeList);

        assertThat(batchResult.size()).isEqualTo(1);
        assertThat(batchResult.get(0).getData()).isEqualTo("entityFieldResult");
    }

    @Test
    public void get_successful_multiple_entity_results() {
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
        );

        Map<String, Object> providerResultData = ImmutableMap.of(
                "_entities", _entityData
        );

        DataFetcherResult<Map<String, Object>> providerResult = DataFetcherResult.<Map<String, Object>>newResult()
                .data(providerResultData)
                .build();

        DataFetchingEnvironment keyFieldDFEMock = mock(DataFetchingEnvironment.class);

        List<DataFetchingEnvironment> dfeList = Arrays.asList(keyFieldDFEMock);

        List<DataFetcherResult<Object>> batchResult = specUnderTest.toBatchResult(providerResult, dfeList);

        assertThat(batchResult.size()).isEqualTo(3);
        assertThat(batchResult.get(0).getData()).isEqualTo("entityFieldResult1");
        assertThat(batchResult.get(1).getData()).isEqualTo("entityFieldResult2");
        assertThat(batchResult.get(2).getData()).isEqualTo("entityFieldResult3");
    }
}
