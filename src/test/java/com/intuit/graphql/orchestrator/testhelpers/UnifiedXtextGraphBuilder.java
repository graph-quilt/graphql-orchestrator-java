package com.intuit.graphql.orchestrator.testhelpers;

import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder;
import org.eclipse.xtext.resource.XtextResourceSet;

import java.util.EnumMap;
import java.util.Map;

public class UnifiedXtextGraphBuilder {

    public static UnifiedXtextGraph build(ServiceProvider serviceProvider) {
        XtextResourceSet xtextResourceSet = XtextResourceSetBuilder.newBuilder()
            .files(serviceProvider.sdlFiles())
            .isFederatedResourceSet(serviceProvider.isFederationProvider())
            .build();

        final Map<Operation, ObjectTypeDefinition> operationMap = new EnumMap<>(Operation.class);
        for (Operation operation : Operation.values()) {
            XtextUtils.findOperationType(operation, xtextResourceSet)
                .ifPresent(operationType -> operationMap.put(operation, operationType));
        }

        return UnifiedXtextGraph.newBuilder()
            .operationMap(operationMap).build();
    }

}
