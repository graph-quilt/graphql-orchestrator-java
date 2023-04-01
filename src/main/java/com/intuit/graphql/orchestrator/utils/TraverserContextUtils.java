package com.intuit.graphql.orchestrator.utils;

import graphql.language.Node;
import graphql.language.SelectionSetContainer;
import graphql.util.TraverserContext;

import java.util.List;
import java.util.stream.Collectors;

public class TraverserContextUtils {
    /**
     * Returns the current nodes parent node definitions
     * @param currentNode context for current node
     * @return List of graphql definitions
     * */
    public static List getParentDefinitions(TraverserContext<Node> currentNode) {
        return currentNode.getParentNodes()
                .stream()
                .filter(SelectionSetContainer.class::isInstance)
                .collect(Collectors.toList());
    }
}
