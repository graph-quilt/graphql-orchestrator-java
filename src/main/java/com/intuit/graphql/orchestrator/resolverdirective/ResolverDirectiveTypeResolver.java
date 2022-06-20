package com.intuit.graphql.orchestrator.resolverdirective;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isEnumType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isPrimitiveType;
import static com.intuit.graphql.utils.XtextTypeUtils.isListType;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.transform.ResolverArgumentListTypeNotSupported;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;

/**
 * This class helps break up {@link ResolverArgumentDirectiveValidator} and provides the ability to get the leaf
 * TypeDefinition from a dot-separated path.
 */
public class ResolverDirectiveTypeResolver {

  /**
   * Returns the TypeDefinition found at the leaf node of a dot-separated path (e.g. "a.b.c"), assuming Query is at
   * root.
   *
   * @param field        dot-separated path where the root type exists (assuming starting from query root)
   * @param source       the graph where the field exists
   * @param argumentName the argument name (for exception building)
   * @param rootContext  the FieldContext of the field with the argument (for exception building)
   * @return a TypeDefinition that represents the leaf node of the path.
   * @throws ResolverArgumentFieldRootObjectDoesNotExist exception is thrown when any fieldDefinition in field path does
   * not exist in source graph
   * @throws ResolverArgumentListTypeNotSupported exception is thrown when any type requested in field path is a List
   * @throws ResolverArgumentPrematureLeafType exception is thrown when a terminal type (Scalar or Enum) is encountered,
   * but it is not the leaf TypeDefinition
   */
  public TypeDefinition resolveField(final String field, final UnifiedXtextGraph source, final String argumentName,
      final FieldContext rootContext)
      throws ResolverArgumentFieldRootObjectDoesNotExist, ResolverArgumentListTypeNotSupported, ResolverArgumentPrematureLeafType {
    final String[] subFields = field.split("\\.");

    //never null QueryType?
    TypeDefinition currentType = source.getOperationType(Operation.QUERY);

    /*
     start at query

     for every subfield a.b.c i check to see if there exists a type definition.
     */
    for (int i = 0; i < subFields.length; i++) {
      final String subField = subFields[i];

      NamedType type = ((ObjectTypeDefinition) currentType).getFieldDefinition().stream()
          .filter(fieldDefinition -> fieldDefinition.getName().equals(subField))
          .map(FieldDefinition::getNamedType)
          .findFirst()
          .orElse(null);

      //check if field definition exists
      if (type == null) {
        throw new ResolverArgumentFieldRootObjectDoesNotExist(argumentName, rootContext, subField);
      }

      //if it is list, throw error not supported
      if (isListType(type)) {
        throw new ResolverArgumentListTypeNotSupported(argumentName, rootContext, subField);
      }

      //if primitive, ensure that it is not a premature scalar
      if (isPrimitiveType(type) && i < subFields.length - 1) {
        throw new ResolverArgumentPrematureLeafType(argumentName, "ScalarTypeDefinition", rootContext, subField);
      }

      currentType = source.getType(type);

      //if enum, ensure it is not premature
      if (isEnumType(currentType) && i < subFields.length - 1) {
        throw new ResolverArgumentPrematureLeafType(argumentName, "EnumTypeDefinition", rootContext, subField);
      }
    }

    return currentType;
  }
}
