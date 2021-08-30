package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.common.FieldPosition;
import graphql.GraphqlErrorException;
import java.util.concurrent.CompletableFuture;

public interface FieldAuthorization<AuthDataT> {

    boolean isAccessAllowed(FieldAuthorizationEnvironment<AuthDataT> fieldAuthorizationEnvironment);

    boolean requiresAccessControl(FieldPosition fieldPosition);

    CompletableFuture<AuthDataT> getFutureAuthData();

    GraphqlErrorException getDeniedGraphQLErrorException();
}
