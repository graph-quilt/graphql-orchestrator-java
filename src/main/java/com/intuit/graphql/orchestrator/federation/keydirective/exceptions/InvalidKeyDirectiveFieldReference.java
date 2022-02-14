package com.intuit.graphql.orchestrator.federation.keydirective.exceptions;

public class InvalidKeyDirectiveFieldReference extends KeyDirectiveException {

  private static final String ERR_INVALID_FIELD_REF_FORMAT = "'%s' does not exist in container '%s'.";

  public InvalidKeyDirectiveFieldReference(String keyFieldName, String containerName) {
    super(String.format(ERR_INVALID_FIELD_REF_FORMAT, keyFieldName, containerName));
  }
}
