package com.intuit.graphql.orchestrator.federation;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isInterfaceTypeDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isObjectTypeDefinition;
import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.containsAny;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.intuit.graphql.orchestrator.utils.FederationUtils;
import com.intuit.graphql.orchestrator.utils.GraphQLUtil;
import lombok.Builder;
import lombok.Getter;

public class EntityTypeMerger {

  public TypeDefinition mergeIntoBaseType(EntityMergingContext entityMergingContext) {
    TypeDefinition baseType = entityMergingContext.getBaseType();
    TypeDefinition typeExtension = entityMergingContext.getTypeExtension();
    checkEntityTypesCanBeMerged(entityMergingContext);
    merge(getFieldDefinitions(baseType), getFieldDefinitions(typeExtension));
    return baseType;
  }

  private void checkEntityTypesCanBeMerged(EntityMergingContext entityMergingContext) {
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

    checkExtensionKeysAreSubset(baseType, typeExtension, entityMergingContext.getTypename(),
            entityMergingContext.getServiceNamespace());

    // TODO check if this is being done already
    //   new fields are not present in base type
  }

  private void checkExtensionKeysAreSubset(TypeDefinition baseEntity, TypeDefinition extEntity, String extTypeName, String extServiceNamespace) {
    List<String> baseEntityKeys = getKeyDirectives(baseEntity).stream()
            .map(this::getDirectiveFieldSet)
            .map(GraphQLUtil::getUniqueIdFromFieldSet)
            .collect(Collectors.toList());

    List<String> subsetKeys = getKeyDirectives(extEntity).stream()
            .map(this::getDirectiveFieldSet)
            .map(GraphQLUtil::getUniqueIdFromFieldSet)
            .collect(Collectors.toList());

    if(!baseEntityKeys.containsAll(subsetKeys)) {
      String incompatibleKeyMergeErrorMsg = "Failed to merge entity extension to base type. Defined keys do not exist in base entity. typename%s, serviceNamespace=%s";
      throw new TypeConflictException(format(incompatibleKeyMergeErrorMsg, extTypeName, extServiceNamespace));
    }
  }

  //todo once requires case is merged in use the getDirectives method; not doing now to avoid conflict
  private List<Directive> getKeyDirectives(TypeDefinition entity) {
    return entity.getDirectives().stream()
            .filter(directive -> directive.getDefinition().getName().equals(FederationUtils.FEDERATION_KEY_DIRECTIVE))
            .collect(Collectors.toList());
  }

  private String getDirectiveFieldSet(Directive directive) {
    return directive.getArguments().get(0).getValueWithVariable().getStringValue();
  }

  private void merge(
      List<FieldDefinition> baseTypeFields, List<FieldDefinition> typeExtensionFields) {
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
    private final TypeDefinition typeExtension;
  }
}
