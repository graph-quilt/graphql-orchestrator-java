package com.intuit.graphql.orchestrator.federation.exceptions;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

import static java.lang.String.format;

public class SharedOwnershipException extends StitchingException {
    private static final String ERROR_MSG = "No @External directive found. Field '%s' is already declared and defined in the base type.";

    public SharedOwnershipException(String fieldName) {
        super(format(ERROR_MSG, fieldName));
    }
}
