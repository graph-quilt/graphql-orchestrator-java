package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.utils.XtextGraphUtils.addToCodeRegistry;

import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger.EntityMergingContext;
import com.intuit.graphql.orchestrator.federation.Federation2PureGraphQLUtil;
import com.intuit.graphql.orchestrator.federation.extendsdirective.exceptions.BaseTypeNotFoundException;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityMetadata;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class KeyTransformerPostMerge implements Transformer<XtextGraph, XtextGraph> {

  private static final EntityTypeMerger entityTypeMerger = new EntityTypeMerger();

  @Override
  public XtextGraph transform(XtextGraph xtextGraph) {
    xtextGraph.getEntityExtensionsByNamespace().keySet().stream()
        .flatMap(namespace -> createEntityMergingContexts(namespace, xtextGraph))
        .map(entityTypeMerger::mergeIntoBaseType)
        .forEach(Federation2PureGraphQLUtil::makeAsPureGraphQL);

    for (EntityExtensionMetadata entityExtensionMetadata : xtextGraph.getEntityExtensionMetadatas()) {
      Optional<EntityMetadata> optionalEntityMetadata = xtextGraph.getFederationMetadataByNamespace()
          .values()
          .stream()
          .flatMap(federationMetadata -> federationMetadata.getEntitiesByTypename().values().stream())
          .filter(entityMetadata -> entityMetadata.getTypeName().equals(entityExtensionMetadata.getTypeName()))
          .findFirst();

      entityExtensionMetadata.setBaseEntityMetadata(optionalEntityMetadata.get());
      entityExtensionMetadata.getRequiredFieldsByFieldName().forEach((fieldName, strings) -> {
        FieldContext fieldContext = new FieldContext(entityExtensionMetadata.getTypeName(), fieldName);
        DataFetcherContext dataFetcherContext = DataFetcherContext
            .newBuilder()
            .dataFetcherType(DataFetcherContext.DataFetcherType.ENTITY_FETCHER)
            .entityExtensionMetadata(entityExtensionMetadata)
            .build();
        addToCodeRegistry(fieldContext, dataFetcherContext, xtextGraph);
      });
    }
    return xtextGraph;
  }

  private Stream<EntityMergingContext> createEntityMergingContexts(
      String serviceNamespace, XtextGraph xtextGraph) {
    return xtextGraph.getEntityExtensionsByNamespace().get(serviceNamespace).values().stream()
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
