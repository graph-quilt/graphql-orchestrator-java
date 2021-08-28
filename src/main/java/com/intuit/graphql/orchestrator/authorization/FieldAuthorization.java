package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.common.FieldPosition;
import java.util.concurrent.CompletableFuture;

public interface FieldAuthorization {

    boolean isFieldAuthorizationEnabled();

    boolean isAccessAllowed(FieldAuthorizationRequest fieldAuthorizationRequest);

    boolean requiresAccessControl(FieldPosition fieldPosition);

    CompletableFuture<Object> getFutureAuthData();
}
