package com.intuit.graphql.orchestrator.resolverdirective;

public class ResolverArgumentNotAFieldOfParentException extends ResolverDirectiveException {

  private static final String ERR_MSG = "Resolver argument value %s should be a reference "
      + "to a field in Parent Type %s";

  public ResolverArgumentNotAFieldOfParentException(String resolverArgValue, String parentTypeName) {
    super(String.format(ERR_MSG, resolverArgValue, parentTypeName));
  }
}
