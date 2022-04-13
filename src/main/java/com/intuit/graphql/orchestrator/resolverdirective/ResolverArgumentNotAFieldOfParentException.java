package com.intuit.graphql.orchestrator.resolverdirective;

import static java.lang.String.format;

public class ResolverArgumentNotAFieldOfParentException extends ResolverDirectiveException {

  private static final String ERR_MSG = "Resolver argument value %s should be a reference "
      + "to a field in Parent Type %s";

  private static final String ERR_MSG2 = "'%s' is not a field of parent type. "
          + "serviceName=%s, "
          + "parentTypeName=%s, "
          + "fieldName=%s, ";

  public ResolverArgumentNotAFieldOfParentException(String resolverArgValue, String parentTypeName) {
    super(format(ERR_MSG, resolverArgValue, parentTypeName));
  }

  public ResolverArgumentNotAFieldOfParentException(String reqdFieldName, String serviceName,
                                                    String parentTypeName, String fieldName) {
    super(format(ERR_MSG2, reqdFieldName, serviceName, parentTypeName, fieldName));
  }
}
