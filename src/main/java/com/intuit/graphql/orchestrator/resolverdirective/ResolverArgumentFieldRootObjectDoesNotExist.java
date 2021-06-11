package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.xtext.FieldContext;

/**
 * Exception will occur if a any sub-path in a resolve argument field does not exist in the schema (starting from the
 * query root).
 *
 * Example:
 *
 * {@code @resolver(field: "a.b.c")}
 *
 * In "a.b.c", "a" must exist as a FieldDefinition under query root, "b" must exist as a FieldDefinition under the
 * ObjectTypeDefinition found for "a", and "c" must exist as a FieldDefinition under the ObjectTypeDefinition found for
 * "b".
 */
public class ResolverArgumentFieldRootObjectDoesNotExist extends ResolverDirectiveException {

  private static final String MSG = "Resolver argument '%s' in '%s': field '%s' does not exist in schema.";

  public ResolverArgumentFieldRootObjectDoesNotExist(final String argumentName, final FieldContext rootContext,
      final String fieldName) {
    super(String.format(MSG, argumentName, rootContext.toString(), fieldName));
  }
}
