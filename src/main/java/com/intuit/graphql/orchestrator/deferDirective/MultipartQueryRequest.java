package com.intuit.graphql.orchestrator.deferDirective;

import graphql.language.OperationDefinition;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Set;

@Getter
@Builder
public class MultipartQueryRequest {
    private OperationDefinition multipartOperationDef;
    @Singular
    private Set<String> fragmentSpreadNames;
}
