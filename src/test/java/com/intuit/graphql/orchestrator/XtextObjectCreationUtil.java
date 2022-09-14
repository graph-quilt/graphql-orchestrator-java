package com.intuit.graphql.orchestrator;

import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createDirectiveDefinition;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createEnumTypeDefinition;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createEnumValueDefinition;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createFieldDefinition;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createInterfaceTypeDefinition;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createInterfaceTypeExtensionDefinition;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createObjectTypeExtensionDefinition;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createUnionMembers;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createUnionMembership;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createUnionTypeDefinition;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.DirectiveDefinition;
import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.EnumValueDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeExtensionDefinition;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeExtensionDefinition;
import com.intuit.graphql.graphQL.UnionMemberShip;
import com.intuit.graphql.graphQL.UnionMembers;
import com.intuit.graphql.graphQL.UnionTypeDefinition;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;

public class XtextObjectCreationUtil {

  public static Directive buildDirective(
      DirectiveDefinition directiveDefinition, List<Argument> arguments) {

    Directive directive = GraphQLFactoryDelegate.createDirective();
    directive.setDefinition(directiveDefinition);

    if (CollectionUtils.isNotEmpty(arguments)) {
      arguments.forEach(argument -> directive.getArguments().add(argument));
    }

    return directive;
  }

  public static DirectiveDefinition buildDirectiveDefinition(String name) {
    DirectiveDefinition directiveDefinition = createDirectiveDefinition();
    directiveDefinition.setName(name);
    return directiveDefinition;
  }

  public static FieldDefinition buildFieldDefinition(String name) {
    FieldDefinition fieldDefinition = createFieldDefinition();
    fieldDefinition.setName(name);
    return fieldDefinition;
  }

  public static FieldDefinition buildFieldDefinition(String name, List<Directive> directives) {
    FieldDefinition fieldDefinition = buildFieldDefinition(name);
    fieldDefinition.getDirectives().addAll(directives);
    return fieldDefinition;
  }

  public static ObjectTypeDefinition buildObjectTypeDefinition(String name, List<FieldDefinition> fieldDefinitions) {
    ObjectTypeDefinition objectTypeDefinition = createObjectTypeDefinition();
    objectTypeDefinition.setName(name);

    objectTypeDefinition.getFieldDefinition().addAll(fieldDefinitions);
    return objectTypeDefinition;
  }

  public static ObjectTypeDefinition buildObjectTypeDefinition(String name) {
    ObjectTypeDefinition objectTypeDefinition = createObjectTypeDefinition();
    objectTypeDefinition.setName(name);
    return objectTypeDefinition;
  }

  public static ObjectTypeExtensionDefinition buildObjectTypeExtensionDefinition(String name, List<FieldDefinition> fieldDefinitions) {
    ObjectTypeExtensionDefinition objectTypeDefinition = buildObjectTypeExtensionDefinition(name);

    objectTypeDefinition.getFieldDefinition().addAll(fieldDefinitions);
    return objectTypeDefinition;
  }

  public static ObjectTypeExtensionDefinition buildObjectTypeExtensionDefinition(String name) {
    ObjectTypeExtensionDefinition objectTypeDefinition = createObjectTypeExtensionDefinition();
    objectTypeDefinition.setName(name);
    return objectTypeDefinition;
  }

  public static InterfaceTypeDefinition buildInterfaceTypeDefinition(String name, List<FieldDefinition> fieldDefinitions) {
    InterfaceTypeDefinition interfaceTypeDefinition = createInterfaceTypeDefinition();
    interfaceTypeDefinition.setName(name);
    interfaceTypeDefinition.getFieldDefinition().addAll(fieldDefinitions);
    return interfaceTypeDefinition;
  }

  public static InterfaceTypeExtensionDefinition buildInterfaceTypeExtensionDefinition(String name, List<FieldDefinition> fieldDefinitions) {
    InterfaceTypeExtensionDefinition interfaceTypeDefinition = buildInterfaceTypeExtensionDefinition(name);

    interfaceTypeDefinition.getFieldDefinition().addAll(fieldDefinitions);
    return interfaceTypeDefinition;
  }

  public static InterfaceTypeExtensionDefinition buildInterfaceTypeExtensionDefinition(String name) {
    InterfaceTypeExtensionDefinition interfaceTypeDefinition = createInterfaceTypeExtensionDefinition();
    interfaceTypeDefinition.setName(name);
    return interfaceTypeDefinition;
  }

  public static UnionTypeDefinition buildUnionTypeDefinition(String name, List<NamedType> memberNamedTypes) {
    UnionMembers unionMembers = createUnionMembers();
    unionMembers.getNamedUnion().addAll(memberNamedTypes);

    UnionMemberShip unionMemberShip = createUnionMembership();
    unionMemberShip.setUnionMembers(unionMembers);

    UnionTypeDefinition unionTypeDefinition = createUnionTypeDefinition();
    unionTypeDefinition.setName(name);
    unionTypeDefinition.setUnionMemberShip(unionMemberShip);
    return unionTypeDefinition;
  }

  public static EnumTypeDefinition buildEnumTypeDefinition(String name, String... enumValues) {
    List<EnumValueDefinition> enumValueDefinitions = Stream.of(enumValues)
        .map(enumValue -> {
          EnumValueDefinition enumValueDefinition = createEnumValueDefinition();
          enumValueDefinition.setEnumValue(enumValue);
          return enumValueDefinition;
        })
        .collect(Collectors.toList());

    EnumTypeDefinition enumTypeDefinition = createEnumTypeDefinition();
    enumTypeDefinition.setName(name);
    enumTypeDefinition.getEnumValueDefinition().addAll(enumValueDefinitions);
    return enumTypeDefinition;
  }

}
