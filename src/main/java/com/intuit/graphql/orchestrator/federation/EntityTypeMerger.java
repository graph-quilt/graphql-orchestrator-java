package com.intuit.graphql.orchestrator.federation;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isInterfaceTypeDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isObjectTypeDefinition;
import static org.apache.commons.collections4.CollectionUtils.containsAny;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.extendsdirective.EntityExtension;
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityTypeMerger {

  public void mergeIntoBaseType(EntityExtension entityExtension) {
    TypeDefinition baseType = entityExtension.getBaseType();
    TypeDefinition typeExtension = entityExtension.getTypeExtension();

    // specification: directive @key(fields: _FieldSet!) repeatable on OBJECT | INTERFACE
    if (isInterfaceTypeDefinition(baseType) && isInterfaceTypeDefinition(typeExtension)) {
      List<FieldDefinition> baseTypeFields = ((InterfaceTypeDefinition) baseType).getFieldDefinition();
      List<FieldDefinition> typeExtensionFields = ((InterfaceTypeDefinition) typeExtension).getFieldDefinition();
      merge(baseTypeFields, typeExtensionFields);
    } else if (isObjectTypeDefinition(baseType) && isObjectTypeDefinition(typeExtension)) {
      List<FieldDefinition> baseTypeFields = ((ObjectTypeDefinition) baseType).getFieldDefinition();
      List<FieldDefinition> typeExtensionFields = ((ObjectTypeDefinition) typeExtension).getFieldDefinition();
      merge(baseTypeFields, typeExtensionFields);
    } else {
      // TODO this should be throw somewhere
      throw new TypeConflictException("");
    }
  }

  private void merge(
      List<FieldDefinition> baseTypeFields, List<FieldDefinition> typeExtensionFields) {
    // TODO
    // Assumption: in TypeConflictResolver, checkEntityTypesCanBeMerged(entityBaseType,
    // entityTypeExtension);
    //   @key is subset
    //   @external fields are vlid
    //   @require fields are valid
    //   new fields are not present in base type
    Set<String> baseTypeFieldNames = baseTypeFields.stream()
        .map(fieldDefinition -> fieldDefinition.getName())
        .collect(Collectors.toSet());

    List<FieldDefinition> newFieldDefinitions =
        typeExtensionFields.stream()
            .filter(fieldDefinition -> !containsAny(baseTypeFieldNames, fieldDefinition.getName()))
            .collect(Collectors.toList());
    baseTypeFields.addAll(newFieldDefinitions);
  }

}
