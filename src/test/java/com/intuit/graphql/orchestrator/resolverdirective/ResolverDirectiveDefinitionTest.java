package com.intuit.graphql.orchestrator.resolverdirective;

import static com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinitionTestUtil.createResolverArguments;
import static com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinitionTestUtil.createResolverField;
import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import org.junit.Test;

public class ResolverDirectiveDefinitionTest {

  @Test
  public void canCreateResolverDirectiveDefinitionTest() {
    String resolverFieldName = "testField";
    String resolverArgumentName1 = "testFieldArg1";
    String resolverArgumentValue1 = "$testFieldValue2";

    String resolverArgumentName2 = "testFieldArg1";
    String resolverArgumentValue2 = "$testFieldValue2";

    Argument resolverField = createResolverField(resolverFieldName);
    Argument resolverArguments = createResolverArguments(resolverArgumentName1, resolverArgumentValue1,
        resolverArgumentName2, resolverArgumentValue2);

    Directive directive = GraphQLFactoryDelegate.createDirective();
    directive.getArguments().add(resolverField);
    directive.getArguments().add(resolverArguments);

    ResolverDirectiveDefinition resolverDirectiveDefinition = ResolverDirectiveDefinition.from(directive);

    assertThat(resolverDirectiveDefinition.getField()).isEqualTo(resolverFieldName);
    ResolverArgumentDefinition resolverArgumentEntry1 = resolverDirectiveDefinition.getArguments().get(0);
    assertThat(resolverArgumentEntry1.getName()).isEqualTo(resolverArgumentName1);
    assertThat(resolverArgumentEntry1.getValue()).isEqualTo(resolverArgumentValue1);

    ResolverArgumentDefinition resolverArgumentEntry2 = resolverDirectiveDefinition.getArguments().get(1);
    assertThat(resolverArgumentEntry2.getName()).isEqualTo(resolverArgumentName2);
    assertThat(resolverArgumentEntry2.getValue()).isEqualTo(resolverArgumentValue2);
  }

  @Test(expected = ResolverDirectiveException.class)
  public void unexpectedArgumentForResolverDirectiveDefinitionTest() {
    // FootType has testField.  testField has @resolver(field: "resolverField", argument: [...]])
    // argument is not valid.
    String resolverFieldName = "resolverField";

    Argument resolverArguments = GraphQLFactoryDelegate.createArgument();
    resolverArguments.setName("argument"); // should be arguments

    Argument resolverField = createResolverField(resolverFieldName);

    Directive directive = GraphQLFactoryDelegate.createDirective();
    directive.getArguments().add(resolverField);
    directive.getArguments().add(resolverArguments);

    // container
    ObjectTypeDefinition fooObjType = GraphQLFactoryDelegate.createObjectTypeDefinition();
    fooObjType.setName("FooType");

    ObjectType objectType = GraphQLFactoryDelegate.createObjectType();
    objectType.setType(fooObjType);

    FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition();
    fieldDefinition.setName("testField");
    fieldDefinition.setNamedType(objectType);
    fieldDefinition.getDirectives().add(directive);
    ResolverDirectiveDefinition.from(directive);

  }

  @Test(expected = ResolverDirectiveException.class)
  public void unexpectedFieldForResolverDirectiveDefinitionTest() {
    String resolverFieldName = ""; // cannot be empty
    String resolverArgumentName1 = "testFieldArg1";
    String resolverArgumentValue1 = "$testFieldValue2";

    String resolverArgumentName2 = "testFieldArg1";
    String resolverArgumentValue2 = "$testFieldValue2";

    Argument resolverField = createResolverField(resolverFieldName);
    Argument resolverArguments = createResolverArguments(resolverArgumentName1, resolverArgumentValue1,
        resolverArgumentName2, resolverArgumentValue2);

    Directive directive = GraphQLFactoryDelegate.createDirective();
    directive.getArguments().add(resolverField);
    directive.getArguments().add(resolverArguments);

    // container
    ObjectTypeDefinition fooObjType = GraphQLFactoryDelegate.createObjectTypeDefinition();
    fooObjType.setName("FooType");

    ObjectType nonNullType = GraphQLFactoryDelegate.createObjectType();
    nonNullType.setType(fooObjType);

    FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition();
    fieldDefinition.setName("testField");
    fieldDefinition.setNamedType(nonNullType);
    fieldDefinition.getDirectives().add(directive);

    ResolverDirectiveDefinition.from(directive);
  }

  @Test(expected = NullPointerException.class)
  public void nullDirectiveTest() {
    ResolverDirectiveDefinition.from(null);
  }

}
