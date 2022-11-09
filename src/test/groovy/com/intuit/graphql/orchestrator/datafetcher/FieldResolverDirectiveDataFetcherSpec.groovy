package com.intuit.graphql.orchestrator.datafetcher


import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext
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
        DataFetcherContext.DataFetcherType actualDataFetcherType = dataFetcher.getDataFetcherType()

        then:
        actualDataFetcherType == DataFetcherContext.DataFetcherType.RESOLVER_ON_FIELD_DEFINITION
    }

}