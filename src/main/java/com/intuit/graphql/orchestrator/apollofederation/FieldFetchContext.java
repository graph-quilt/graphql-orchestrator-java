package com.intuit.graphql.orchestrator.apollofederation;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;

public class FieldFetchContext extends SubGraphContext {

  // keys

  // remote entities

  public FieldFetchContext(FieldDefinition fieldDefinition,
      TypeDefinition parentTypeDefinition,
      boolean requiresTypeNameInjection,
      ServiceMetadata serviceMetadata) {
    super(fieldDefinition, parentTypeDefinition, requiresTypeNameInjection, serviceMetadata);
  }

}
