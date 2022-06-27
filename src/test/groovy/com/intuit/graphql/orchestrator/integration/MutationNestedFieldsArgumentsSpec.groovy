package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.ServiceD
import com.intuit.graphql.orchestrator.ServiceE
import com.intuit.graphql.orchestrator.TestCase
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException
import helpers.BaseIntegrationTestSpecification

class MutationNestedFieldsArgumentsSpec extends BaseIntegrationTestSpecification {

    void cannotBuildDueToMutationNestedFieldsHasMismatchedArguments() {
        when:
        TestCase.newTestCase()
                .service(new ServiceD())
                .service(new ServiceE())
                .build()

        then:
        thrown(FieldMergeException)
    }
}
