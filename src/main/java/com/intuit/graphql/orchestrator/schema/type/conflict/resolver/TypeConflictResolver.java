package com.intuit.graphql.orchestrator.schema.type.conflict.resolver;

import com.intuit.graphql.graphQL.TypeDefinition;
import graphql.schema.GraphQLType;

public interface TypeConflictResolver<T extends TypeDefinition> {

  /***
   *
   * Returns the GraphQLType after conflict resolution, throws exception if it
   * cannot resolve the conflict.
   *
   * @param conflictingType
   * @param existingType
   * @return ResolvedType
   */
  GraphQLType resolve(T conflictingType, GraphQLType existingType) throws TypeConflictException;

}
