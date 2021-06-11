package com.intuit.graphql.orchestrator.stitching;

/**
 * The base exception class for any error that occured in this library.
 */
public class StitchingException extends RuntimeException {

  public StitchingException(String message, Throwable t) {
    super(message, t);
  }

  public StitchingException(final String message) {
    super(message);
  }
}
