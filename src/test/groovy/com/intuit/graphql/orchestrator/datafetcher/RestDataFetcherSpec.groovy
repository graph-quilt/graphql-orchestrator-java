package com.intuit.graphql.orchestrator.datafetcher

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.batch.QueryExecutor
import com.intuit.graphql.orchestrator.schema.ServiceMetadata
import com.intuit.graphql.orchestrator.schema.ServiceMetadataImpl
import graphql.GraphQLContext
import graphql.execution.DataFetcherResult
import graphql.execution.MergedField
import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.language.OperationDefinition.Operation
import graphql.language.SelectionSet
import graphql.schema.DataFetchingEnvironment
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment

class RestDataFetcherSpec extends Specification {

    private DataFetchingEnvironment dataFetchingEnvironment

    def setup() {
        Map<String, Object> variables = ImmutableMap.of(
                "key1", "val1",
                "key2", "val2"
        )

        SelectionSet selectionSet = SelectionSet.newSelectionSet()
                .selection(Field.newField("subField1").build())
                .selection(Field.newField("subField2").build()).build()

        Field topLevelField = Field.newField("topLevelField").selectionSet(selectionSet).build()

        OperationDefinition operationDefinition = OperationDefinition.newOperationDefinition()
                .name("TestRestQuery")
                .operation(Operation.QUERY)
                .selectionSet(selectionSet)
                .build()

        dataFetchingEnvironment = createTestDataFetchingEnvironment(
                operationDefinition,
                MergedField.newMergedField(topLevelField)
                        .build(),
                variables
        )
    }

    @SuppressWarnings("unchecked")
    def "can Execute Request"() {
        given:
        QueryExecutor queryExecutor = { executionInput, context ->
            assert executionInput != null
            // assertThat(executionInput.getQuery()).isNotNull()
            assert executionInput.getOperationName() != null
            assert executionInput.getVariables() != null

            Document document = context.get(Document.class)
            DataFetchingEnvironment dfe = context.get(DataFetchingEnvironment.class)

            assert document != null
            assert dfe != null
            assert dfe == dataFetchingEnvironment

            Map<String, Object> responseMap = ImmutableMap.of("data", ImmutableMap.of(
                    "topLevelField", ImmutableMap.of(
                    "subField1", "stringVal1",
                    "subField2", "stringVal2"
                ))
            )

            return CompletableFuture.completedFuture(responseMap)
        }

        TestServiceProvider testServiceProvider = TestServiceProvider.newBuilder().namespace("TEST")
                .queryFunction(queryExecutor)
                .serviceType(ServiceType.REST).build()
        ServiceMetadata serviceMetadata = Mock(ServiceMetadataImpl.class)
        serviceMetadata.getServiceProvider() >> testServiceProvider

        RestDataFetcher restDataFetcher = new RestDataFetcher(serviceMetadata)

        when:
        ((CompletableFuture<DataFetcherResult<Map<String, Object>>>) restDataFetcher.get(dataFetchingEnvironment))
                .whenComplete({ dataFetcherResult, throwable ->
                    assert throwable == null
                    assert dataFetcherResult != null
                    Map<String, Object> data = (Map<String, Object>) dataFetcherResult.getData()
                    assert data.containsOnlyKeys("data")
                    assert ((Map<String, Object>) data.get("topLevelField")).containsOnlyKeys("subField1", "subField2")
                })

        then:
        noExceptionThrown()
    }

    // TODO see if needed
    // public void canExecuteRequestWithEmptySelectionSet() {
    // }

    private DataFetchingEnvironment createTestDataFetchingEnvironment(
            OperationDefinition opDef, MergedField field, Map<String, Object> variables) {
        return newDataFetchingEnvironment()
                .context(GraphQLContext.newContext().build())
                .mergedField(field)
                .operationDefinition(opDef)
                .variables(variables)
                .build()
    }
}