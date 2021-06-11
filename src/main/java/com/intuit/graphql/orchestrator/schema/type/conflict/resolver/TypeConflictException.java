package com.intuit.graphql.orchestrator.schema.type.conflict.resolver;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class TypeConflictException extends StitchingException {

  public TypeConflictException(String message) {
    super(message);
  }

}
