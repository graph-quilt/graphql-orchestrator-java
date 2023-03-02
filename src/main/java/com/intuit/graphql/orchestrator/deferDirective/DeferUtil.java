package com.intuit.graphql.orchestrator.deferDirective;

import graphql.language.Argument;
import graphql.language.BooleanValue;
import graphql.language.Directive;
import graphql.language.DirectivesContainer;
import graphql.language.Node;
import graphql.language.SelectionSet;

import java.util.List;
import java.util.Optional;

import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_IF_ARG;

public class DeferUtil {

    /*
     * Checks if it is necessary to create ei for deferred field.
     * Currently, selection should be skipped if all the children field are deferred resulting in an empty selection set.
     */
    public static boolean hasNonDeferredSelection(Node selection) {
        Optional<SelectionSet> possibleSelectionSet = ((List<Node>)selection.getChildren())
                .stream()
                .filter(SelectionSet.class::isInstance)
                .map(SelectionSet.class::cast)
                .findAny();

        //it is leaf node with no children
        return !possibleSelectionSet.isPresent() ||
                // at least one of the children are not deferred
                possibleSelectionSet.get().getChildren().stream().anyMatch(child -> !containsEnabledDeferDirective(child));
    }

    /*
     * Verifies that Node has defer directive that is not disabled
     */
    public static  boolean containsEnabledDeferDirective(Node node) {
        return node instanceof DirectivesContainer && containsEnabledDeferDirective((DirectivesContainer) node);
    }

    /*
     * Verifies that DirectiveContainer has defer directive that is not disabled
     */
    public static  boolean containsEnabledDeferDirective(DirectivesContainer directivesContainer) {
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
