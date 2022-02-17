package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger;
import com.intuit.graphql.orchestrator.federation.Federation2PureGraphQLUtil;
import com.intuit.graphql.orchestrator.federation.extendsdirective.exceptions.BaseTypeNotFoundException;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class KeyTransformerPostMerge implements Transformer<XtextGraph, XtextGraph> {

  private static final EntityTypeMerger entityTypeMerger = new EntityTypeMerger();

  @Override
  public XtextGraph transform(XtextGraph xtextGraph) {
    Map<String, TypeDefinition> entitiesByTypename = xtextGraph.getEntitiesByTypeName();
    Map<String, List<TypeDefinition>> entityExtensionsByNamespace =
        xtextGraph.getEntityExtensionsByNamespace();

    entityExtensionsByNamespace.keySet().stream()
        .flatMap(namespace -> entityExtensionsByNamespace.get(namespace).stream())
        .map(entityTypeExtension -> {
          String entityTypename = entityTypeExtension.getName();
          TypeDefinition entityBaseType = entitiesByTypename.get(entityTypename);
          if (Objects.isNull(entityBaseType)) {
            // TODO,
            //   1. Can this check be done before postMerge?  What if the base service has not registered or was processed late?
            //   2. Add source namespace sourceService=extensionMetadata.getServiceMetadata().getNamespace()
            throw new BaseTypeNotFoundException(entityTypename);
          }
          return EntityTypeMerger.createEntityTypeMergerContext(entityBaseType, entityTypeExtension);
        })
        .forEach(entityTypeMerger::mergeIntoBaseType);

    entitiesByTypename.values().forEach(Federation2PureGraphQLUtil::makeAsPureGraphQL);

    return xtextGraph;
  }

}
