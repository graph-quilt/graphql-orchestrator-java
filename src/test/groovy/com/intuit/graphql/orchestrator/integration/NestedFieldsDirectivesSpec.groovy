package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.ServiceA
import com.intuit.graphql.orchestrator.ServiceC
import com.intuit.graphql.orchestrator.TestCase
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException
import helpers.BaseIntegrationTestSpecification

class NestedFieldsDirectivesSpec extends BaseIntegrationTestSpecification {

    void cannotBuildDueToQueryNestedFieldsHasMismatchedDirectives() {
        when:
        TestCase.newTestCase()
                .service(new ServiceA())
                .service(new ServiceC())
                .build()

        then:
        thrown(FieldMergeException)
    }

}
