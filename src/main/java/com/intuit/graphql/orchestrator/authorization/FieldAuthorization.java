package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.common.FieldPosition;
import java.util.concurrent.CompletableFuture;

public interface FieldAuthorization<AuthDataT> {

    boolean isAccessAllowed(FieldAuthorizationRequest fieldAuthorizationRequest);

    boolean requiresAccessControl(FieldPosition fieldPosition);

    CompletableFuture<AuthDataT> getFutureAuthData();
}
