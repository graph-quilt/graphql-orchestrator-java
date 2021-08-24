package com.intuit.graphql.orchestrator.authorization;

public interface FieldAuthorization {

    boolean isAccessAllowed(FieldAuthorizationRequest fieldAuthorizationRequest);

}
