package com.intuit.graphql.orchestrator.xtext;

import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.utils.XtextUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.xtext.resource.XtextResourceSet;

@Slf4j
public class XtextGraphBuilder {

  public static XtextGraph build(ServiceProvider serviceProvider) {

    if(serviceProvider.getSeviceType() == ServiceProvider.ServiceType.FEDERATION) {
      try {
        String federation_directives = IOUtils.toString(Files.newInputStream(Paths.get("src/main/resources/federation_built_in_directives.graphqls")));
        for (Map.Entry<String, String> entry : serviceProvider.sdlFiles().entrySet() ) {
          serviceProvider.sdlFiles().put(entry.getKey(), federation_directives + entry.getValue());
        }
      } catch (IOException ex) {
        log.error("Failed to read resource");
      }
    }

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
