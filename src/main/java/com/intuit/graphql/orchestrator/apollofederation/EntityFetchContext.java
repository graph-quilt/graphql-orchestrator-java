package com.intuit.graphql.orchestrator.apollofederation;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;

public class EntityFetchContext extends SubGraphContext {


  // keys

  // Base Type

  public EntityFetchContext(FieldDefinition fieldDefinition,
      TypeDefinition parentTypeDefinition,
      boolean requiresTypeNameInjection,
      ServiceMetadata serviceMetadata) {
    super(fieldDefinition, parentTypeDefinition, requiresTypeNameInjection, serviceMetadata);
  }

}
