package com.intuit.graphql.orchestrator.federation;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirective;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirectiveDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.DirectiveDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.graphQL.impl.ArgumentImpl;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger.EntityMergingContext;

import java.util.Arrays;
import java.util.List;

import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException;
import com.intuit.graphql.orchestrator.utils.FederationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EntityTypeMergerTest {

  private static final FieldDefinition TEST_FIELD_DEFINITION_1 = buildFieldDefinition("testField1");
  private static final FieldDefinition TEST_FIELD_DEFINITION_2 = buildFieldDefinition("testField2");

  @Mock private EntityMergingContext entityMergingContextMock;

  private EntityTypeMerger subjectUnderTest = new EntityTypeMerger();

  @Test
  public void mergeIntoBaseType_objectTypeDefinition_success() {

    ObjectTypeDefinition baseObjectType =
        buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_1));

    ObjectTypeDefinition objectTypeExtension =
        buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_2));

    when(entityMergingContextMock.getBaseType()).thenReturn(baseObjectType);
    when(entityMergingContextMock.getTypeExtension()).thenReturn(objectTypeExtension);

    TypeDefinition actual = subjectUnderTest.mergeIntoBaseType(entityMergingContextMock);

    assertThat(actual).isSameAs(baseObjectType);
    List<FieldDefinition> actualFieldDefinitions = getFieldDefinitions(actual);
    assertThat(actualFieldDefinitions).hasSize(2);
  }

  public void mergeIntoBaseType_interfaceTypeDefinition_success() {
    // TODO
  }

  public void mergeIntoBaseType_notTheSameTypeDefinition_success() {
    // TODO
  }

  @Test
  public void mergeIntoBaseType_success_extension_key_in_subset() {

    Directive fooKeyDirective1 = createMockKeyDirectory("foo");
    Directive fooKeyDirective2 = createMockKeyDirectory("foo");
    Directive barKeyDirective = createMockKeyDirectory("bar");

    ObjectTypeDefinition baseObjectType =
            buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_1));
    baseObjectType.getDirectives().add(fooKeyDirective1);
    baseObjectType.getDirectives().add(barKeyDirective);

    ObjectTypeDefinition objectTypeExtension =
            buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_2));
    objectTypeExtension.getDirectives().add(fooKeyDirective2);

    when(entityMergingContextMock.getBaseType()).thenReturn(baseObjectType);
    when(entityMergingContextMock.getTypeExtension()).thenReturn(objectTypeExtension);

    subjectUnderTest.mergeIntoBaseType(entityMergingContextMock);
  }

  @Test(expected = TypeConflictException.class)
  public void mergeIntoBaseType_fails_extension_key_not_subset() {
    Directive fooKeyDirective1 = createMockKeyDirectory("foo");
    Directive fooKeyDirective2 = createMockKeyDirectory("foo");
    Directive barKeyDirective = createMockKeyDirectory("bar");

    ObjectTypeDefinition baseObjectType =
            buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_1));
    baseObjectType.getDirectives().add(fooKeyDirective1);

    ObjectTypeDefinition objectTypeExtension =
            buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_2));
    objectTypeExtension.getDirectives().add(fooKeyDirective2);
    objectTypeExtension.getDirectives().add(barKeyDirective);

    when(entityMergingContextMock.getBaseType()).thenReturn(baseObjectType);
    when(entityMergingContextMock.getTypeExtension()).thenReturn(objectTypeExtension);

    TypeDefinition actual = subjectUnderTest.mergeIntoBaseType(entityMergingContextMock);

    assertThat(actual).isSameAs(baseObjectType);
    List<FieldDefinition> actualFieldDefinitions = getFieldDefinitions(actual);
    assertThat(actualFieldDefinitions).hasSize(2);
  }

  private Directive createMockKeyDirectory(String fieldSet) {
    DirectiveDefinition keyDirectiveDefinition1 = buildDirectiveDefinition(FederationUtils.FEDERATION_KEY_DIRECTIVE);
    ArgumentImpl fieldsArgument = Mockito.mock(ArgumentImpl.class);
    ValueWithVariable valueWithVariableMock = Mockito.mock(ValueWithVariable.class);
    List<Argument> fooKey = Arrays.asList(fieldsArgument);

    Mockito.when(valueWithVariableMock.getStringValue()).thenReturn(fieldSet);
    Mockito.when(fieldsArgument.getValueWithVariable()).thenReturn(valueWithVariableMock);

    return buildDirective(keyDirectiveDefinition1, fooKey);
  }

}
