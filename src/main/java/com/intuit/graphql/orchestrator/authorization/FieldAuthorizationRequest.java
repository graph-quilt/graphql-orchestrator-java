package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.common.FieldPosition;
import graphql.GraphQLContext;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;

@Builder
@Getter
public class FieldAuthorizationRequest {

    @NonNull
    private FieldPosition fieldPosition;

    @NonNull
    private String clientId;

    @NonNull
    private Map<String, Object> authData;

    @NonNull
    private GraphQLContext graphQLContext;

}
