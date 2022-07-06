package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.graphQL.*;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.eclipse.emf.ecore.EObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.*;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.FIELD_REFERENCE_PREFIX;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.RESOLVER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.isReferenceToFieldInParentType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FieldResolverDirectiveUtilTest {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

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

    Directive resolverDirective1 = buildDirective(buildDirectiveDefinition(RESOLVER_DIRECTIVE_NAME), Collections.emptyList());
    Directive resolverDirective2 = buildDirective(buildDirectiveDefinition(RESOLVER_DIRECTIVE_NAME), Collections.emptyList());

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
