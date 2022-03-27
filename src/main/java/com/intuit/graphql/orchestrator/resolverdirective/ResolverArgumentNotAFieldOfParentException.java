package com.intuit.graphql.orchestrator.resolverdirective;

import static java.lang.String.format;

public class ResolverArgumentNotAFieldOfParentException extends ResolverDirectiveException {

  private static final String ERR_MSG = "'%s' is not a field of parent type. "
      + "serviceName=%s, "
      + "parentTypeName=%s, "
      + "fieldName=%s, ";

  public ResolverArgumentNotAFieldOfParentException(String reqdFieldName, String serviceName,
      String parentTypeName, String fieldName) {

    super(format(ERR_MSG, reqdFieldName, serviceName, parentTypeName, fieldName));
  }
}
