package com.intuit.graphql.orchestrator.federation.requiresDirective.exceptions;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class EmptyFieldSetException extends StitchingException {
    private static String ERROR_MSG = "";

    public EmptyFieldSetException(String fidl) {
        super(ERROR_MSG);
    }
}
