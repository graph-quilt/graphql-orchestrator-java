package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import lombok.Builder;
import lombok.Getter;

/**
 * This class holds data about a fieldDefinition with resolver directive.
 *
 * If a parent type has two child fields and both has resolver directive, then each child field
 * shall have a corresponding instance of this class.
 */
@Builder
@Getter
public class FieldResolverContext {

  private FieldContext fieldContext;
  private boolean requiresTypeNameInjection;
  private FieldDefinition fieldDefinition;
  private TypeDefinition parentTypeDefinition;
  private String serviceNamespace;
  private ResolverDirectiveDefinition resolverDirectiveDefinition;

}
