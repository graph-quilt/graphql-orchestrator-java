package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeExtensionDefinition;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger.EntityMergingContext;
import com.intuit.graphql.orchestrator.federation.Federation2PureGraphQLUtil;
import com.intuit.graphql.orchestrator.federation.exceptions.BaseTypeNotFoundException;
import com.intuit.graphql.orchestrator.federation.exceptions.SharedOwnershipException;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.validators.ExternalValidator;
import com.intuit.graphql.orchestrator.federation.validators.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_EXTENDS_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_EXTERNAL_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_INACCESSIBLE_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.isTypeSystemForBaseType;
import static com.intuit.graphql.orchestrator.utils.XtextGraphUtils.addToCodeRegistry;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isInterfaceTypeDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isInterfaceTypeExtensionDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isObjectTypeExtensionDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.definitionContainsDirective;
import static java.lang.String.format;

public class FederationTransformerPostMerge implements Transformer<XtextGraph, XtextGraph> {

  private static final EntityTypeMerger entityTypeMerger = new EntityTypeMerger();
  private final ExternalValidator externalValidator = new ExternalValidator();
  private final KeyDirectiveValidator keyDirectiveValidator = new KeyDirectiveValidator();

  @Override
  public XtextGraph transform(XtextGraph xtextGraph) {
      List<TypeDefinition> baseEntityTypes = xtextGraph.getEntityExtensionsByNamespace().keySet().stream()
              .flatMap(namespace -> createEntityMergingContexts(namespace, xtextGraph))
              .peek(this::validateBaseExtensionCompatibility)
              .map(entityTypeMerger::mergeIntoBaseType)
              .collect(Collectors.toList());

      //prune inaccessible info from entity types
      pruneInaccessibleInfo(xtextGraph, baseEntityTypes);
      baseEntityTypes.forEach(Federation2PureGraphQLUtil::makeAsPureGraphQL);

      //prune inaccessible info from value types
      pruneInaccessibleInfo(xtextGraph, xtextGraph.getValueTypesByName().values());

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
                String entityTypename = null;
                if(entityTypeExtension.getType() != null) {
                  entityTypename = entityTypeExtension.getType().getName();
                } else {
                  entityTypename = entityTypeExtension.getTypeExtension().getName();
                }

                TypeDefinition entityBaseType = getBaseEntity(xtextGraph, entityTypename, serviceNamespace);

                return EntityMergingContext.builder()
                  .typename(entityTypename)
                  .serviceNamespace(serviceNamespace)
                  .extensionSystemDefinition(entityTypeExtension)
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

  private void pruneInaccessibleInfo(XtextGraph xtextGraph, Collection<TypeDefinition> definitions) {
      definitions.forEach(typeDefinition -> {
          if(definitionContainsDirective(typeDefinition, FEDERATION_INACCESSIBLE_DIRECTIVE)) {
              xtextGraph.getTypes().remove(typeDefinition.getName());
              xtextGraph.getBlacklistedTypes().add(typeDefinition.getName());
          } else {
              getFieldDefinitions(typeDefinition, true)
                .removeIf(fieldDefinition -> definitionContainsDirective(fieldDefinition, FEDERATION_INACCESSIBLE_DIRECTIVE));
          }
      });
  }

  private void validateBaseExtensionCompatibility(EntityMergingContext entityMergingContext) {
      TypeDefinition baseType = entityMergingContext.getBaseType();
      TypeSystemDefinition typeSystemDefinition = entityMergingContext.getExtensionSystemDefinition();

      if(isTypeSystemForBaseType(typeSystemDefinition)) {
          TypeDefinition typeExtension = typeSystemDefinition.getType();

          if (!(isInterfaceTypeDefinition(baseType) && isInterfaceTypeDefinition(typeExtension)
                  || isObjectTypeDefinition(baseType) && isObjectTypeDefinition(typeExtension))) {
              throw new TypeConflictException(
                      format(
                              "Failed to merge entity extension to base type. typename%s, serviceNamespace=%s",
                              entityMergingContext.getTypename(),
                              entityMergingContext.getServiceNamespace()
                      )
              );
          }

          checkFederationTypeDirectives(typeExtension.getName(), typeExtension.getDirectives(), entityMergingContext);
      } else {
          TypeExtensionDefinition typeExtension = typeSystemDefinition.getTypeExtension();

          if (!(isInterfaceTypeDefinition(baseType) && isInterfaceTypeExtensionDefinition(typeExtension)
                  || isObjectTypeDefinition(baseType) && isObjectTypeExtensionDefinition(typeExtension))) {
              throw new TypeConflictException(
                      format(
                              "Incompatible type definitions for Entity extension and Entity base type . typename%s, serviceNamespace=%s",
                              entityMergingContext.getTypename(),
                              entityMergingContext.getServiceNamespace()
                      )
              );
          }

          checkFederationTypeDirectives(typeExtension.getName(),typeExtension.getDirectives(), entityMergingContext);
      }

      checkFederationFieldDirectives(entityMergingContext);
  }

  private void checkFederationFieldDirectives(EntityMergingContext entityMergingContext) {
      List<FieldDefinition> extFieldDefinitions = null;
      if(isTypeSystemForBaseType(entityMergingContext.getExtensionSystemDefinition())) {
          extFieldDefinitions = getFieldDefinitions(entityMergingContext.getExtensionSystemDefinition().getType());
      } else {
          extFieldDefinitions = getFieldDefinitions(entityMergingContext.getExtensionSystemDefinition().getTypeExtension());
      }

      List<String> baseFieldNames = getFieldDefinitions(entityMergingContext.getBaseType())
              .stream().map(FieldDefinition::getName).collect(Collectors.toList());

      extFieldDefinitions.forEach( fieldDefinition -> {
          boolean sharedField = baseFieldNames.contains(fieldDefinition.getName());
          AtomicBoolean hasExternalAtomicBoolean = new AtomicBoolean(false);

          fieldDefinition.getDirectives().forEach(directive -> {
              String directiveName = directive.getDefinition().getName();
              if (StringUtils.equals(directiveName, FEDERATION_EXTERNAL_DIRECTIVE)) {
                  externalValidator.validatePostMerge(entityMergingContext, fieldDefinition);
                  hasExternalAtomicBoolean.set(true);
              }
          });

          if(sharedField && !hasExternalAtomicBoolean.get()) {
              throw new SharedOwnershipException(fieldDefinition.getName());
          }
      });

  }

  private void checkFederationTypeDirectives(String typeName,List<Directive> typeDirectives, EntityMergingContext entityMergingContext) {
      typeDirectives.forEach(directive -> {
          String directiveName = directive.getDefinition().getName();
          boolean isInaccessible = StringUtils.equals(directiveName, FEDERATION_INACCESSIBLE_DIRECTIVE);
          if(isInaccessible && !StringUtils.equals(directiveName, FEDERATION_EXTENDS_DIRECTIVE)) {
              throw new TypeConflictException(String.format("%s type definition cannot have both the extends and inaccessible directive ", typeName));
          } else if(StringUtils.equals(directiveName, FEDERATION_KEY_DIRECTIVE)) {
              keyDirectiveValidator.validatePostMerge(entityMergingContext);
          }
      });
  }
}
