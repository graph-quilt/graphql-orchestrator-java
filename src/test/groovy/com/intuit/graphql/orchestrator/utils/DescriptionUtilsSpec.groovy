package com.intuit.graphql.orchestrator.utils

import spock.lang.Specification

import static com.intuit.graphql.orchestrator.utils.DescriptionUtils.attachNamespace
import static com.intuit.graphql.orchestrator.utils.DescriptionUtils.mergeDescriptions

class DescriptionUtilsSpec extends Specification {

    def "attach Name Space Test"(){
        expect:
        attachNamespace("ns","desc") == "[ns] desc"
        attachNamespace("ns",null) == "[ns]"
    }

    def "merge Descrtiption Test"(){
        given:
        String firstMerge = mergeDescriptions("[ns1] desc1", "[ns2] desc2")

        expect:
        firstMerge == "[ns1,ns2] desc1, desc2"
        mergeDescriptions(firstMerge, firstMerge) == "[ns1,ns2,ns1,ns2] desc1, desc2, desc1, desc2"
        mergeDescriptions(firstMerge, "[ns3] desc3") == "[ns1,ns2,ns3] desc1, desc2, desc3"
        mergeDescriptions(firstMerge, "[ns3]") == "[ns1,ns2,ns3] desc1, desc2"
        mergeDescriptions(firstMerge, "") == firstMerge
    }
}
