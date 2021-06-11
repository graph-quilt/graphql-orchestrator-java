package com.intuit.graphql.orchestrator.resolverdirective;

public class NotAValidFieldReference extends ResolverDirectiveException {

  private static final String ERR_INVALID_FIELD_REF_FORMAT = "'%s' is not a valid field reference.";

  public NotAValidFieldReference(String resolverArgValue) {
    super(String.format(ERR_INVALID_FIELD_REF_FORMAT, resolverArgValue));
  }
}
