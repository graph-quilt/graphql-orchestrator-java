package com.intuit.graphql.orchestrator.utils;

import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;

public class XtextGraphUtils {

  private XtextGraphUtils() {}

  public static void addToCodeRegistry(FieldContext fieldContext, DataFetcherContext dataFetcherContext,
      XtextGraph sourceXtextGraph) {

    sourceXtextGraph
        .getCodeRegistry()
        .put(fieldContext, dataFetcherContext);
  }

}
