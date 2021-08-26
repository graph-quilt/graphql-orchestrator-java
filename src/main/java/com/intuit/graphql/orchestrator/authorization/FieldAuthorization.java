package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.common.FieldPosition;

public interface FieldAuthorization {

    boolean isAccessAllowed(FieldAuthorizationRequest fieldAuthorizationRequest);

    boolean requiresAccessControl(FieldPosition fieldPosition);
}
