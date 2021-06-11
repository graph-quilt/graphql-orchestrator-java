package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

/**
 * A class thrown if an error occured during nested merging.
 */
public class FieldMergeException extends StitchingException {

  public FieldMergeException(String message) {
    super(message);
  }
}
