package com.intuit.graphql.orchestrator.utils;

import static com.intuit.graphql.utils.XtextTypeUtils.getObjectType;
import static com.intuit.graphql.utils.XtextTypeUtils.unwrapAll;

import com.intuit.graphql.graphQL.*;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;

import java.util.List;
import java.util.Objects;
import org.eclipse.emf.ecore.EObject;

public class XtextTypeUtils {

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
    if (typeDefinition instanceof InterfaceTypeDefinition) {
      return ((InterfaceTypeDefinition)typeDefinition).getFieldDefinition();
    }
    if (typeDefinition instanceof ObjectTypeDefinition) {
      return ((ObjectTypeDefinition)typeDefinition).getFieldDefinition();
    }
    String errorMessage = String.format("Failed to get fieldDefinitions for typeName=%s, typeInstance=%s",
            typeDefinition.getName(), typeDefinition.getClass().getName());
    throw new IllegalArgumentException(errorMessage);
  }
}
