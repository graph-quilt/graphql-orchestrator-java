package com.intuit.graphql.orchestrator.federation.exceptions;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class ExternalFieldNotFoundInBaseException extends StitchingException {

    private static final String MESSAGE_TEMPLATE = "Field %s is declared as external, but is not found in the originating type.";

    public ExternalFieldNotFoundInBaseException(String externalFieldName) {
        super(String.format(MESSAGE_TEMPLATE, externalFieldName));
    }
}
