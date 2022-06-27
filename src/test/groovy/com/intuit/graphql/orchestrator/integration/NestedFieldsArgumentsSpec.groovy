package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.ServiceA
import com.intuit.graphql.orchestrator.ServiceB
import com.intuit.graphql.orchestrator.TestCase
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException
import helpers.BaseIntegrationTestSpecification

class NestedFieldsArgumentsSpec extends BaseIntegrationTestSpecification {

    void cannotBuildDueToQueryNestedFieldsHasMismatchedArguments() {
        when:
        TestCase.newTestCase()
                .service(new ServiceA())
                .service(new ServiceB())
                .build()

        then:
        thrown(FieldMergeException)
    }

}
