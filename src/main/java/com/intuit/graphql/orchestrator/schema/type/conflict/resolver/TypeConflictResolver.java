package com.intuit.graphql.orchestrator.schema.type.conflict.resolver;

import com.intuit.graphql.graphQL.TypeDefinition;
import graphql.schema.GraphQLType;

public interface TypeConflictResolver<T extends TypeDefinition> {

  /**
   * Resolves conflict
   *
   * @param conflictingType the new type ot resolve
   * @param existingType existingType in the schema
   * @return ResolvedType
   * @throws TypeConflictException if cannot resolve conflict.
   */
  GraphQLType resolve(T conflictingType, GraphQLType existingType) throws TypeConflictException;

}
