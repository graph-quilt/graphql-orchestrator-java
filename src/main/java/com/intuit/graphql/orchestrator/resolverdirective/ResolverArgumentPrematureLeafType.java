package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.xtext.FieldContext;

/**
 * Exception will occur when parsing the field in a resolver directive field and a ScalarTypeDefinition or
 * EnumTypeDefinition is found in any level that is not the leaf node. Scalar types and Enum types are considered
 * terminal types and do not have any object nodes underneath them.
 *
 * <p>
 * <b>Example</b>
 * </p>
 * <pre>
 *   {@code @resolver(field: "a.b.c")}
 * </pre>
 *
 * An exception should be thrown if a ScalarTypeDefinition or EnumTypeDefinition is found in "a", or "b".
 */
public class ResolverArgumentPrematureLeafType extends ResolverDirectiveException {

  private static final String MSG = "Resolver argument '%s' in '%s': Premature %s found in field '%s'.";

  public ResolverArgumentPrematureLeafType(final String argumentName, final String typeName,
      final FieldContext rootContext,
      String field) {
    super(String.format(MSG, argumentName, rootContext.toString(), typeName, field));
  }
}
