package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class MultipleResolverDirectiveDefinition extends StitchingException {

  private static final String ERROR_MESSAGE = "Expecting to have 1 resolver directive but "
      + "found multiple definitions.  directives count = %s";

  public MultipleResolverDirectiveDefinition(int size) {
    super(String.format(ERROR_MESSAGE, size));
  }

}
