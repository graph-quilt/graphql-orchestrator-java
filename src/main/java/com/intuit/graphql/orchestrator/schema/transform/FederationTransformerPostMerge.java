package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger.EntityMergingContext;
import com.intuit.graphql.orchestrator.federation.ExternalValidator;
import com.intuit.graphql.orchestrator.federation.Federation2PureGraphQLUtil;
import com.intuit.graphql.orchestrator.federation.extendsdirective.exceptions.BaseTypeNotFoundException;
import com.intuit.graphql.orchestrator.federation.keydirective.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_EXTERNAL_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextGraphUtils.addToCodeRegistry;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isInterfaceTypeDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isObjectTypeDefinition;
import static java.lang.String.format;

public class FederationTransformerPostMerge implements Transformer<XtextGraph, XtextGraph> {

  private static final EntityTypeMerger entityTypeMerger = new EntityTypeMerger();
  private final ExternalValidator externalValidator = new ExternalValidator();
  private final KeyDirectiveValidator keyDirectiveValidator = new KeyDirectiveValidator();

  @Override
  public XtextGraph transform(XtextGraph xtextGraph) {
    xtextGraph.getEntityExtensionsByNamespace().keySet().stream()
        .flatMap(namespace -> createEntityMergingContexts(namespace, xtextGraph))
        .peek(this::validateBaseExtensionCompatibility)
        .map(entityTypeMerger::mergeIntoBaseType)
        .forEach(Federation2PureGraphQLUtil::makeAsPureGraphQL);

      for (FederationMetadata.EntityExtensionMetadata entityExtensionMetadata : xtextGraph.getEntityExtensionMetadatas()) {
          Optional<FederationMetadata.EntityMetadata> optionalEntityMetadata = xtextGraph.getFederationMetadataByNamespace()
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
              TypeDefinition entityBaseType = getBaseEntity(xtextGraph, entityTypename, serviceNamespace);

              return EntityMergingContext.builder()
                  .typename(entityTypename)
                  .serviceNamespace(serviceNamespace)
                  .typeExtension(entityTypeExtension)
                  .baseType(entityBaseType)
                  .build();
            });
  }

  private TypeDefinition getBaseEntity(XtextGraph xtextGraph, String entityTypename, String serviceNamespace) {
        TypeDefinition entityBaseType = xtextGraph.getEntitiesByTypeName().get(entityTypename);
        if (Objects.isNull(entityBaseType)) {
            throw new BaseTypeNotFoundException(entityTypename, serviceNamespace);
        }
        return entityBaseType;
    }

  private void validateBaseExtensionCompatibility(EntityMergingContext entityMergingContext) {
      TypeDefinition baseType = entityMergingContext.getBaseType();
      TypeDefinition typeExtension = entityMergingContext.getTypeExtension();

      // specification: directive @key(fields: _FieldSet!) repeatable on OBJECT | INTERFACE
      if (!(isInterfaceTypeDefinition(baseType) && isInterfaceTypeDefinition(typeExtension)
              || isObjectTypeDefinition(baseType) && isObjectTypeDefinition(typeExtension))) {
          String errMsgTemplate =
                  "Failed to merge entity extension to base type. typename%s, serviceNamespace=%s";
          throw new TypeConflictException(
                  format(
                          errMsgTemplate,
                          entityMergingContext.getTypename(),
                          entityMergingContext.getServiceNamespace()));
      }

      typeExtension.getDirectives().forEach(directive -> {
          String directiveName = directive.getDefinition().getName();
          if(StringUtils.equals(directiveName, FEDERATION_KEY_DIRECTIVE)) {
              keyDirectiveValidator.validatePostMerge(entityMergingContext);
          }
      });

      checkFederationFieldDirectives(entityMergingContext);
  }

  private void checkFederationFieldDirectives(EntityMergingContext entityMergingContext) {
      List<FieldDefinition> fieldDefinitions = getFieldDefinitions(entityMergingContext.getTypeExtension());



      fieldDefinitions.forEach( fieldDefinition ->
          //validate field ownership inside this portion

          fieldDefinition.getDirectives().forEach( directive -> {
              String directiveName = directive.getDefinition().getName();
              if (StringUtils.equals(directiveName, FEDERATION_EXTERNAL_DIRECTIVE)) {
                  externalValidator.validatePostMerge(entityMergingContext, fieldDefinition, directive);
              }
          })
      );

  }
}
