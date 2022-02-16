package com.intuit.graphql.orchestrator.federation.keydirective.exceptions;

public class InvalidLocationForKeyDirective extends KeyDirectiveException {

  private static final String ERR_MSG = "For '%s' key directive in schema is  "
      + "container type '%s' that is not an Object Type, Object Type extension, or Interface";

  public InvalidLocationForKeyDirective(String locationName, String containerName) {
    super(String.format(ERR_MSG, locationName, containerName));
  }

}
