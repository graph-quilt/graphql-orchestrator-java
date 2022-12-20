package com.intuit.graphql.orchestrator.authorization;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class SelectionSetMetadata {

  @Getter private int remainingSelectionsCount;
  @Getter private final String selectionSetPath;

  public void decreaseRemainingSelection() {
    --this.remainingSelectionsCount;
  }

  public void increaseRemainingSelection(int count) {
    this.remainingSelectionsCount += count;
  }
}
