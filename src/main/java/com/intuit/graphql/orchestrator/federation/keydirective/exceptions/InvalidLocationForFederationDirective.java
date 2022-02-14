package com.intuit.graphql.orchestrator.federation.keydirective.exceptions;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class InvalidLocationForFederationDirective extends StitchingException {

    private static final String ERR_MSG = "Directive '%s' cannot be in a non federated provider";

    public InvalidLocationForFederationDirective(String directiveName) {
        super(String.format(ERR_MSG, directiveName));
    }
}