package com.intuit.graphql.orchestrator.utils;

import com.intuit.graphql.graphQL.ArgumentsDefinition;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InputObjectTypeDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeExtensionDefinition;
import com.intuit.graphql.graphQL.ListType;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeExtensionDefinition;
import com.intuit.graphql.graphQL.PrimitiveType;
import com.intuit.graphql.graphQL.ScalarTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeExtensionDefinition;
import com.intuit.graphql.graphQL.UnionTypeDefinition;
import com.intuit.graphql.graphQL.impl.ListTypeImpl;
import com.intuit.graphql.graphQL.impl.ObjectTypeImpl;
import com.intuit.graphql.graphQL.impl.PrimitiveTypeImpl;
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_INACCESSIBLE_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.definitionContainsDirective;
import static com.intuit.graphql.utils.XtextTypeUtils.getObjectType;
import static com.intuit.graphql.utils.XtextTypeUtils.isNonNull;
import static com.intuit.graphql.utils.XtextTypeUtils.isWrapped;
import static com.intuit.graphql.utils.XtextTypeUtils.typeName;
import static com.intuit.graphql.utils.XtextTypeUtils.unwrapAll;
import static com.intuit.graphql.utils.XtextTypeUtils.unwrapOne;
import static java.lang.String.format;

public class XtextTypeUtils {

  public static final String XTEXT_TYPE_FORMAT = "[name:%s, type:%s, description:%s]";

  public static boolean isPrimitiveType(NamedType type) {
    return type instanceof PrimitiveType;
  }

  public static boolean isListType(NamedType type) {
    return type instanceof ListType;
  }

  public static boolean isObjectType(NamedType type) {
    return type instanceof ObjectType;
  }

  public static boolean isEnumType(final TypeDefinition type) {
    return type instanceof EnumTypeDefinition;
  }

  public static boolean isScalarType(final TypeDefinition type) {
    return type instanceof ScalarTypeDefinition;
  }

  public static String getParentTypeName(FieldDefinition fieldDefinition) {
    Objects.requireNonNull(fieldDefinition, "fieldDefinition is required.");
    EObject eContainer = fieldDefinition.eContainer();
    if (eContainer instanceof TypeExtensionDefinition) {
      return ((TypeExtensionDefinition) eContainer).getName();
    } else {
      return ((TypeDefinition) eContainer).getName();
    }
  }

  public static String getNamedTypeName(NamedType namedType) {
    if(isObjectType(namedType)) {
      return ((ObjectType) namedType).getType().getName();
    } else if (isPrimitiveType(namedType)) {
      return ((PrimitiveType) namedType).getType();
    } else if( isListType(namedType)) {
      return  getNamedTypeName(((ListType) namedType).getType());
    } else {
      return null;
    }
  }

  /**
   * returns true if any type is a leaf node (scalar or enum)
   *
   * @param types the types to check
   * @return true if any type is a leaf node, or else false.
   */
  public static boolean isAnyTypeLeaf(TypeDefinition... types) {
    for (final TypeDefinition type : types) {
      if (isEnumType(type) || isScalarType(type)) {
        return true;
      }
    }
    return false;
  }

  public static NamedType createNamedType(TypeDefinition typeDefinition) {
    Objects.requireNonNull(typeDefinition, "typeDefinition is required.");
    ObjectType objectType = GraphQLFactoryDelegate.createObjectType();
    objectType.setType(typeDefinition);
    return objectType;
  }


  public static boolean isInterfaceOrUnionType(NamedType type) {
    final TypeDefinition typeDefinition = getObjectType(unwrapAll(type));
    if (Objects.nonNull(typeDefinition)) {
      return typeDefinition instanceof InterfaceTypeDefinition
          || typeDefinition instanceof UnionTypeDefinition;
    }
    return false;
  }

  public static List<FieldDefinition> getFieldDefinitions(TypeDefinition typeDefinition) {
    return getFieldDefinitions(typeDefinition, false);
  }

  public static List<FieldDefinition> getFieldDefinitions(TypeDefinition typeDefinition, boolean defaultList) {
    if (typeDefinition instanceof InterfaceTypeDefinition) {
      return ((InterfaceTypeDefinition)typeDefinition).getFieldDefinition();
    }
    if (typeDefinition instanceof ObjectTypeDefinition) {
      return ((ObjectTypeDefinition)typeDefinition).getFieldDefinition();
    }

    if(defaultList) {
      return new ArrayList<>();
    } else {
      String errorMessage = format("Failed to get fieldDefinitions for typeName=%s, typeInstance=%s",
              typeDefinition.getName(), typeDefinition.getClass().getName());
      throw new IllegalArgumentException(errorMessage);
    }
  }

  public static List<FieldDefinition> getFieldDefinitions(TypeExtensionDefinition typeDefinition) {
    return getFieldDefinitions(typeDefinition, false);
  }

