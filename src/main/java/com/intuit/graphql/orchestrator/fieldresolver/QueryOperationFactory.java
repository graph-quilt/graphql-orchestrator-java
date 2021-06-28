package com.intuit.graphql.orchestrator.fieldresolver;

import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import graphql.language.SelectionSet;

import java.util.function.Supplier;

public class QueryOperationFactory {

    public OperationDefinition create(String operationName, Supplier<SelectionSet> selectionSetSupplier) {
        return OperationDefinition.newOperationDefinition()
                .name(operationName)
                .selectionSet(selectionSetSupplier.get())
                .operation(Operation.QUERY)
                .build();

    }

}

