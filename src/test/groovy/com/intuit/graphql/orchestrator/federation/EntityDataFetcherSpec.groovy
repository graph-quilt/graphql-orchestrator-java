package com.intuit.graphql.orchestrator.federation

import com.intuit.graphql.orchestrator.datafetcher.DataFetcherType
import graphql.language.Field
import graphql.schema.DataFetchingEnvironment
import org.dataloader.DataLoader
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static com.intuit.graphql.orchestrator.batch.DataLoaderKeyUtil.createDataLoaderKey

class EntityDataFetcherSpec extends Specification {

    private final String entityName = "MockEntity"

    private final String namespace = "testNamespace"

    private DataFetchingEnvironment dataFetchingEnvironmentMock

    private DataLoader dataLoaderMock

    private EntityDataFetcher subjectUnderTest

    def setup() {
        dataFetchingEnvironmentMock = Mock(DataFetchingEnvironment)
        dataLoaderMock = Mock(DataLoader)

        subjectUnderTest =  new EntityDataFetcher(entityName, namespace)
    }

    def "returns correct namespace"() {
        when:
        String actualNamespace = subjectUnderTest.getNamespace()

        then:
        actualNamespace == namespace
    }

    def "returns correct DataFetcherType"() {
        when:
        DataFetcherType actualDataFetcherType = subjectUnderTest.getDataFetcherType()

        then:
        actualDataFetcherType == DataFetcherType.ENTITY_DATA_FETCHER
    }

    def "load entityBatchFetcher Success"() {
        given:
        String requestedEntityFieldName = "ExtEntityField"
        Field requestedEntityField = Field.newField().name(requestedEntityFieldName).alias("AliasedName").build()

        dataLoaderMock.load(dataFetchingEnvironmentMock) >> CompletableFuture.completedFuture("Success")
        dataFetchingEnvironmentMock.getField() >> requestedEntityField
        dataFetchingEnvironmentMock.getDataLoader( _ as String ) >> ({String invocationOnMock ->
            if (invocationOnMock.equals(createDataLoaderKey(entityName, requestedEntityFieldName))) {
                return dataLoaderMock
            }
            throw new RuntimeException("Failed to get the batch data loader; key has changed")
        })

        when:
        CompletableFuture<Object> result = subjectUnderTest.get(dataFetchingEnvironmentMock)

        then:
        result.isDone()
        result.get() == "Success"
    }
}
