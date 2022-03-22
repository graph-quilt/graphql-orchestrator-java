package com.intuit.graphql.orchestrator.federation;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static org.apache.commons.collections4.CollectionUtils.containsAny;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.intuit.graphql.graphQL.TypeSystemDefinition;
import lombok.Builder;
import lombok.Getter;

public class EntityTypeMerger {

  public TypeDefinition mergeIntoBaseType(EntityMergingContext entityMergingContext) {
    merge(entityMergingContext);
    return entityMergingContext.getBaseType();
  }

  private void merge(EntityMergingContext entityMergingContext) {
    List<FieldDefinition> baseTypeFields = getFieldDefinitions(entityMergingContext.getBaseType());
    List<FieldDefinition> typeExtensionFields = null;
    if(entityMergingContext.getExtensionSystemDefinition().getType() != null) {
      typeExtensionFields = getFieldDefinitions(entityMergingContext.getExtensionSystemDefinition().getType());
    } else {
      typeExtensionFields = getFieldDefinitions(entityMergingContext.getExtensionSystemDefinition().getTypeExtension());
    }

    Set<String> baseTypeFieldNames =
        baseTypeFields.stream().map(FieldDefinition::getName).collect(Collectors.toSet());

    List<FieldDefinition> newFieldDefinitions =
        typeExtensionFields.stream()
            .filter(fieldDefinition -> !containsAny(baseTypeFieldNames, fieldDefinition.getName()))
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
