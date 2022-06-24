package com.intuit.graphql.orchestrator.stitching

import helpers.BaseIntegrationTestSpecification

class SchemaStitcherSpec extends BaseIntegrationTestSpecification {

    SchemaStitcher.Builder sfBuilder

    void setup() {
        sfBuilder = SchemaStitcher.newBuilder()
    }

    void testBuilderService() {
        when:
        sfBuilder.service(null)

        then:
        thrown(NullPointerException)
    }

    void testBuilderServices() {
        when:
        sfBuilder.services(null)

        then:
        thrown(NullPointerException)
    }

    void testBuilderBatchLoaderHooks() {
        when:
        sfBuilder.batchLoaderHooks(null)

        then:
        thrown(NullPointerException)
    }
}
