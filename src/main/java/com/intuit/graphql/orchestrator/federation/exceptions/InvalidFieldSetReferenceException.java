package com.intuit.graphql.orchestrator.federation.exceptions;

import com.intuit.graphql.orchestrator.stitching.StitchingException;

public class InvalidFieldSetReferenceException extends StitchingException {

    private static final String ERR_INVALID_FIELD_REF_FORMAT = "Field '%s' does not exist in container '%s'.";

    public InvalidFieldSetReferenceException(String keyFieldName, String containerName) {
        super(String.format(ERR_INVALID_FIELD_REF_FORMAT, keyFieldName, containerName));
    }
}
