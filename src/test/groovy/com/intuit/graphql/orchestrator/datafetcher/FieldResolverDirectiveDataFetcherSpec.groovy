package com.intuit.graphql.orchestrator.datafetcher

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext
import spock.lang.Specification

class FieldResolverDirectiveDataFetcherSpec extends Specification {

    FieldResolverDirectiveDataFetcher dataFetcher

    def setup() {
        FieldResolverContext fieldResolverContext = Mock(FieldResolverContext.class)
        dataFetcher = new FieldResolverDirectiveDataFetcher(fieldResolverContext, "TestNamespace", ServiceProvider.ServiceType.GRAPHQL)
    }

    def "returns correct namespace"() {
        when:
        String actualNamespace = dataFetcher.getNamespace()

        then:
        actualNamespace == "TestNamespace"
    }

    def "returns correct DataFetcherType"() {
        when:
        DataFetcherContext.DataFetcherType actualDataFetcherType = dataFetcher.getDataFetcherType()

        then:
        actualDataFetcherType == DataFetcherContext.DataFetcherType.RESOLVER_ON_FIELD_DEFINITION
    }

    def "returns correct ServiceType"() {
        when:
        ServiceProvider.ServiceType actualServiceType = dataFetcher.getServiceType()

        then:
        actualServiceType == ServiceProvider.ServiceType.GRAPHQL
    }

}