  public static List<FieldDefinition> getFieldDefinitions(TypeExtensionDefinition typeDefinition, boolean defaultList) {
    if (typeDefinition instanceof ObjectTypeExtensionDefinition) {
      return ((ObjectTypeExtensionDefinition) typeDefinition).getFieldDefinition();
    }
    if (typeDefinition instanceof InterfaceTypeExtensionDefinition) {
      return ((InterfaceTypeExtensionDefinition) typeDefinition).getFieldDefinition();
    }

    if(defaultList) {
      return new ArrayList<>();
    } else {
      String errorMessage = format("Failed to get fieldDefinitions for typeName=%s, typeInstance=%s",
              typeDefinition.getName(), typeDefinition.getClass().getName());
      throw new IllegalArgumentException(errorMessage);
    }
  }

  public static boolean compareTypes(NamedType lType, NamedType rType) {
    if (isNonNull(lType) != isNonNull(rType) || isWrapped(lType) != isWrapped(rType)) {
      return false;
    }
    if (isWrapped(lType) && isWrapped(rType)) {
      return compareTypes(unwrapOne(lType), unwrapOne(rType));
    }
    return StringUtils.equals(typeName(lType), typeName(rType));
  }

  public static boolean isCompatible(NamedType stubType, NamedType targetType) {
    if (isWrapped(stubType) != isWrapped(targetType)) {
      return false;
    }
    if (isWrapped(stubType) && isWrapped(targetType)) {
      return isCompatible(unwrapOne(stubType), unwrapOne(targetType));
    }
    if (isNonNull(stubType) && !isNonNull(targetType)) {
      // e.g. stubType=A! targetType=A
      // subtype should be less restrictive
      return false;
    }
    return StringUtils.equals(typeName(stubType), typeName(targetType));
  }

  public static String toDescriptiveString(TypeDefinition typeDefinition) {
    return format(XTEXT_TYPE_FORMAT, typeDefinition.getName(), typeDefinition.eClass().getName(),
            typeDefinition.getDesc());
  }

  public static String toDescriptiveString(NamedType namedType) {
    TypeDefinition objectType = com.intuit.graphql.utils.XtextTypeUtils.getObjectType(namedType);
    return Objects.nonNull(objectType) ? toDescriptiveString(objectType)
            : format(XTEXT_TYPE_FORMAT, com.intuit.graphql.utils.XtextTypeUtils.typeName(namedType), StringUtils.EMPTY, StringUtils.EMPTY);
  }

  public static String toDescriptiveString(ArgumentsDefinition argumentsDefinition) {
    if (Objects.nonNull(argumentsDefinition)) {
      StringBuilder result = new StringBuilder();
      result.append("[");
      result.append(argumentsDefinition.getInputValueDefinition().stream().map(Objects::toString)
          .collect(Collectors.joining(",")));
      result.append("]");
      return result.toString();
    }
    return StringUtils.EMPTY;
  }

  public static void checkFieldsCompatibility(final TypeDefinition existingTypeDefinition, final TypeDefinition conflictingTypeDefinition,
                                              boolean existingTypeIsEntity, boolean conflictingTypeisEntity, boolean federatedComparison) {
    List<FieldDefinition> existingFieldDefinitions = getFieldDefinitions(existingTypeDefinition);
    Map<String,FieldDefinition> possibleConflictingFieldMap = getFieldDefinitions(conflictingTypeDefinition)
            .stream().collect(Collectors.toMap(FieldDefinition::getName, Function.identity()));

    for(FieldDefinition existingDefinition : existingFieldDefinitions) {
      FieldDefinition possibleConflictingSharedField = possibleConflictingFieldMap.get(existingDefinition.getName());
      if(possibleConflictingSharedField != null) {
        if(areCompatibleSharedFields(existingDefinition, possibleConflictingSharedField)) {
          //removes from map since we know that it is shared and compatible
          // doing so to leave behind only unique fields that need to be checked
          possibleConflictingFieldMap.remove(possibleConflictingSharedField.getName());
        } else {
          throw new TypeConflictException(
                  format("Type %s is conflicting with existing type %s. Both types must be able to resolve %s",
                          toDescriptiveString(existingTypeDefinition),
                          toDescriptiveString(conflictingTypeDefinition),
                          possibleConflictingSharedField.getName()
                  )
          );
        }
      } else if(!federatedComparison && !doesTypeAllowUniqueFields(existingTypeDefinition, existingTypeIsEntity)
              && !isInaccessible(existingDefinition)) {
        throw new TypeConflictException(
                format("Type %s is conflicting with existing type %s. Type '%s' does not contain field %s",
                        toDescriptiveString(conflictingTypeDefinition),
                        toDescriptiveString(existingTypeDefinition),
                        toDescriptiveString(conflictingTypeDefinition),
                        existingDefinition.getName()
                )
        );
      }
    }

    if(federatedComparison) {
      if(isEntity(conflictingTypeDefinition) != isEntity(existingTypeDefinition)) {
        throw new TypeConflictException("Type %s is conflicting with existing type %s. Only one of the types are an entity.");
      }

      for(FieldDefinition fieldDefinition :possibleConflictingFieldMap.values()) {
        if(!doesTypeAllowUniqueFields(conflictingTypeDefinition, existingTypeIsEntity) && !isInaccessible(fieldDefinition)) {
          throw new TypeConflictException(
                  format("Type %s is conflicting with existing type %s. Type '%s' does not contain field %s",
                          toDescriptiveString(conflictingTypeDefinition),
                          toDescriptiveString(existingTypeDefinition),
                          toDescriptiveString(conflictingTypeDefinition),
                          fieldDefinition.getName()
                  ));
        }
      }
    }
  }

