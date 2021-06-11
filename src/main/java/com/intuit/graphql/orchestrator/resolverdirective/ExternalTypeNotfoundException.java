package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class ExternalTypeNotfoundException extends StitchingException {

  private static final String MESSAGE_TEMPLATE =
      "External type not found.  "
          + "serviceName=%s, "
          + "parentTypeName=%s, "
          + "fieldName=%s, "
          + "placeHolderTypeDescription=%s";

  public ExternalTypeNotfoundException(
      String serviceName,
      String parentTypeName,
      String fieldName,
      String placeHolderTypeDescription) {
    super(String.format(MESSAGE_TEMPLATE, serviceName, parentTypeName, fieldName,
        placeHolderTypeDescription));
  }

}
