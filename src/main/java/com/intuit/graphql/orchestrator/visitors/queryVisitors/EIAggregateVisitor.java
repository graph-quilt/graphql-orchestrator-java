package com.intuit.graphql.orchestrator.visitors.queryVisitors;

import com.intuit.graphql.orchestrator.deferDirective.DeferOptions;
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
import java.util.List;

/*
* QueryCreatorVisitor is a Visitor that contains all the visitors pertaining to processing EI.
* This visitor is in charge of calling each visitor in visitor list and controls flow.
* This visitor also maintains and passes the aggregate information like shared context and results of visitors
*/
@Getter
public class EIAggregateVisitor extends NodeVisitorStub {

    List<QueryVisitorStub> visitorStubs;
    TraversalControl visitorResult = TraversalControl.CONTINUE;

    public EIAggregateVisitor(ExecutionInput originalEI, DeferOptions options) {
        this.visitorStubs = new ArrayList<>();
        //Note !Order is important!
        //TODO add an option for nested defers as feature flag
        //TODO create a POJO for defer options gets passed in at the execute level
        this.visitorStubs.add(
                DeferDirectiveQueryModifier.builder()
                .originalEI(originalEI)
                .deferOptions(options)
                .build()
        );
    }

    /*
    * Visits Field and proceeds to call every visitor in list before traversing
    */
    @Override
    public TraversalControl visitField(Field node, TraverserContext<Node> context) {
        return visitorStubs.stream()
                .map(visitor -> visitor.visitField(node, context))
                .filter(result -> !TraversalControl.CONTINUE.equals(result))
                .findFirst()
                .orElse(this.visitSelection(node, context));
    }

    /*
     * Visits Field and proceeds to call every visitor in list before traversing
     */
    @Override
    public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
        return visitorStubs.stream()
                .map(visitor -> visitor.visitFragmentSpread(node, context))
                .filter(result -> !TraversalControl.CONTINUE.equals(result))
                .findFirst()
                .orElse(this.visitSelection(node, context));
    }

    /*
     * Visits Field and proceeds to call every visitor in list before traversing
     */
    @Override
    public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
        return visitorStubs.stream()
                .map(visitor -> visitor.visitInlineFragment(node, context))
                .filter(result -> !TraversalControl.CONTINUE.equals(result))
                .findFirst()
                .orElse(this.visitSelection(node, context));
    }

    /*
    * Retrieves the aggregate results by calling each visitor and getting result
    */
    public QueryCreatorResult generateResults() {
        QueryCreatorResult.QueryCreatorResultBuilder builder = QueryCreatorResult.builder();
        this.visitorStubs.forEach(visitor -> visitor.addResultsToBuilder(builder));
        return builder.build();
    }
}
