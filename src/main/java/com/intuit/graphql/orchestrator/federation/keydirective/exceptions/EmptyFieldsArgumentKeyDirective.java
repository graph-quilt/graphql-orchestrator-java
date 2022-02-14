package com.intuit.graphql.orchestrator.federation.keydirective.exceptions;

public class EmptyFieldsArgumentKeyDirective extends KeyDirectiveException {
    private static final String ERROR_MSG = "Fields argument cannot be empty for key directive in '%s'";

    public EmptyFieldsArgumentKeyDirective(String containerName) {
        super(String.format(ERROR_MSG, containerName));
    }
}
