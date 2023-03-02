package com.intuit.graphql.orchestrator.visitors.queryVisitors;

import graphql.ExecutionInput;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class QueryCreatorResult {
    private List<ExecutionInput> forkedDeferEIs;
}
