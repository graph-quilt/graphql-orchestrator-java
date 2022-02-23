package com.intuit.graphql.orchestrator.federation.exceptions;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class DirectiveMissingRequiredArgumentException extends StitchingException {
    private static final String ERROR_MSG = "%s Directive for '%s' cannot have multiple arguments";

    public DirectiveMissingRequiredArgumentException(String directiveName, String entity) {
        super(String.format(ERROR_MSG, directiveName ,entity));
    }
}
