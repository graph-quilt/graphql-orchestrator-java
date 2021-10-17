package com.intuit.graphql.orchestrator.authorization;

import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.Node;
import graphql.util.TraverserContext;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class SelectionSetMetadata {

    private int remainingSelectionsCount;
    @Getter private final String selectionSetPath;

    public void decreaseRemainingSelection() {
        --this.remainingSelectionsCount;
    }

    public int getRemainingSelectionsCount() {
        return this.remainingSelectionsCount;
    }

    public static String getSelectionSetPathString(TraverserContext<Node> context) {
        List<String> pathList = context.getParentNodes().stream()
            .filter(node -> node instanceof Field || node instanceof FragmentDefinition)
            .map(node -> (node instanceof Field) ? ((Field)node).getName() : ((FragmentDefinition)node).getName())
            .collect(Collectors.toList());
        Collections.reverse(pathList);
        StringJoiner pathBuilder = new StringJoiner(".");
        pathList.forEach(pathBuilder::add);
        return pathBuilder.toString();
    }

}
