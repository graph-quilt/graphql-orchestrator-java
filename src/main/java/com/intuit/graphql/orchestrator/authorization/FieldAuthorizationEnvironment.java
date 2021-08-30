package com.intuit.graphql.orchestrator.authorization;

import graphql.schema.FieldCoordinates;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;

@Builder
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FieldAuthorizationEnvironment<AuthDataT> {

    @NonNull
    @EqualsAndHashCode.Include
    private FieldCoordinates fieldCoordinates;

    @NonNull
    @EqualsAndHashCode.Include
    Map<String, Object> fieldArguments;

    @NonNull
    @EqualsAndHashCode.Include
    private AuthDataT authData;


}
