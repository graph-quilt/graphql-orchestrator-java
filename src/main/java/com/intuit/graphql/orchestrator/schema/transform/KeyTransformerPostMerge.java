package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger.EntityMergingContext;
import com.intuit.graphql.orchestrator.federation.Federation2PureGraphQLUtil;
import com.intuit.graphql.orchestrator.federation.extendsdirective.exceptions.BaseTypeNotFoundException;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.Objects;
import java.util.stream.Stream;

public class KeyTransformerPostMerge implements Transformer<XtextGraph, XtextGraph> {

  private static final EntityTypeMerger entityTypeMerger = new EntityTypeMerger();

  @Override
  public XtextGraph transform(XtextGraph xtextGraph) {
    xtextGraph.getEntityExtensionsByNamespace().keySet().stream()
        .flatMap(namespace -> createEntityMergingContexts(namespace, xtextGraph))
        .map(entityTypeMerger::mergeIntoBaseType)
        .forEach(Federation2PureGraphQLUtil::makeAsPureGraphQL);
    return xtextGraph;
  }

  private Stream<EntityMergingContext> createEntityMergingContexts(
      String serviceNamespace, XtextGraph xtextGraph) {
    return xtextGraph.getEntityExtensionsByNamespace().get(serviceNamespace).stream()
        .map(
            entityTypeExtension -> {
              String entityTypename = entityTypeExtension.getName();
              TypeDefinition entityBaseType =
                  xtextGraph.getEntitiesByTypeName().get(entityTypename);
              if (Objects.isNull(entityBaseType)) {
                throw new BaseTypeNotFoundException(entityTypename, serviceNamespace);
              }
              return EntityMergingContext.builder()
                  .typename(entityTypename)
                  .serviceNamespace(serviceNamespace)
                  .typeExtension(entityTypeExtension)
                  .baseType(entityBaseType)
                  .build();
            });
  }
}
