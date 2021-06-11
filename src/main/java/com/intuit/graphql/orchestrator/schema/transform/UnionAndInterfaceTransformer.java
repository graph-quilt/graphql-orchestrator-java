package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isInterfaceOrUnionType;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.Objects;
import java.util.Optional;

public class UnionAndInterfaceTransformer implements Transformer<XtextGraph, XtextGraph> {

  @Override
  public XtextGraph transform(XtextGraph xtextGraph) {

    Optional<FieldDefinition> unionOrInterface = XtextUtils
        .getAllContentsOfType(FieldDefinition.class, xtextGraph.getXtextResourceSet())
        .filter(this::isUnionOrInterface).findAny();

    if (unionOrInterface.isPresent()) {
      return xtextGraph.transform(builder -> builder.hasInterfaceOrUnion(true));
    }
    return xtextGraph;
  }

  private boolean isUnionOrInterface(FieldDefinition fieldDefinition) {
    return Objects.nonNull(fieldDefinition.getNamedType()) && isInterfaceOrUnionType(fieldDefinition.getNamedType());
  }
}


