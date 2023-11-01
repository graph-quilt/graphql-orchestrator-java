package com.intuit.graphql.orchestrator.stitching;


import java.util.List;

public class InvalidDirectivePairingException extends StitchingException {

  private static final String ERR_MSG = "Field %s in container type %s with resolver directive not allowed "
      + "to have argument definitions.";

  public InvalidDirectivePairingException(List<String> directiveNames) {
    super(String.format(ERR_MSG, directiveNames.get(0), directiveNames.get(1)));
  }
}
