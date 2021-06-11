package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.xtext.FieldContext;

/**
 * Exception will occur when requested argument type does not match with type in schema.
 *
 * <pre>
 *   {@code
 *     field(arg: SomeInputType @resolver(field: "a.b.c"))
 *
 *     ...
 *     type BType {
 *       c: String # type in schema is String, but requested type is ObjectType!
 *     }
 *   }
 * </pre>
 */
public class ResolverArgumentTypeMismatch extends ResolverDirectiveException {

  private static final String MSG = "Resolver argument '%s' in '%s': Expected type '%s' to be '%s'.";

  private static final String MSG_WITH_PARENT_CONTEXT = "Resolver argument '%s' in '%s': Expected type '%s' in '%s' to be '%s'.";

  public ResolverArgumentTypeMismatch(final String argumentName, final FieldContext rootContext,
      final String inputTypeName, final String expectedTypeName) {
    super(String.format(MSG, argumentName, rootContext.toString(), inputTypeName,
        expectedTypeName));
  }

  public ResolverArgumentTypeMismatch(final String argumentName, final FieldContext rootContext,
      final FieldContext parentContext, final String inputTypeName, final String expectedTypeName) {
    super(String
        .format(MSG_WITH_PARENT_CONTEXT, argumentName, rootContext.toString(),
            inputTypeName, parentContext.toString(), expectedTypeName));
  }

  public static ResolverArgumentTypeMismatch create(final String argumentName, final FieldContext rootContext,
      final FieldContext parentContext, final String inputTypeName, final String expectedTypeName) {
    if (parentContext == null) {
      return new ResolverArgumentTypeMismatch(argumentName, rootContext, inputTypeName, expectedTypeName);
    }

    return new ResolverArgumentTypeMismatch(argumentName, rootContext, parentContext, inputTypeName, expectedTypeName);
  }
}
