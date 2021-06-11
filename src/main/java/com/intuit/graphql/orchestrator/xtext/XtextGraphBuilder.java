package com.intuit.graphql.orchestrator.xtext;

import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import java.util.EnumMap;
import java.util.Map;
import org.eclipse.xtext.resource.XtextResourceSet;

public class XtextGraphBuilder {

  public static XtextGraph build(ServiceProvider serviceProvider) {

    XtextResourceSet xtextResourceSet = XtextResourceSetBuilder.newBuilder()
        .files(serviceProvider.sdlFiles())
        .build();

    final Map<Operation, ObjectTypeDefinition> operationMap = new EnumMap<>(Operation.class);
    for (Operation operation : Operation.values()) {
      XtextUtils.findOperationType(operation, xtextResourceSet)
          .ifPresent(operationType -> operationMap.put(operation, operationType));
    }

    return XtextGraph.newBuilder().xtextResourceSet(xtextResourceSet)
        .serviceProvider(serviceProvider)
        .operationMap(operationMap).build();
  }

}
