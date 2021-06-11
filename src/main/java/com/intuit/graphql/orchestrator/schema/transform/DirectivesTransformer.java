package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.DirectiveDefinition;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.Set;
import java.util.stream.Collectors;

public class DirectivesTransformer implements Transformer<XtextGraph, XtextGraph> {

  @Override
  public XtextGraph transform(XtextGraph source) {
    Set<DirectiveDefinition> directives = XtextUtils
        .getAllContentsOfType(DirectiveDefinition.class, source.getXtextResourceSet())
        .collect(Collectors.toSet());
    return source.transform(builder -> builder.directives(directives));
  }
}
