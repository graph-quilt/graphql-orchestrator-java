package com.intuit.graphql.orchestrator.authorization;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

public class FieldPath {

  private static final String SEPARATOR = "/";

  private final List<String> pathList = new ArrayList<>();

  public FieldPath() {
  }

  public FieldPath(String initialElement) {
    this.add(initialElement);
  }

  public FieldPath createChildPath(String newElement) {
    Objects.requireNonNull(newElement);
    FieldPath newFieldPath = new FieldPath();
    newFieldPath.pathList.addAll(this.pathList);
    newFieldPath.pathList.add(newElement);
    return newFieldPath;
  }

  private void add(String newElement) {
    Objects.requireNonNull(newElement);
    this.pathList.add(newElement);
  }

  @Override
  public String toString() {
    return StringUtils.join(pathList, SEPARATOR);
  }

}
