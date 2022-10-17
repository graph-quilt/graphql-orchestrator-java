package com.intuit.graphql.orchestrator.stitching

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.batch.BatchLoaderExecutionHooks
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
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

    def "test Builder Service with non-null service"() {
        given:
        ServiceProvider serviceProvider = TestServiceProvider.newBuilder().build()
        when:
        sfBuilder.service(serviceProvider)
        then:
        noExceptionThrown()
    }

    def "test Builder Services"() {
        when:
        sfBuilder.services(null)

        then:
        thrown(NullPointerException)
    }

    def "test Builder Services with non-null service"() {
        given:
        ServiceProvider serviceProvider = TestServiceProvider.newBuilder().build()
        when:
        sfBuilder.services(List.of(serviceProvider))
        then:
        noExceptionThrown()
    }

    def "test Builder Batch Loader Hooks"() {
        when:
        sfBuilder.batchLoaderHooks(null)

        then:
        thrown(NullPointerException)
    }

    def "test Builder Batch Loader Hooks with non-null Batch Loader Hooks"() {
        when:
        BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> mockBatchLoaderHook =
                Mock(BatchLoaderExecutionHooks.class)
        sfBuilder.batchLoaderHooks(mockBatchLoaderHook)

        then:
        noExceptionThrown()
    }
}
