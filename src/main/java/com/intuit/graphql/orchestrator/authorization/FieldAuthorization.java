package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import graphql.schema.FieldCoordinates;
import java.util.concurrent.CompletableFuture;

public interface FieldAuthorization<AuthDataT> {

    boolean isAccessAllowed(FieldAuthorizationEnvironment<AuthDataT> fieldAuthorizationEnvironment);

    boolean requiresAccessControl(FieldCoordinates fieldCoordinates);

    CompletableFuture<AuthDataT> getFutureAuthData();

    GraphqlErrorException getDeniedGraphQLErrorException();
}
