package com.intuit.graphql.orchestrator.utils;

import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.Node;
import graphql.util.TraverserContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class QueryPathUtils {

  private static final String PATH_SEPARATOR = ".";

  private QueryPathUtils(){}

  public static List<Object> getParentNodesAsPathList(TraverserContext<Node> context) {
    List<Node> nodes = new ArrayList<>(context.getParentNodes());
    Collections.reverse(nodes);
    nodes.add(context.thisNode());
    List<Object> pathList = nodes.stream()
        .filter(node -> node instanceof Field || node instanceof FragmentDefinition)
        .map(node -> (node instanceof Field) ? ((Field)node).getName() : ((FragmentDefinition)node).getName())
        .collect(Collectors.toList());

    return pathList;
  }

  public static String pathListToFQN(List<Object> pathList) {
    return StringUtils.join(pathList, PATH_SEPARATOR);
  }

}