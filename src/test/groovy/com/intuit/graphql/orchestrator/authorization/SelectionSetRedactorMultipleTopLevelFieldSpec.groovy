package com.intuit.graphql.orchestrator.authorization

import spock.lang.Specification

class SelectionSetRedactorMultipleTopLevelFieldSpec extends Specification {
    // There won't be a case for multiple top-level fields for SelectionSetRedactor.
    // In GraphQLServiceBatchLoader,  every top-level comes as a separate DataFetchingEnvironment and
    // SelectionSetRedactor only process per DataFetchingEnvironment
}
