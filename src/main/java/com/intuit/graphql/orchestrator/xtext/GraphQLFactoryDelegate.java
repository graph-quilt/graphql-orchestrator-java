package com.intuit.graphql.orchestrator.xtext;

import com.intuit.graphql.graphQL.*;

/**
 * This class provides an easier-to-read pattern for Xtext GraphQL object generation using the instance {@code
 * GraphQLFactory}.
 *
 * This class allows you to statically import each Xtext factory method without the need to explicitly reference the
 * static instance.
 *
 * For example, {@code GraphQLFactory.eInstance.createScalarTypeDefinition()} will be {@code
 * createScalarTypeDefinition()} with static imports.
 */
public class GraphQLFactoryDelegate {

  private static final GraphQLFactory instance = GraphQLFactory.eINSTANCE;

  private GraphQLFactoryDelegate() {

  }

  public static ScalarTypeDefinition createScalarTypeDefinition() {
    return instance.createScalarTypeDefinition();
  }

  public static ObjectTypeDefinition createObjectTypeDefinition() {
    return instance.createObjectTypeDefinition();
  }

  public static InputObjectTypeDefinition createInputObjectTypeDefinition() {
    return instance.createInputObjectTypeDefinition();
  }

  public static InterfaceTypeDefinition createInterfaceTypeDefinition() {
    return instance.createInterfaceTypeDefinition();
  }

  public static UnionTypeDefinition createUnionTypeDefinition() {
    return instance.createUnionTypeDefinition();
  }

  public static UnionMembers createUnionMembers() {
    return instance.createUnionMembers();
  }

  public static UnionMemberShip createUnionMembership() {
    return instance.createUnionMemberShip();
  }

  public static EnumTypeDefinition createEnumTypeDefinition() {
    return instance.createEnumTypeDefinition();
  }

  public static EnumValueDefinition createEnumValueDefinition() {
    return instance.createEnumValueDefinition();
  }

  public static FieldDefinition createFieldDefinition() {
    return instance.createFieldDefinition();
  }

  public static ArgumentsDefinition createArgumentsDefinition() {
    return instance.createArgumentsDefinition();
  }

  public static Argument createArgument() {
    return instance.createArgument();
  }

  public static Value createValue() {
    return instance.createValue();
  }

  public static ValueWithVariable createValueWithVariable() {
    return instance.createValueWithVariable();
  }

  public static ArrayValueWithVariable createArrayValueWithVariable() {
    return instance.createArrayValueWithVariable();
  }

  public static ObjectValueWithVariable createObjectValueWithVariable() {
    return instance.createObjectValueWithVariable();
  }

  public static ObjectFieldWithVariable createObjectFieldWithVariable() {
    return instance.createObjectFieldWithVariable();
  }

  public static Directive createDirective() {
    return instance.createDirective();
  }

  public static DirectiveDefinition createDirectiveDefinition() {
    return instance.createDirectiveDefinition();
  }

  public static ObjectType createObjectType() {
    return instance.createObjectType();
  }

  public static ListType createListType() {
    return instance.createListType();
  }
}