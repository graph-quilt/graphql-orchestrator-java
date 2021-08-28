package com.intuit.graphql.orchestrator.authorization;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class FieldPath {

  private static final String SEPARATOR = "/";

  private final List<String> pathList = new ArrayList<>();

  public FieldPath(String element) {
    this.add(element);
  }

  private FieldPath(List<String> initialElements) {
    this.pathList.addAll(initialElements);
  }

  private void add(String element) {
    this.pathList.add(element);
  }

  @Override
  public String toString() {
    return StringUtils.join(pathList, SEPARATOR);
  }

  public FieldPath createChildPath(String name) {
    FieldPath newFieldPath = new FieldPath(this.pathList);
    newFieldPath.add(name);
    return newFieldPath;
  }
}
