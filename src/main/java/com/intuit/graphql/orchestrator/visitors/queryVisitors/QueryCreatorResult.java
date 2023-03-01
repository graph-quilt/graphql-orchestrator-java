package com.intuit.graphql.orchestrator.visitors.queryVisitors;

import graphql.ExecutionInput;
import lombok.AllArgsConstructor;

import java.util.List;

import static com.intuit.graphql.orchestrator.visitors.queryVisitors.DeferDirectiveQueryVisitor.GENERATED_EIS;

@AllArgsConstructor
public class QueryCreatorResult {

    private QueryCreatorVisitor creatorVisitor;

    public List<ExecutionInput> getForkedDeferEIs(){
        return (List<ExecutionInput>)creatorVisitor.getResults().get(GENERATED_EIS);
    }
}
