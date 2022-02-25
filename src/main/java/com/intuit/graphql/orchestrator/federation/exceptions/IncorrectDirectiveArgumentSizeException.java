package com.intuit.graphql.orchestrator.federation.exceptions;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class IncorrectDirectiveArgumentSizeException extends StitchingException {
    private static final String ERROR_EXACT_MSG = "%s Directive for '%s' must have %d arguments";
    private static final String ERROR_ATLEAST_MSG = "%s Directive for '%s' must have at least %d arguments";

    public IncorrectDirectiveArgumentSizeException(String directiveName,String entity, int desiredArgumentSize) {
        this(directiveName, entity, desiredArgumentSize, true);
    }

    public IncorrectDirectiveArgumentSizeException(String directiveName,String entity, int desiredArgumentSize, boolean exactSize) {
        super(String.format(((exactSize) ? ERROR_EXACT_MSG : ERROR_ATLEAST_MSG), directiveName, entity, desiredArgumentSize));
    }
}
