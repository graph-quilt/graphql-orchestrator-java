package com.intuit.graphql.orchestrator.visitors.queryVisitors;

import graphql.GraphQLException;
import graphql.language.Argument;
import graphql.language.BooleanValue;
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

import java.util.List;
import java.util.Optional;

import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_IF_ARG;
import static com.intuit.graphql.orchestrator.utils.IntrospectionUtil.__typenameField;
import static graphql.util.TreeTransformerUtil.changeNode;
import static graphql.util.TreeTransformerUtil.deleteNode;

@Builder
public class PruneChildDeferSelectionsModifier extends NodeVisitorStub {
    private boolean nestedDefersAllowed = false;

    @Override
    public TraversalControl visitField(Field node, TraverserContext<Node> context) {
        if(containsValidDeferDirective(node) && isValidDeferredSelection(node)) {
            if(this.nestedDefersAllowed) {
                return deleteNode(context);
            } else {
                throw new GraphQLException("Nested defers are currently unavailable.");
            }
        }

        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
        if(containsValidDeferDirective(node) && isValidDeferredSelection(node)) {
            if(this.nestedDefersAllowed) {
                return deleteNode(context);
            } else {
                throw new GraphQLException("Nested defers are currently unavailable.");
            }
        }

        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
        if(containsValidDeferDirective(node) && isValidDeferredSelection(node)) {
            if(this.nestedDefersAllowed) {
                return deleteNode(context);
            } else {
                throw new GraphQLException("Nested defers are currently unavailable.");
            }
        }

        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
        SelectionSet newSelectionSet = node.transform(builder -> builder.selection(__typenameField));

        return changeNode(context, newSelectionSet);
    }

    private boolean isValidDeferredSelection(Node selection) {
        Optional<SelectionSet> possibleSelectionSet = ((List<Node>)selection.getChildren())
                .stream()
                .filter(SelectionSet.class::isInstance)
                .map(SelectionSet.class::cast)
                .findAny();

        //it is leaf node with no children or at least one of the children are not deferred
        return !possibleSelectionSet.isPresent() ||
                possibleSelectionSet.get().getChildren().stream().anyMatch(child -> !containsValidDeferDirective(child));
    }

    private boolean containsValidDeferDirective(Node node) {
        return node instanceof DirectivesContainer && containsValidDeferDirective((DirectivesContainer) node);
    }

    private boolean containsValidDeferDirective(DirectivesContainer directivesContainer) {
        //DirectivesContainer returns list as List<Object> so casting to correct type
        List<Directive> nodesDirectives = directivesContainer.getDirectives();

        return nodesDirectives
                .stream()
                .filter(directive ->  DEFER_DIRECTIVE_NAME.equals(directive.getName()))
                .findFirst()
                .map(directive -> {
                    Argument ifArg = directive.getArgument(DEFER_IF_ARG);
                    return ifArg == null || ((BooleanValue) ifArg.getValue()).isValue();
                })
                .orElse(false);

    }
}
