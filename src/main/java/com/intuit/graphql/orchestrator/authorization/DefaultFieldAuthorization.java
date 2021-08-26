package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.common.FieldPosition;

public class DefaultFieldAuthorization implements FieldAuthorization {

    @Override
    public boolean isAccessAllowed(FieldAuthorizationRequest fieldAuthorizationRequest) {
        return true;
    }

    @Override
    public boolean requiresAccessControl(FieldPosition fieldPosition) {
        return false;
    }
}
