package com.intuit.graphql.orchestrator.federation.extendsdirective.exceptions;

import static java.lang.String.format;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

/**
 * This exception should be thrown if a service provider is extending a type
 * but the base type is not found.
 */
public class BaseTypeNotFoundException extends StitchingException {

  public BaseTypeNotFoundException(String entityTypename) {
    super(format("Base type not found.  typename=%s", entityTypename));
  }

}