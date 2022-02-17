package com.intuit.graphql.orchestrator.federation.exceptions;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class FederationDirectiveInvalidProviderException extends StitchingException {

    private static final String ERR_MSG = "Directive '%s' cannot be in a non federated provider";

    public FederationDirectiveInvalidProviderException(String directiveName) {
        super(String.format(ERR_MSG, directiveName));
    }
}