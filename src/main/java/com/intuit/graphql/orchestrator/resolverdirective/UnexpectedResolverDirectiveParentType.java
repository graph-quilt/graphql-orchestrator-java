package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class UnexpectedResolverDirectiveParentType extends StitchingException {

  public UnexpectedResolverDirectiveParentType(String message) {
    super(message);
  }
}
