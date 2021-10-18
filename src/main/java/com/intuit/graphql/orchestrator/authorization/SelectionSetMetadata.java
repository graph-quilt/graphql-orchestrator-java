package com.intuit.graphql.orchestrator.authorization;

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

}
