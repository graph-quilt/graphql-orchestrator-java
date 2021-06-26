package com.intuit.graphql.orchestrator.resolverdirective;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirective;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirectiveDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.FIELD_REFERENCE_PREFIX;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.isReferenceToFieldInParentType;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.DirectiveDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeExtensionDefinition;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.language.Argument;
import graphql.language.StringValue;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.emf.ecore.EObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FieldResolverDirectiveUtilTest {

  private static final String RESOLVER_DIRECTIVE_DEFINITION_STRING = "testResolverDirectiveToString";
  private static final String SERVICE_NAMESPACE = "testServiceNameSpace";

  @Mock
  private ResolverDirectiveDefinition resolverDirectiveDefinitionMock;

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Before
  public void setup() {
    when(resolverDirectiveDefinitionMock.toString()).thenReturn(RESOLVER_DIRECTIVE_DEFINITION_STRING);
  }

  @Test
  public void canCreateGraphQLArgumentsFromResolverArguments() {
    // GIVEN
    List<ResolverArgument> resolverArguments = Arrays.asList(
        new ResolverArgument("key1", "$field1"),
        new ResolverArgument("key2", "$field2")
    );

    GraphQLFieldsContainer parentType = GraphQLObjectType.newObject()
        .name("SomeType")
        .field(newFieldDefinition().name("field1").type(GraphQLString).build())
        .field(newFieldDefinition().name("field2").type(GraphQLString).build())
        .build();

    Map<String, Object> source = new HashMap<>();
    source.put("field1", "Value1");
    source.put("field2", "Value2");

    // WHEN
    List<Argument> arguments = FieldResolverDirectiveUtil
        .createResolverQueryFieldArguments(resolverArguments, parentType, source,
            resolverDirectiveDefinitionMock, SERVICE_NAMESPACE);

    // THEN
    assertThat(arguments).hasSize(2);
    assertThat(arguments.get(0).getName()).isEqualTo("key1");
    assertThat(((StringValue) arguments.get(0).getValue()).getValue()).isEqualTo("Value1");
    assertThat(arguments.get(1).getName()).isEqualTo("key2");
    assertThat(((StringValue) arguments.get(1).getValue()).getValue()).isEqualTo("Value2");
  }

  @Test
  public void cannotCreateGraphQLArgumentsFieldNotPresentInParentSelection() {
    // GIVEN

    exceptionRule.expect(FieldNotFoundInParentException.class);
    exceptionRule.expectMessage(startsWith("Field not found in parent's resolved value.  fieldName=field2,  "
        + "parentTypeName=SomeType,  resolverDirectiveDefinition=testResolverDirectiveToString, "
        + "serviceNameSpace=testServiceNameSpace"));

    List<ResolverArgument> resolverArguments = Arrays.asList(
        new ResolverArgument("key1", "$field1"),
        new ResolverArgument("key2", "$field2")
    );

    GraphQLFieldsContainer parentType = GraphQLObjectType.newObject()
        .name("SomeType")
        .field(newFieldDefinition().name("field1").type(GraphQLString).build())
        .field(newFieldDefinition().name("field2").type(GraphQLString).build())
        .build();

    Map<String, Object> sourceWithoutField2 = new HashMap<>();
    sourceWithoutField2.put("field1", "Value1");

    // WHEN
    FieldResolverDirectiveUtil
        .createResolverQueryFieldArguments(resolverArguments, parentType, sourceWithoutField2,
            resolverDirectiveDefinitionMock, SERVICE_NAMESPACE);

  }

  @Test
  public void cannotCreateGraphQLArgumentsFieldValueNotPresentInParentSelection() {
    // GIVEN

    exceptionRule.expect(FieldValueIsNullInParentException.class);
    exceptionRule.expectMessage(startsWith("Field value not found in parent's resolved value.  fieldName=field2,  "
        + "parentTypeName=SomeType,  resolverDirectiveDefinition=testResolverDirectiveToString, "
        + "serviceNameSpace=testServiceNameSpace"));

    List<ResolverArgument> resolverArguments = Arrays.asList(
        new ResolverArgument("key1", "$field1"),
        new ResolverArgument("key2", "$field2")
    );

    GraphQLFieldsContainer parentType = GraphQLObjectType.newObject()
        .name("SomeType")
        .field(newFieldDefinition().name("field1").type(GraphQLString).build())
        .field(newFieldDefinition().name("field2").type(GraphQLString).build())
        .build();

    Map<String, Object> sourceField2ValueIsMissing = new HashMap<>();
    sourceField2ValueIsMissing.put("field1", "Value1");
    sourceField2ValueIsMissing.put("field2", null);

    // WHEN
    FieldResolverDirectiveUtil
        .createResolverQueryFieldArguments(resolverArguments, parentType, sourceField2ValueIsMissing,
            resolverDirectiveDefinitionMock, SERVICE_NAMESPACE);

  }

  @Test(expected = ResolverArgumentNotAFieldOfParentException.class)
  public void cannotCreateArgumentsDueToNotAFieldOfParentError() {
    // GIVEN
    List<ResolverArgument> resolverArguments = Collections.singletonList(
        new ResolverArgument("key1", "$field1")
    );

    GraphQLFieldsContainer parentType = GraphQLObjectType.newObject()
        .name("SomeType")
        .build();

    // WHEN.. THEN throws exception
    FieldResolverDirectiveUtil
        .createResolverQueryFieldArguments(resolverArguments, parentType, new HashMap<>(),
            resolverDirectiveDefinitionMock, SERVICE_NAMESPACE);

  }

  @Test(expected = NotAValidFieldReference.class)
  public void cannotCreateArgumentsWrongFirstCharInFieldReference() {
    // GIVEN
    List<ResolverArgument> resolverArguments = Collections.singletonList(
        new ResolverArgument("key1", "SHOULD_START_WITH_$")
    );

    GraphQLFieldsContainer parentType = GraphQLObjectType.newObject()
        .name("SomeType")
        .build();

    // WHEN.. THEN throws exception
    FieldResolverDirectiveUtil
        .createResolverQueryFieldArguments(resolverArguments, parentType, new HashMap<>(),
            resolverDirectiveDefinitionMock, SERVICE_NAMESPACE);

  }

  @Test(expected = NotAValidFieldReference.class)
  public void cannotCreateArgumentNoNameInFieldReference() {
    // GIVEN
    List<ResolverArgument> resolverArguments = Collections.singletonList(
        new ResolverArgument("key1", "$")
    );

    GraphQLFieldsContainer parentType = GraphQLObjectType.newObject()
        .name("SomeType")
        .build();

    // WHEN.. THEN throws exception
    FieldResolverDirectiveUtil
        .createResolverQueryFieldArguments(resolverArguments, parentType, new HashMap<>(),
            resolverDirectiveDefinitionMock, SERVICE_NAMESPACE);

  }

  @Test
  public void isReferenceToFieldInParentTypeSuccess() {
    // GIVEN
    String fieldName = "someFieldName";
    FieldDefinition fieldDefinition = buildFieldDefinition(fieldName);
    String fieldReferenceWithNoCorrectPrefix = String.join("", FIELD_REFERENCE_PREFIX, fieldName);
    ObjectTypeDefinition typeDefinition = buildObjectTypeDefinition("TypeName", Collections.singletonList(fieldDefinition));

    // WHEN
    boolean actualResult = isReferenceToFieldInParentType(fieldReferenceWithNoCorrectPrefix, typeDefinition);

    // THEN
    assertThat(actualResult).isTrue();
  }

  @Test
  public void isReferenceToFieldInParentTypeFailsNotPresentInParentType() {
    // GIVEN
    String fieldName = "someFieldName";
    FieldDefinition fieldDefinition = buildFieldDefinition("someOTHERFieldName");
    String fieldReferenceWithNoCorrectPrefix = String.join("", FIELD_REFERENCE_PREFIX, fieldName);
    ObjectTypeDefinition typeDefinition = buildObjectTypeDefinition("TypeName", Collections.singletonList(fieldDefinition));

    // WHEN
    boolean actualResult = isReferenceToFieldInParentType(fieldReferenceWithNoCorrectPrefix, typeDefinition);

    // THEN
    assertThat(actualResult).isFalse();
  }

  @Test(expected = NotAValidFieldReference.class)
  public void isReferenceToFieldInParentTypeFailsInvalidArgument() {
    // GIVEN
    String fieldName = "someFieldName";
    FieldDefinition fieldDefinition = buildFieldDefinition(fieldName);
    ObjectTypeDefinition typeDefinition = buildObjectTypeDefinition("TypeName", Collections.singletonList(fieldDefinition));

    // WHEN
    isReferenceToFieldInParentType(fieldName, typeDefinition);
  }

  @Test(expected = MultipleResolverDirectiveDefinition.class)
  public void createFieldResolverContextsThrowsExceptionForMultipleResolverDirectives() {

    XtextGraph mockXtextGraph = Mockito.mock(XtextGraph.class);

    Directive resolverDirective1 = buildDirective(buildDirectiveDefinition("resolver"), Collections.emptyList());
    Directive resolverDirective2 = buildDirective(buildDirectiveDefinition("resolver"), Collections.emptyList());

    List<Directive> directives = Arrays.asList(resolverDirective1, resolverDirective2);
    FieldDefinition fieldDefinitionWithResolver1 = buildFieldDefinition("testField1", directives);

    // leaving this line here as it seems there's a bug in object creation.  This resets the list of
    // directives in fieldDefinitionWithResolver1.
    // FieldDefinition fieldDefinitionWithResolver2 = buildFieldDefinition("testField2", directives);

    List<FieldDefinition> fieldDefinitions = new ArrayList<>();
    fieldDefinitions.add(fieldDefinitionWithResolver1);

    ObjectTypeDefinition objectTypeDefinition = buildObjectTypeDefinition("TestType", fieldDefinitions);

    FieldResolverDirectiveUtil
        .createFieldResolverContexts(objectTypeDefinition, mockXtextGraph);
  }

  @Test
  public void canContainFieldResolverDirectiveReturnsTrueForObjectTypeExtensionDefinition() {
    EObject eContainer = mock(ObjectTypeExtensionDefinition.class);

    boolean actual = FieldResolverDirectiveUtil.canContainFieldResolverDirective(eContainer);

    assertThat(actual).isTrue();
  }

  @Test
  public void canContainFieldResolverDirectiveReturnsTrueForObjectTypeDefinition() {
    EObject eContainer = mock(ObjectTypeDefinition.class);

    boolean actual = FieldResolverDirectiveUtil.canContainFieldResolverDirective(eContainer);

    assertThat(actual).isTrue();
  }

  @Test
  public void canContainFieldResolverDirectiveReturnsFalseForInterfaceTypeDefinition() {
    EObject eContainer = mock(InterfaceTypeDefinition.class);

    boolean actual = FieldResolverDirectiveUtil.canContainFieldResolverDirective(eContainer);

    assertThat(actual).isFalse();
  }

  @Test
  public void getResolverDirectiveParentTypeNameInvalidParentType() {
    // GIVEN
    exceptionRule.expect(UnexpectedResolverDirectiveParentType.class);
    exceptionRule.expectMessage(startsWith("Expecting parent to be an instance of FieldDefinition.  "
        + "directive=mockDirective, pareTypeInstance=InterfaceTypeDefinition"));

    EObject eContainer = mock(InterfaceTypeDefinition.class);

    DirectiveDefinition mockDirectiveDefinition = mock(DirectiveDefinition.class);
    when(mockDirectiveDefinition.getName()).thenReturn("mockDirective");

    Directive mockDirective = mock(Directive.class);
    when(mockDirective.eContainer()).thenReturn(eContainer);
    when(mockDirective.getDefinition()).thenReturn(mockDirectiveDefinition);

    // WHEN
    FieldResolverDirectiveUtil.getResolverDirectiveParentTypeName(mockDirective);
  }

}
