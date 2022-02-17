package com.intuit.graphql.orchestrator.federation.extendsdirective;

import com.intuit.graphql.graphQL.TypeDefinition;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EntityExtension {
  private TypeDefinition baseType;
  private TypeDefinition typeExtension;
}
