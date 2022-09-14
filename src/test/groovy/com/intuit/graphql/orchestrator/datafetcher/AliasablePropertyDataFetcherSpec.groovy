package com.intuit.graphql.orchestrator.datafetcher

import graphql.execution.MergedField
import graphql.language.Field
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingEnvironmentImpl
import spock.lang.Specification

class AliasablePropertyDataFetcherSpec extends Specification {

    private static final MergedField aliasField = MergedField.newMergedField()
            .addField(Field.newField().name("original").alias("alias").build()).build()

    private Map<String, Object> data

    private DataFetchingEnvironmentImpl.Builder dfeBuilder

    def setup() {
        this.data = new HashMap<>()
        dfeBuilder = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
    }

    def "gets Correct Alias Value From Map"() {
        given:
        data.put("alias", "data")
        final DataFetchingEnvironment dataFetchingEnvironment = dfeBuilder
                .source(data)
                .mergedField(aliasField)
                .build()

        when:
        final AliasablePropertyDataFetcher aliasablePropertyDataFetcher = new AliasablePropertyDataFetcher("original")

        then:
        aliasablePropertyDataFetcher.get(dataFetchingEnvironment) == "data"
    }

    def "gets Original Value With Field"() {
        given:
        data.put("original", "data")
        final MergedField fieldWithoutAlias = MergedField.newMergedField()
                .addField(Field.newField().name("original").build()).build()

        final DataFetchingEnvironment dataFetchingEnvironment = dfeBuilder.source(data)
                .mergedField(fieldWithoutAlias).build()

        when:
        final AliasablePropertyDataFetcher aliasablePropertyDataFetcher = new AliasablePropertyDataFetcher("original")

        then:
        aliasablePropertyDataFetcher.get(dataFetchingEnvironment) == "data"
    }

    def "delegates To Default Data Fetcher"() {
        given:
        final DataFetchingEnvironment dataFetchingEnvironment = dfeBuilder.source(new Original())
                .build()

        when:
        final AliasablePropertyDataFetcher aliasablePropertyDataFetcher = new AliasablePropertyDataFetcher("original")

        then:
        aliasablePropertyDataFetcher.get(dataFetchingEnvironment) == "original"
    }

    private static class Original {
        String getOriginal() {
            return "original"
        }
    }
}
