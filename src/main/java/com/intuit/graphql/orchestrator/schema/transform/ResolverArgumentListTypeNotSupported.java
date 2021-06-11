package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveException;
import com.intuit.graphql.orchestrator.xtext.FieldContext;

public class ResolverArgumentListTypeNotSupported extends ResolverDirectiveException {

  private static final String MSG = "Resolver argument '%s' in '%s'. Field '%s' is a List type, which is not supported.";

  public ResolverArgumentListTypeNotSupported(String argumentName, FieldContext rootContext, String subField) {
    super(String.format(MSG, argumentName, rootContext.toString(), subField));
  }
}
