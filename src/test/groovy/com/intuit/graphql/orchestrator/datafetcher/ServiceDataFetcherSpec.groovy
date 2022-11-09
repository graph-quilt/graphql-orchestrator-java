package com.intuit.graphql.orchestrator.datafetcher

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.schema.ServiceMetadata
import com.intuit.graphql.orchestrator.schema.ServiceMetadataImpl
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext
import graphql.GraphQLContext
import graphql.execution.MergedField
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingEnvironmentImpl
import org.dataloader.BatchLoader
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static graphql.execution.MergedField.newMergedField
import static graphql.language.Field.newField

class ServiceDataFetcherSpec extends Specification {

    def "gets Data From Data Loader"() {
        TestServiceProvider testServiceProviderForQuery = TestServiceProvider.newBuilder().namespace("Query.topLevelField")
                .serviceType(ServiceProvider.ServiceType.REST).build()

        TestServiceProvider testServiceProviderForMutation = TestServiceProvider.newBuilder().namespace("Mutation.topLevelField")
                .serviceType(ServiceProvider.ServiceType.REST).build()

        ServiceMetadata serviceMetadataForQuery = Mock(ServiceMetadataImpl.class)
        serviceMetadataForQuery.getServiceProvider() >> testServiceProviderForQuery

        ServiceMetadata serviceMetadataForMutation = Mock(ServiceMetadataImpl.class)
        serviceMetadataForMutation.getServiceProvider() >> testServiceProviderForMutation

        given:
        final ServiceDataFetcher queryFetcher = new ServiceDataFetcher(serviceMetadataForQuery)
        final ServiceDataFetcher mutationFetcher = new ServiceDataFetcher(serviceMetadataForMutation)

        final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry()
        final BatchLoader<Object, Object> batchLoaderQuery = { a ->
            CompletableFuture
                    .completedFuture(Arrays.asList("qresult"))
        }
        final BatchLoader<Object, Object> batchLoaderMutation = { a ->
            CompletableFuture
                    .completedFuture(Arrays.asList("mresult"))
        }
        dataLoaderRegistry
                .register("Query.topLevelField", DataLoader.newDataLoader(batchLoaderQuery))
                .register("Mutation.topLevelField", DataLoader.newDataLoader(batchLoaderMutation))
        MergedField mergedField = newMergedField(newField("first").build()).build()

        DataFetchingEnvironment dataFetchingEnvironment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .dataLoaderRegistry(dataLoaderRegistry)
                .mergedField(mergedField)
                .context(GraphQLContext.newContext().build())
                .build()

        final Object qresult = queryFetcher.get(dataFetchingEnvironment)
        final Object mresult = mutationFetcher.get(dataFetchingEnvironment)

        when:
        dataLoaderRegistry.dispatchAll()
        then:
        ((CompletableFuture) qresult).getNow("fail") == "qresult"

        ((CompletableFuture) mresult).getNow("fail") == "mresult"
    }

    def "returns correct namespace"() {
        given:
        ServiceProvider serviceProvider = Mock(ServiceProvider.class)
        serviceProvider.getNameSpace() >> "TestNamespace"

        ServiceMetadata serviceMetadata = Mock(ServiceMetadataImpl.class)
        serviceMetadata.getServiceProvider() >> serviceProvider

        ServiceDataFetcher serviceDataFetcher = new ServiceDataFetcher(serviceMetadata)

        when:
        String actualNamespace = serviceDataFetcher.getNamespace()

        then:
        actualNamespace == "TestNamespace"
    }

    def "returns correct DataFetcherType"() {
        given:
        ServiceMetadata serviceMetadata = Mock(ServiceMetadataImpl.class)

        ServiceDataFetcher serviceDataFetcher = new ServiceDataFetcher(serviceMetadata)

        when:
        DataFetcherContext.DataFetcherType actualDataFetcherType = serviceDataFetcher.getDataFetcherType()

        then:
        actualDataFetcherType == DataFetcherContext.DataFetcherType.SERVICE
    }

}
