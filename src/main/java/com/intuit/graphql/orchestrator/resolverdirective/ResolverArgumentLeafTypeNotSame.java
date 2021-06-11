package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.xtext.FieldContext;

/**
 * Exception will occur when either ScalarTypes are not referring to the same scalar type name.
 *
 * <pre>
 *  {@code
 *    field(arg: Int @resolver(field: "a.b.c"))
 *
 *    ...type in schema
 *
 *    type BType {
 *      c: String # type mismatch!
 *    }
 *
 *  }
 * </pre>
 * For example, an exception should be thrown if the requested type is {@code Int} and the type in the schema is a
 * {@code String}.
 */
public class ResolverArgumentLeafTypeNotSame extends ResolverDirectiveException {

  private static final String MSG = "Resolver argument '%s' in '%s': Expected '%s' to be '%s'.";

  private static final String MSG_WITH_PARENT_CONTEXT = "Resolver argument '%s' in '%s': Expected '%s' in '%s' to be '%s'.";

  public ResolverArgumentLeafTypeNotSame(final String argumentName, FieldContext fieldContext,
      String foundScalarType, String expectedScalarType) {
    super(String.format(MSG, argumentName, fieldContext.toString(), foundScalarType,
        expectedScalarType));
  }

  public ResolverArgumentLeafTypeNotSame(final String argumentName, FieldContext fieldContext,
      FieldContext inputTypeParentContext, String foundType, String expectedType) {
    super(String.format(MSG_WITH_PARENT_CONTEXT, argumentName, fieldContext.toString(), foundType,
        inputTypeParentContext.toString(), expectedType));
  }

  public static ResolverArgumentLeafTypeNotSame create(final String argumentName, FieldContext fieldContext,
      FieldContext inputTypeParentContext, String foundType, String expectedType) {
    if (inputTypeParentContext == null) {
      return new ResolverArgumentLeafTypeNotSame(argumentName, fieldContext, foundType, expectedType);
    }

    return new ResolverArgumentLeafTypeNotSame(argumentName, fieldContext, inputTypeParentContext, foundType,
        expectedType);
  }
}
