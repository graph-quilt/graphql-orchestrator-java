package com.intuit.graphql.orchestrator.stitching;


public class InvalidDirectivePairingException extends StitchingException {

  private static final String ERR_MSG = "Field %s in container type %s with resolver directive not allowed "
      + "to have argument definitions.";

  public InvalidDirectivePairingException(String fieldName, String containerName) {
    super(String.format(ERR_MSG, fieldName, containerName));
  }
}
