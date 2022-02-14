package com.intuit.graphql.orchestrator.federation.keydirective.exceptions;

public class MultipleArgumentsForKeyDirective extends KeyDirectiveException {
    private static final String ERROR_MSG = "Key Directive for '%s' cannot have multiple arguments";

    public MultipleArgumentsForKeyDirective(String entity) {
        super(String.format(ERROR_MSG, entity));
    }
}
