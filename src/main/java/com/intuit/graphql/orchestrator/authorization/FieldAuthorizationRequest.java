package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.common.FieldPosition;
import graphql.GraphQLContext;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;

@Builder
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FieldAuthorizationRequest {

    @NonNull
    @EqualsAndHashCode.Include
    private FieldPosition fieldPosition;

    @NonNull
    @EqualsAndHashCode.Include
    private String clientId;

    @NonNull
    @EqualsAndHashCode.Include
    private Map<String, Object> authData;

    @NonNull
    private GraphQLContext graphQLContext;

}