  public static String toDescriptiveString(EList<Directive> directives) {
    if (Objects.nonNull(directives)) {
      StringBuilder result = new StringBuilder();
      result.append("[");
      result.append(
              directives.stream().map(dir -> dir.getDefinition()).map(Objects::toString).collect(Collectors.joining(",")));
      result.append("]");
      return result.toString();
    }
    return StringUtils.EMPTY;
  }

  public static boolean isValidInputType(NamedType namedType) {
    //input fields are either scalars, enums, or other input objects
    NamedType unwrappedNamedType = unwrapAll(namedType);
    if (isObjectType(unwrappedNamedType)) {
      TypeDefinition objectType = com.intuit.graphql.utils.XtextTypeUtils.getObjectType(unwrappedNamedType);
      return (objectType instanceof InputObjectTypeDefinition) ||
              (objectType instanceof ScalarTypeDefinition) ||
              (objectType instanceof EnumTypeDefinition);
    }
    return true;
  }

  public static boolean isEntity(final TypeDefinition type) {
    return definitionContainsDirective(type, FEDERATION_KEY_DIRECTIVE);
  }

  public static boolean isInaccessible(final TypeDefinition type) {
    return definitionContainsDirective(type, FEDERATION_INACCESSIBLE_DIRECTIVE);
  }

  public static boolean isInaccessible(final FieldDefinition fieldDefinition) {
    return definitionContainsDirective(fieldDefinition, FEDERATION_INACCESSIBLE_DIRECTIVE);
  }

  private static boolean doesTypeAllowUniqueFields(TypeDefinition typeDefinition, boolean isEntity) {
    return isEntity || typeDefinition instanceof InterfaceTypeDefinition
            || typeDefinition instanceof EnumTypeDefinition
            || typeDefinition instanceof UnionTypeDefinition;
  }

  public static boolean areCompatibleSharedFields(FieldDefinition fieldDefinition1, FieldDefinition fieldDefinition2) {
    return
            areCompatibleTypes(fieldDefinition1.getNamedType(), fieldDefinition2.getNamedType());
  }

  public static boolean areCompatibleTypes(NamedType namedType1, NamedType namedType2) {
    String type1 = null, type2 = null;
    boolean compatible = false;

    if(namedType1 instanceof PrimitiveTypeImpl && namedType2 instanceof PrimitiveTypeImpl) {
      type1 = ((PrimitiveTypeImpl)namedType1).getType();
      type2 = ((PrimitiveTypeImpl)namedType2).getType();
      compatible = type1.equals(type2);
    } else if(namedType1 instanceof ObjectTypeImpl && namedType2 instanceof ObjectTypeImpl) {
      type1 = ((ObjectTypeImpl)namedType1).getType().getName();
      type2 = ((ObjectTypeImpl)namedType1).getType().getName();
      compatible = type1.equals(type2);
    } else if(namedType1 instanceof ListTypeImpl && namedType2 instanceof ListTypeImpl) {
      NamedType listType1 = ((ListTypeImpl)namedType1).getType();
      NamedType listType2 = ((ListTypeImpl)namedType2).getType();

      compatible = areCompatibleTypes(listType1, listType2);
    }

    return compatible;
  }

  public static boolean isObjectTypeDefinition(TypeDefinition typeDefinition) {
    return Objects.nonNull(typeDefinition) && typeDefinition instanceof ObjectTypeDefinition;
  }

  public static boolean isInterfaceTypeDefinition(TypeDefinition typeDefinition) {
    return Objects.nonNull(typeDefinition) && typeDefinition instanceof InterfaceTypeDefinition;

  }

  public static boolean isObjectTypeExtensionDefinition(TypeExtensionDefinition typeDefinition) {
    return Objects.nonNull(typeDefinition) && typeDefinition instanceof ObjectTypeExtensionDefinition;
  }

  public static boolean isInterfaceTypeExtensionDefinition(TypeExtensionDefinition typeDefinition) {
    return Objects.nonNull(typeDefinition) && typeDefinition instanceof InterfaceTypeExtensionDefinition;
  }

  public static boolean objectTypeContainsFieldWithName(TypeDefinition typeDefinition, String fieldName) {
    return getFieldDefinitions(typeDefinition, true)
            .stream()
            .map(FieldDefinition::getName)
            .anyMatch(fieldDefinition -> fieldDefinition.equals(fieldName));
  }
  public static FieldDefinition getFieldDefinitionFromObjectTypeDefinition(TypeDefinition typeDefinition, String fieldName) {
    return getFieldDefinitions(typeDefinition, true)
            .stream()
            .filter(fieldDefinition -> fieldDefinition.getName().equals(fieldName))
            .findFirst()
            .orElse(null);
  }
}
