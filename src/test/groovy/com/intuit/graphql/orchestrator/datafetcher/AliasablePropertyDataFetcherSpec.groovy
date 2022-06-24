package com.intuit.graphql.orchestrator.datafetcher

import graphql.execution.MergedField
import graphql.language.Field
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingEnvironmentImpl
import helpers.BaseIntegrationTestSpecification

class AliasablePropertyDataFetcherSpec extends BaseIntegrationTestSpecification {

    private static final MergedField aliasField = MergedField.newMergedField()
            .addField(Field.newField().name("original").alias("alias").build()).build()

    private Map<String, Object> data

    private DataFetchingEnvironmentImpl.Builder dfeBuilder

    void setup() {
        this.data = new HashMap<>()
        dfeBuilder = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
    }

    void getsCorrectAliasValueFromMap() {
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

    void getsOriginalValueWithField() {
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

    void delegatesToDefaultDataFetcher() {
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
