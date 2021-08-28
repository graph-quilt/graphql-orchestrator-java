package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.common.FieldPosition;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.StringUtils;

public class DefaultFieldAuthorization implements FieldAuthorization {

    @Override
    public boolean isFieldAuthorizationEnabled() {
        return false;
    }

    @Override
    public boolean isAccessAllowed(FieldAuthorizationRequest fieldAuthorizationRequest) {
        return true;
    }

    @Override
    public boolean requiresAccessControl(FieldPosition fieldPosition) {
        return false;
    }

    @Override
    public CompletableFuture<Object> getFutureAuthData() {
    return CompletableFuture.completedFuture(StringUtils.EMPTY);
    }
}
