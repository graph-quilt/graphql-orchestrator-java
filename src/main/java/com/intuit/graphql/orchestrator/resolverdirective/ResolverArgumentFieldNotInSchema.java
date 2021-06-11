package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.xtext.FieldContext;

/**
 * Exception will occur if the requested argument input type (at any recursive level in the input type) does not exist
 * in the schema.
 *
 * Example:
 *
 * <pre>
 *   {@code
 *
 *    input MyInputType {
 *      a: Int
 *      b: Int
 *      c: NestedType
 *    }
 *
 *    input NestedType {
 *      d: Int
 *    }
 *
 *    ...
 *    field(arg1: MyInputType @resolver(field: "root.child.leaf")): String
 *
 *   }
 * </pre>
 *
 * For the above {@code MyInputType} to be valid, ALL fields found in MyInputType found in "root.child.leaf" should
 * exist in the schema.
 */
public class ResolverArgumentFieldNotInSchema extends ResolverDirectiveException {

  private static final String MSG = "Resolver argument '%s' in '%s': field '%s' in InputType '%s' does not exist in schema.";

  public ResolverArgumentFieldNotInSchema(final String argumentName, final FieldContext rootContext,
      final FieldContext fieldContext) {
    super(String
        .format(MSG, argumentName, rootContext.toString(), fieldContext.getFieldName(), fieldContext.getParentType()));
  }
}
