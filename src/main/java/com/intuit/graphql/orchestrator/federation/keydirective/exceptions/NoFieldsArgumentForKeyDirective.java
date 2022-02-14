package com.intuit.graphql.orchestrator.federation.keydirective.exceptions;

public class NoFieldsArgumentForKeyDirective extends KeyDirectiveException {
    private static final String ERROR_MSG = "Key directive for '%s' needs to have fields argument";

    public NoFieldsArgumentForKeyDirective(String entity){
        super(String.format(ERROR_MSG, entity));
    }

}
