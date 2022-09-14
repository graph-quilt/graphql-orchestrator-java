package com.intuit.graphql.orchestrator.fieldresolver;

import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import graphql.language.SelectionSet;

public class QueryOperationFactory {

    public OperationDefinition create(String operationName, SelectionSet selectionSet) {
        return OperationDefinition.newOperationDefinition()
                .name(operationName)
                .selectionSet(selectionSet)
                .operation(Operation.QUERY)
                .build();

    }

}

