package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;

public class DomainTypesTransformer implements Transformer<XtextGraph, XtextGraph> {

  public static final String DELIMITER = "_";

  @Override
  public XtextGraph transform(XtextGraph xtextGraph) {

    Set<String> domainTypes = xtextGraph.getServiceProvider().domainTypes();
    if (CollectionUtils.isNotEmpty(domainTypes)) {
      String namespace = xtextGraph.getServiceProvider().getNameSpace();
      XtextUtils.getAllTypes(xtextGraph.getXtextResourceSet())
          .filter(typeDefinition -> domainTypes.contains(typeDefinition.getName()))
          .forEach(domainType -> domainType.setName(namespace + DELIMITER + domainType.getName()));
    }
    return xtextGraph;
  }
}
