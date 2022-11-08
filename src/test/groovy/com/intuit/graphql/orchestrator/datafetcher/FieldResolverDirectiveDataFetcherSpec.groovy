package com.intuit.graphql.orchestrator.datafetcher


import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import spock.lang.Specification

class FieldResolverDirectiveDataFetcherSpec extends Specification {

    def "returns correct namespace"() {
        given:
        FieldResolverContext fieldResolverContext = Mock(FieldResolverContext.class)
        FieldResolverDirectiveDataFetcher dataFetcher = new FieldResolverDirectiveDataFetcher(fieldResolverContext, "TestNamespace")

        when:
        String actualNamespace = dataFetcher.getNamespace()

        then:
        actualNamespace == "TestNamespace"
    }

    def "returns correct DataFetcherType"() {
        given:
       FieldResolverContext fieldResolverContext = Mock(FieldResolverContext.class)
        FieldResolverDirectiveDataFetcher dataFetcher = new FieldResolverDirectiveDataFetcher(fieldResolverContext, "TestNamespace")

        when:
        DataFetcherType actualDataFetcherType = dataFetcher.getDataFetcherType()

        then:
        actualDataFetcherType == DataFetcherType.FIELD_RESOLVER_DATA_FETCHER
    }

}