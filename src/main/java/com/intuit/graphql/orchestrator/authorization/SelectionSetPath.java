package com.intuit.graphql.orchestrator.authorization;

import graphql.language.SelectionSet;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class SelectionSetPath {

    private final List<String> pathList = new ArrayList<>();

    private int originalSelectionsCount;

    private int remainingSelectionsCount;

    public SelectionSetPath(SelectionSet subSelectionSet) {
        this.originalSelectionsCount = subSelectionSet == null ? 0 : CollectionUtils.size(subSelectionSet.getSelections());
        this.remainingSelectionsCount = this.originalSelectionsCount;
    }

    public void add(String selectionName) {
        this.pathList.add(requireNonNull(selectionName));
    }

    public static SelectionSetPath createRelativePath(SelectionSetPath oldSelectionSetPath, String selectionName, SelectionSet subSelectionSet) {
        SelectionSetPath selectionSetPath = new SelectionSetPath(subSelectionSet);
        selectionSetPath.pathList.addAll(oldSelectionSetPath.pathList);
        selectionSetPath.add(selectionName);
        return selectionSetPath;
    }

    public void decreaseRemainingSelection() {
        --this.remainingSelectionsCount;
    }

    public int getRemainingSelectionsCount() {
        return this.remainingSelectionsCount;
    }

    public List<String> getPathList() {
        return pathList;
    }
}
