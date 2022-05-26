package com.intuit.graphql.orchestrator.federation.exceptions;

import static java.lang.String.format;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

/**
 * This exception should be thrown if a service provider is extending a type but the base type is
 * not found.
 */
public class BaseTypeNotFoundException extends StitchingException {

  private static final String ERR_MSG = "Base type not found.  typename=%s, serviceNamespace=%s";

  public BaseTypeNotFoundException(String entityTypename, String serviceNamespace) {
    super(format(ERR_MSG, entityTypename, serviceNamespace));
  }
}
