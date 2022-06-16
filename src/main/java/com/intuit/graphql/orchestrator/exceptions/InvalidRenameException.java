package com.intuit.graphql.orchestrator.exceptions;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class InvalidRenameException extends StitchingException  {

    public InvalidRenameException(String message, Throwable t) {
        super(message, t);
    }

    public InvalidRenameException(final String message) {
        super(message);
    }
}
