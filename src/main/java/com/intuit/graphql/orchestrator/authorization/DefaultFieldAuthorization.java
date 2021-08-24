package com.intuit.graphql.orchestrator.authorization;

public class DefaultFieldAuthorization implements FieldAuthorization {

    @Override
    public boolean isAccessAllowed(FieldAuthorizationRequest fieldAuthorizationRequest) {
        return true;
    }
}
