package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphQLContext;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BatchFieldAuthorization {

    CompletableFuture<Object> getFutureAuthData();

    void batchAuthorizeOrThrowGraphQLError(GraphQLContext context,
        List<DataFetchingEnvironment> keys, Object authData) throws GraphqlErrorException;
}
