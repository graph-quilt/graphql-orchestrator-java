package com.intuit.graphql.orchestrator.xtext;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.ArgumentsDefinition;
import com.intuit.graphql.graphQL.ArrayValueWithVariable;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.DirectiveDefinition;
import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.EnumValueDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.GraphQLFactory;
import com.intuit.graphql.graphQL.InputObjectTypeDefinition;
import com.intuit.graphql.graphQL.InputValueDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ListType;
import com.intuit.graphql.graphQL.ObjectFieldWithVariable;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectValueWithVariable;
import com.intuit.graphql.graphQL.PrimitiveType;
import com.intuit.graphql.graphQL.ScalarTypeDefinition;
import com.intuit.graphql.graphQL.UnionMemberShip;
import com.intuit.graphql.graphQL.UnionMembers;
import com.intuit.graphql.graphQL.UnionTypeDefinition;
import com.intuit.graphql.graphQL.Value;
import com.intuit.graphql.graphQL.ValueWithVariable;

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

  public static PrimitiveType createPrimitiveType() {
    return instance.createPrimitiveType();
  }

  public static ListType createListType() {
    return instance.createListType();
  }

  public static InputValueDefinition createInputValueDefinition() {
    return instance.createInputValueDefinition();
  }


}