package com.intuit.graphql.orchestrator.deferDirective;

import graphql.language.Argument;
import graphql.language.BooleanValue;
import graphql.language.Directive;
import graphql.language.DirectivesContainer;
import graphql.language.Node;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import lombok.NonNull;

import java.util.List;

import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_IF_ARG;

public class DeferUtil {
    /**
     * Checks if it is necessary to create ei for deferred field.
     * Currently, selection should be skipped if all the children field are deferred resulting in an empty selection set.
     * @param selection: node to check if children are all deferred
     * @return boolean: true if all children are deferred, false otherwise
     */
    public static boolean hasNonDeferredSelection(@NonNull  Selection selection) {
        return ((List<Node>)selection.getChildren())
                .stream()
                .filter(SelectionSet.class::isInstance)
                .map(SelectionSet.class::cast)
                .findAny()
                .get()
                .getSelections()
                .stream()
                .anyMatch(child -> !containsEnabledDeferDirective(child));
    }

    /**
     * Verifies that Node has defer directive that is not disabled
     * @param node: node to check if contains defer directive
     * @return boolean: true if node has an enabled defer, false otherwise
     */
    public static boolean containsEnabledDeferDirective(Selection node) {
        return node instanceof DirectivesContainer &&
                ((List<Directive>) ((DirectivesContainer) node).getDirectives())
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
