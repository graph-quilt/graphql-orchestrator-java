package com.intuit.graphql.orchestrator.stitching

import spock.lang.Specification

class SchemaStitcherSpec extends Specification {

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
