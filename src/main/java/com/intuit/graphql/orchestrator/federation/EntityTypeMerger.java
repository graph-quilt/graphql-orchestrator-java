package com.intuit.graphql.orchestrator.federation;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.RESOLVER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_EXTERNAL_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.isTypeSystemForBaseType;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.isTypeSystemForExtensionType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.definitionContainsDirective;
import static org.apache.commons.collections4.CollectionUtils.containsAny;

@Slf4j
public class EntityTypeMerger {

  public TypeDefinition mergeIntoBaseType(EntityMergingContext entityMergingContext, UnifiedXtextGraph unifiedXtextGraph) {
    pruneConflictingResolverInfo(entityMergingContext, unifiedXtextGraph);
    merge(entityMergingContext);
    return entityMergingContext.getBaseType();
  }

  private void pruneConflictingResolverInfo(EntityMergingContext entityMergingContext, UnifiedXtextGraph unifiedXtextGraph) {
    List<FieldDefinition> entityFieldDefinitions = null;

    if(isTypeSystemForExtensionType(entityMergingContext.getExtensionSystemDefinition())) {
      entityFieldDefinitions = getFieldDefinitions(entityMergingContext.getExtensionSystemDefinition().getTypeExtension());
    } else {
      entityFieldDefinitions = getFieldDefinitions(entityMergingContext.getExtensionSystemDefinition().getType());
    }

    entityFieldDefinitions
    .stream()
    .filter(entityFieldDefinition-> !definitionContainsDirective(entityFieldDefinition, FEDERATION_EXTERNAL_DIRECTIVE))
    .forEach(newEntityField -> {
      getFieldDefinitions(entityMergingContext.getBaseType()).removeIf(preexistingField ->
        definitionContainsDirective(preexistingField, RESOLVER_DIRECTIVE_NAME)
        && preexistingField.getName().equals(newEntityField.getName())
      );

      unifiedXtextGraph.getFieldResolverContexts().removeIf(fieldResolverContext ->
        fieldResolverContext.getParentTypename().equals(entityMergingContext.getTypename())
        && fieldResolverContext.getFieldName().equals(newEntityField.getName())
      );

      unifiedXtextGraph.getFederationMetadataByNamespace()
      .entrySet()
      .stream()
      .filter(entrySet -> !entrySet.getKey().equals(entityMergingContext.getServiceNamespace()))
      .map(Map.Entry::getValue)
      .map(federationMetadata -> federationMetadata.getEntityMetadataByName(entityMergingContext.getTypename()))
      .filter(Objects::nonNull)
      .forEach(entityMetadata -> entityMetadata.getFields().remove(newEntityField.getName()));
    });
  }

  private void merge(EntityMergingContext entityMergingContext) {
    List<FieldDefinition> baseTypeFields = getFieldDefinitions(entityMergingContext.getBaseType());
    List<FieldDefinition> typeExtensionFields = null;
    if(isTypeSystemForBaseType(entityMergingContext.getExtensionSystemDefinition())) {
      typeExtensionFields = getFieldDefinitions(entityMergingContext.getExtensionSystemDefinition().getType());
    } else {
      typeExtensionFields = getFieldDefinitions(entityMergingContext.getExtensionSystemDefinition().getTypeExtension());
    }

    Set<String> baseTypeFieldNames =
        baseTypeFields.stream().map(FieldDefinition::getName).collect(Collectors.toSet());

    List<FieldDefinition> newFieldDefinitions =
        typeExtensionFields.stream()
            .filter(fieldDefinition -> !containsAny(baseTypeFieldNames, fieldDefinition.getName()))
            .peek(fieldDefinition -> {
              if(log.isDebugEnabled()) {
                log.debug("Service {} added field {} to entity {}",
                        entityMergingContext.serviceNamespace,
                        fieldDefinition.getName(),
                        entityMergingContext.typename
                );
              }
            })
            .collect(Collectors.toList());
    baseTypeFields.addAll(newFieldDefinitions);
  }

  @Builder
  @Getter
  public static class EntityMergingContext {
    private final String typename;
    private final String serviceNamespace;
    private final TypeDefinition baseType;
    private final TypeSystemDefinition extensionSystemDefinition;
  }
}
