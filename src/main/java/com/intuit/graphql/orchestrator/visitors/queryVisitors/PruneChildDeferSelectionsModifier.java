package com.intuit.graphql.orchestrator.visitors.queryVisitors;

import com.intuit.graphql.orchestrator.deferDirective.DeferOptions;
import graphql.GraphQLException;
import graphql.language.Directive;
import graphql.language.DirectivesContainer;
import graphql.language.Field;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.SelectionSet;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import lombok.Builder;

import static com.intuit.graphql.orchestrator.deferDirective.DeferUtil.containsEnabledDeferDirective;
import static com.intuit.graphql.orchestrator.deferDirective.DeferUtil.hasNonDeferredSelection;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.IntrospectionUtil.__typenameField;
import static graphql.util.TreeTransformerUtil.changeNode;
import static graphql.util.TreeTransformerUtil.deleteNode;

@Builder
public class PruneChildDeferSelectionsModifier extends NodeVisitorStub {
    private DeferOptions deferOptions;

    @Override
    public TraversalControl visitField(Field node, TraverserContext<Node> context) {
        return deleteDeferIfExists(node, context);
    }

    @Override
    public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
        return deleteDeferIfExists(node, context);
    }

    @Override
    public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
        return deleteDeferIfExists(node, context);
    }

    @Override
    public TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
        SelectionSet newSelectionSet = node.transform(builder -> builder.selection(__typenameField));

        return changeNode(context, newSelectionSet);
    }

    public TraversalControl visitDirective(Directive node, TraverserContext<Node> context) {
        //removes unnecessary and disabled defer directive if exists
        if(DEFER_DIRECTIVE_NAME.equals(node.getName())) {
            deleteNode(context);
        }

        return this.visitNode(node, context);
    }

    private TraversalControl deleteDeferIfExists(DirectivesContainer node, TraverserContext<Node> context) {
        //skip if it does not have the defer directive
        if(node.hasDirective(DEFER_DIRECTIVE_NAME) && containsEnabledDeferDirective(node)) {
            //if node has an enabled defer, check if option allows it
            if(!this.deferOptions.isNestedDefersAllowed()) {
                throw new GraphQLException("Nested defers are currently unavailable.");
            }

           if(hasNonDeferredSelection(node)) {
               return deleteNode(context);
           }
        }

        return TraversalControl.CONTINUE;
    }
}
