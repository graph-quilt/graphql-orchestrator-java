package com.intuit.graphql.orchestrator.authorization;

public class SelectionSetFieldsCounter {

    private int remainingSelectionsCount;

    public SelectionSetFieldsCounter(int originalSelectionsCount) {
        this.remainingSelectionsCount = originalSelectionsCount;
    }

    public void decreaseRemainingSelection() {
        --this.remainingSelectionsCount;
    }

    public int getRemainingSelectionsCount() {
        return this.remainingSelectionsCount;
    }

}
