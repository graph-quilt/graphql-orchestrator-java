package com.intuit.graphql.orchestrator.integration

import helpers.BaseIntegrationTestSpecification

class DownstreamRestErrorMappingSpec extends BaseIntegrationTestSpecification {

    //Default Batch Result Transformer

    //single
    def "200 with errors with path (single result)"(){}
    def "200 with errors with path and partial data (single result)"(){}
    def "200 with error with no path (single result)"(){}
    def "200 with error with no path and partial data (single result)"(){}

    //batch
    def "200 with errors with path (batch result)"(){}
    def "200 with errors with path and partial data (batch result)"(){}
    def "200 with error with no path (batch result)"(){}
    def "200 with error with no path and partial data (batch result)"(){}

}
