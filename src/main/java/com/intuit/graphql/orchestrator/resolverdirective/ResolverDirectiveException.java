package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class ResolverDirectiveException extends StitchingException {

  public ResolverDirectiveException(String message) {
    super(message);
  }
}
