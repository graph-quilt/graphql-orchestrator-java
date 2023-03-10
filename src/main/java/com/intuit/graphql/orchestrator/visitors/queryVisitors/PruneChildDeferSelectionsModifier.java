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
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import lombok.Builder;

import static com.intuit.graphql.orchestrator.deferDirective.DeferUtil.containsEnabledDeferDirective;
import static com.intuit.graphql.orchestrator.deferDirective.DeferUtil.hasNonDeferredSelection;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.IntrospectionUtil.__typenameField;
import static com.intuit.graphql.orchestrator.utils.NodeUtils.isLeaf;
import static com.intuit.graphql.orchestrator.utils.NodeUtils.removeDirectiveFromNode;
import static graphql.util.TreeTransformerUtil.changeNode;
import static graphql.util.TreeTransformerUtil.deleteNode;

@Builder
public class PruneChildDeferSelectionsModifier extends NodeVisitorStub {
    private DeferOptions deferOptions;

    /**
     * Visits Field and deletes defer information or throws an exception
     * @param node current node traverser is visiting
     * @param context context for the current node
     * @return TraversalControl whether to continue or abort
     * */
    @Override
    public TraversalControl visitField(Field node, TraverserContext<Node> context) {
        return deleteDeferIfExists(node, context);
    }

    /**
     * Visits FragmentSpread and deletes defer information or throws an exception
     * @param node current node traverser is visiting
     * @param context context for the current node
     * @return TraversalControl whether to continue or abort
     * */
    @Override
    public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
        return deleteDeferIfExists(node, context);
    }

    /**
     * Visits Inline Fragment and deletes defer information or throws an exception
     * @param node current node traverser is visiting
     * @param context context for the current node
     * @return TraversalControl whether to continue or abort
     * */
    @Override
    public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
        return deleteDeferIfExists(node, context);
    }

    /**
     * Visits SelectionSet and add typename to selection set
     * @param node current node traverser is visiting
     * @param context context for the current node
     * @return TraversalControl whether to continue or abort
     * */
    @Override
    public TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
        SelectionSet newSelectionSet = node.transform(builder -> builder.selection(__typenameField));

        return changeNode(context, newSelectionSet);
    }

    /**
     * Visits Directive and deletes defer information
     * @param node current node traverser is visiting
     * @param context context for the current node
     * @return TraversalControl whether to continue or abort
     * */
    public TraversalControl visitDirective(Directive node, TraverserContext<Node> context) {
        //removes unnecessary and disabled defer directive if exists
        if(DEFER_DIRECTIVE_NAME.equals(node.getName())) {
            deleteNode(context);
        }

        return this.visitNode(node, context);
    }

    /**
     * deletes defer information or throws an exception for selection
     * @param node current node traverser is visiting
     * @param context context for the current node
     * @return TraversalControl whether to continue or abort
     * */
    private TraversalControl deleteDeferIfExists(Selection node, TraverserContext<Node> context) {
        //skip if it does not have defer directive
        if(((DirectivesContainer)node).hasDirective(DEFER_DIRECTIVE_NAME)) {
            if(containsEnabledDeferDirective(node)) {
                //if node has an enabled defer, check if option allows it
                if(!this.deferOptions.isNestedDefersAllowed()) {
                    throw new GraphQLException("Nested defers are currently unavailable.");
                }

                if(isLeaf(node) || hasNonDeferredSelection(node)) {
                    //delete node if it is enabled because extractor will create query for it
                    return deleteNode(context);
                } else {
                    //remove directive so it is not included in downstream query
                    return  changeNode(context, removeDirectiveFromNode(node, DEFER_DIRECTIVE_NAME));
                }
            } else {
                //remove directive so it is not included in downstream query
                return  changeNode(context, removeDirectiveFromNode(node, DEFER_DIRECTIVE_NAME));
            }
        }

        return TraversalControl.CONTINUE;
    }
}
