package com.intuit.graphql.orchestrator.utils;

import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;

public class XtextGraphUtils {

  private XtextGraphUtils() {}

  public static void addToCodeRegistry(FieldContext fieldContext, DataFetcherContext dataFetcherContext,
      XtextGraph sourceXtextGraph) {

    sourceXtextGraph
        .getCodeRegistry()
        .put(fieldContext, dataFetcherContext);
  }

  public static void addToCodeRegistry(FieldContext fieldContext, DataFetcherContext dataFetcherContext,
      UnifiedXtextGraph sourceUnifiedXtextGraph) {
    sourceUnifiedXtextGraph
        .getCodeRegistry()
        .put(fieldContext, dataFetcherContext);
  }

}
