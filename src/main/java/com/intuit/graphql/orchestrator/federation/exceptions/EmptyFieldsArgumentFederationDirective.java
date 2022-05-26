package com.intuit.graphql.orchestrator.federation.exceptions;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class EmptyFieldsArgumentFederationDirective extends StitchingException {
    private static final String ERROR_MSG = "Fields argument cannot be empty for %s directive in '%s'";

    public EmptyFieldsArgumentFederationDirective(String containerName, String directiveName) {
        super(String.format(ERROR_MSG, directiveName, containerName));
    }
}
