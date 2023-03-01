package com.intuit.graphql.orchestrator.visitors.queryVisitors;

import graphql.ExecutionInput;
import graphql.language.Field;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class QueryCreatorVisitor extends NodeVisitorStub {

    List<QueryVisitorStub> visitorStubs;
    TraversalControl visitorResult = TraversalControl.CONTINUE;

    public QueryCreatorVisitor(ExecutionInput originalEI) {
        this.visitorStubs = new ArrayList<>();
        //Note !Order is important!
        //TODO add an option for nested defers as feature flag
        //TODO create a POJO for defer options gets passed in at the execute level
        this.visitorStubs.add(
                ClientDirectiveQueryVisitor.builder()
                .originalEI(originalEI)
                .build()
        );
    }

    public TraversalControl visitField(Field node, TraverserContext<Node> context) {
        for (NodeVisitorStub visitor: visitorStubs) {
            this.visitorResult = visitor.visitField(node, context);
            //Do not process the remaining visitor since it should not traverse anymore
            if(this.visitorResult != TraversalControl.CONTINUE) {
                break;
            }
        }

        //calls the parents original method if it was set to continue
        return (this.visitorResult == TraversalControl.CONTINUE) ? this.visitSelection(node, context) : this.visitorResult;
    }

    public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
        for (NodeVisitorStub visitor: visitorStubs) {
            this.visitorResult = visitor.visitFragmentSpread(node, context);
            //Do not process the remaining visitor since it should not traverse anymore
            if(this.visitorResult != TraversalControl.CONTINUE) {
                break;
            }
        }

        //calls the parents original method if it was set to continue
        return (this.visitorResult == TraversalControl.CONTINUE) ? this.visitSelection(node, context) : this.visitorResult;
    }

    public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
        for (NodeVisitorStub visitor: visitorStubs) {
            this.visitorResult = visitor.visitInlineFragment(node, context);
            //Do not process the remaining visitor since it should not traverse anymore
            if(this.visitorResult != TraversalControl.CONTINUE) {
                break;
            }
        }

        //calls the parents original method if it was set to continue
        return (this.visitorResult == TraversalControl.CONTINUE) ? this.visitSelection(node, context) : this.visitorResult;
    }

    public Map<String, Object> getResults() {
        Map<String, Object> results = new HashMap<>();

        this.visitorStubs.stream()
            .map(QueryVisitorStub::getResults)
            .forEach(results::putAll);

        return results;
    }
}
