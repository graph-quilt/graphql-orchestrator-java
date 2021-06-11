package com.intuit.graphql.orchestrator.resolverdirective;

public class NotAValidLocationForFieldResolverDirective extends ResolverDirectiveException {

  private static final String ERR_MSG = "Field %s with resolver directive is defined in "
      + "container type %s that is not an Object Type or Object Type extension";

  public NotAValidLocationForFieldResolverDirective(String fieldName, String containerName) {
    super(String.format(ERR_MSG, fieldName, containerName));
  }

}
