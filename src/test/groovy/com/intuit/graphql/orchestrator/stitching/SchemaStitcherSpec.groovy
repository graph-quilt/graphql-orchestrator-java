package com.intuit.graphql.orchestrator.stitching

import spock.lang.Specification

class SchemaStitcherSpec extends Specification {

    SchemaStitcher.Builder sfBuilder

    def setup() {
        sfBuilder = SchemaStitcher.newBuilder()
    }

    def "test Builder Service"() {
        when:
        sfBuilder.service(null)

        then:
        thrown(NullPointerException)
    }

    def "test Builder Services"() {
        when:
        sfBuilder.services(null)

        then:
        thrown(NullPointerException)
    }

    def "test Builder Batch Loader Hooks"() {
        when:
        sfBuilder.batchLoaderHooks(null)

        then:
        thrown(NullPointerException)
    }
}
