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
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

public class ResolverDirectiveDefinitionTest {

  private static final String TEST_RESOLVER_FIELDNAME = "resolverField";
  private static final String TEST_RESOLVER_ARGUMENT_NAME1 = "testFieldArg1";
  private static final String TEST_RESOLVER_ARGUMENT_VALUE1 = "$testFieldValue1";
  private static final String TEST_RESOLVER_ARGUMENT_NAME2 = "testFieldArg1";
  private static final String TEST_RESOLVER_ARGUMENT_VALUE2 = "$testFieldValue2";

  private ResolverDirectiveDefinition subjectUnderTest;

  @Before
  public void setup() {
    Argument resolverField = createResolverField(TEST_RESOLVER_FIELDNAME);
    Argument resolverArguments = createResolverArguments(TEST_RESOLVER_ARGUMENT_NAME1,
        TEST_RESOLVER_ARGUMENT_VALUE1,
        TEST_RESOLVER_ARGUMENT_NAME2, TEST_RESOLVER_ARGUMENT_VALUE2);

    Directive directive = GraphQLFactoryDelegate.createDirective();
    directive.getArguments().add(resolverField);
    directive.getArguments().add(resolverArguments);

    subjectUnderTest = ResolverDirectiveDefinition.from(directive);
  }

  @Test
  public void canCreateResolverDirectiveDefinitionTest() {
    assertThat(subjectUnderTest.getField()).isEqualTo(TEST_RESOLVER_FIELDNAME);
    ResolverArgumentDefinition resolverArgumentEntry1 = subjectUnderTest.getArguments().get(0);
    assertThat(resolverArgumentEntry1.getName()).isEqualTo(TEST_RESOLVER_ARGUMENT_NAME1);
    assertThat(resolverArgumentEntry1.getValue()).isEqualTo(TEST_RESOLVER_ARGUMENT_VALUE1);

    ResolverArgumentDefinition resolverArgumentEntry2 = subjectUnderTest.getArguments().get(1);
    assertThat(resolverArgumentEntry2.getName()).isEqualTo(TEST_RESOLVER_ARGUMENT_NAME2);
    assertThat(resolverArgumentEntry2.getValue()).isEqualTo(TEST_RESOLVER_ARGUMENT_VALUE2);
  }

  @Test(expected = ResolverDirectiveException.class)
  public void unexpectedArgumentForResolverDirectiveDefinitionTest() {
    // FootType has testField.  testField has @resolver(field: "resolverField", argument: [...]])
    // argument is not valid.
    Argument resolverArguments = GraphQLFactoryDelegate.createArgument();
    resolverArguments.setName("argument"); // should be arguments

    Argument resolverField = createResolverField(TEST_RESOLVER_FIELDNAME);

    Directive directive = GraphQLFactoryDelegate.createDirective();
    directive.getArguments().add(resolverField);
    directive.getArguments().add(resolverArguments);

    // container
    ObjectTypeDefinition fooObjType = GraphQLFactoryDelegate.createObjectTypeDefinition();
    fooObjType.setName("FooType");

    ObjectType objectType = GraphQLFactoryDelegate.createObjectType();
    objectType.setType(fooObjType);

    FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition();
    fieldDefinition.setName(TEST_RESOLVER_FIELDNAME);
    fieldDefinition.setNamedType(objectType);
    fieldDefinition.getDirectives().add(directive);
    ResolverDirectiveDefinition.from(directive);

  }

  @Test(expected = ResolverDirectiveException.class)
  public void unexpectedFieldForResolverDirectiveDefinitionTest() {
    String resolverFieldName = StringUtils.EMPTY; // cannot be empty

    Argument resolverField = createResolverField(resolverFieldName);
    Argument resolverArguments = createResolverArguments(TEST_RESOLVER_ARGUMENT_NAME1,
        TEST_RESOLVER_ARGUMENT_VALUE1,
        TEST_RESOLVER_ARGUMENT_NAME2, TEST_RESOLVER_ARGUMENT_VALUE2);

    Directive directive = GraphQLFactoryDelegate.createDirective();
    directive.getArguments().add(resolverField);
    directive.getArguments().add(resolverArguments);

    // container
    ObjectTypeDefinition fooObjType = GraphQLFactoryDelegate.createObjectTypeDefinition();
    fooObjType.setName("FooType");

    ObjectType nonNullType = GraphQLFactoryDelegate.createObjectType();
    nonNullType.setType(fooObjType);

    FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition();
    fieldDefinition.setName(TEST_RESOLVER_FIELDNAME);
    fieldDefinition.setNamedType(nonNullType);
    fieldDefinition.getDirectives().add(directive);

    ResolverDirectiveDefinition.from(directive);
  }

  @Test(expected = NullPointerException.class)
  public void nullDirectiveTest() {
    ResolverDirectiveDefinition.from(null);
  }


  @Test
  public void extractRequiredFieldsFrom() {
    Set<String> actual = ResolverDirectiveDefinition.extractRequiredFieldsFrom(subjectUnderTest);
    assertThat(actual).hasSize(2);
    assertThat(actual.contains("testFieldValue1")).isTrue();
    assertThat(actual.contains("testFieldValue2")).isTrue();
  }

}
