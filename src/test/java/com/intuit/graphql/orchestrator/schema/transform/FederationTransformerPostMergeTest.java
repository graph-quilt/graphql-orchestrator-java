package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirective;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirectiveDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeExtensionDefinition;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_EXTERNAL_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createTypeSystemDefinition;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.DirectiveDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeExtensionDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.graphQL.impl.ArgumentImpl;
import com.intuit.graphql.orchestrator.federation.exceptions.ExternalFieldNotFoundInBaseException;
import com.intuit.graphql.orchestrator.federation.exceptions.SharedOwnershipException;
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FederationTransformerPostMergeTest {

  private final FieldDefinition BASE_FIELD_DEFINITION = buildFieldDefinition("testField1");
  private final Directive EXTERNAL_DIRECTIVE = buildDirective(buildDirectiveDefinition(FEDERATION_EXTERNAL_DIRECTIVE), null);
  private final FieldDefinition EXTENSION_FIELD_DEFINITION = buildFieldDefinition("testField1", singletonList(EXTERNAL_DIRECTIVE));

  @Mock
  private UnifiedXtextGraph unifiedXtextGraphMock;

  private final FederationTransformerPostMerge subjectUnderTest = new FederationTransformerPostMerge();

  @Before
  public void setup() {
    Map<String, TypeDefinition> entitiesByTypeName = new HashMap<>();
    Map<String, Map<String, TypeSystemDefinition>> entityExtensionsByNamespace = new HashMap<>();
    ObjectTypeDefinition baseObjectType =
            buildObjectTypeDefinition("EntityType", singletonList(BASE_FIELD_DEFINITION));
    entitiesByTypeName.put("EntityType", baseObjectType);

    ObjectTypeDefinition objectTypeExtension =
            buildObjectTypeDefinition("EntityType", singletonList(EXTENSION_FIELD_DEFINITION));

    TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition();
    typeSystemDefinition.setType(objectTypeExtension);

    entityExtensionsByNamespace.put("testNamespace", ImmutableMap.of("EntityType", typeSystemDefinition));

    when(unifiedXtextGraphMock.getEntitiesByTypeName()).thenReturn(entitiesByTypeName);
    when(unifiedXtextGraphMock.getEntityExtensionsByNamespace()).thenReturn(entityExtensionsByNamespace);
  }

  @Test
  public void transform_success() {
    UnifiedXtextGraph actual = subjectUnderTest.transform(unifiedXtextGraphMock);
    assertThat(actual).isSameAs(unifiedXtextGraphMock);
    verify(unifiedXtextGraphMock, times(1)).getEntitiesByTypeName();
    verify(unifiedXtextGraphMock, times(2)).getEntityExtensionsByNamespace();
  }

  @Test
  public void transform_success_extension_key_in_subset() {
    HashMap<String, Map<String, TypeSystemDefinition>> extensionsByNamespace = new HashMap<>();
    HashMap<String, TypeSystemDefinition> extDefinitionsByName = new HashMap<>();
    HashMap<String, TypeDefinition> baseDefinitionsByName = new HashMap<>();
    TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition();

    Directive sharedKeyDirective1 = createMockKeyDirectory("testField1");
    Directive sharedKeyDirective2 = createMockKeyDirectory("testField1");
    Directive uniqueKeyDirective = createMockKeyDirectory("testField2");

    ObjectTypeDefinition baseObjectType =
            buildObjectTypeDefinition("EntityType", singletonList(BASE_FIELD_DEFINITION));
    baseObjectType.getDirectives().addAll(Arrays.asList(sharedKeyDirective1,uniqueKeyDirective));

    ObjectTypeDefinition objectTypeExtension =
            buildObjectTypeDefinition("EntityType", singletonList(EXTENSION_FIELD_DEFINITION));
    objectTypeExtension.getDirectives().add(sharedKeyDirective2);

    typeSystemDefinition.setType(objectTypeExtension);
    baseDefinitionsByName.put("EntityType", baseObjectType);
    extDefinitionsByName.put("EntityType", typeSystemDefinition);
    extensionsByNamespace.put("testNamespace", extDefinitionsByName);

    when(unifiedXtextGraphMock.getEntityExtensionsByNamespace()).thenReturn(extensionsByNamespace);
    when(unifiedXtextGraphMock.getEntitiesByTypeName()).thenReturn(baseDefinitionsByName);

    UnifiedXtextGraph actual = subjectUnderTest.transform(unifiedXtextGraphMock);
    assertThat(actual).isSameAs(unifiedXtextGraphMock);
    verify(unifiedXtextGraphMock, times(1)).getEntitiesByTypeName();
    verify(unifiedXtextGraphMock, times(2)).getEntityExtensionsByNamespace();
  }

  @Test
  public void transform_success_extension_ObjectTypeExtension_key_in_subset() {
    HashMap<String, Map<String, TypeSystemDefinition>> extensionsByNamespace = new HashMap<>();

    HashMap<String, TypeSystemDefinition> extDefinitionsByName = new HashMap<>();
    HashMap<String, TypeDefinition> baseDefinitionsByName = new HashMap<>();
    TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition();

    Directive sharedKeyDirective1 = createMockKeyDirectory("testField1");
    Directive sharedKeyDirective2 = createMockKeyDirectory("testField1");
    Directive uniqueKeyDirective = createMockKeyDirectory("testField2");

    ObjectTypeDefinition baseObjectType =
            buildObjectTypeDefinition("EntityType", singletonList(BASE_FIELD_DEFINITION));
    baseObjectType.getDirectives().addAll(Arrays.asList(sharedKeyDirective1,uniqueKeyDirective));

    ObjectTypeExtensionDefinition objectTypeExtension =
            buildObjectTypeExtensionDefinition("EntityType", singletonList(EXTENSION_FIELD_DEFINITION));
    objectTypeExtension.getDirectives().add(sharedKeyDirective2);

    typeSystemDefinition.setTypeExtension(objectTypeExtension);
    baseDefinitionsByName.put("EntityType", baseObjectType);
    extDefinitionsByName.put("EntityType", typeSystemDefinition);
    extensionsByNamespace.put("testNamespace", extDefinitionsByName);

    when(unifiedXtextGraphMock.getEntityExtensionsByNamespace()).thenReturn(extensionsByNamespace);
    when(unifiedXtextGraphMock.getEntitiesByTypeName()).thenReturn(baseDefinitionsByName);

    UnifiedXtextGraph actual = subjectUnderTest.transform(unifiedXtextGraphMock);
    assertThat(actual).isSameAs(unifiedXtextGraphMock);
    verify(unifiedXtextGraphMock, times(1)).getEntitiesByTypeName();
    verify(unifiedXtextGraphMock, times(2)).getEntityExtensionsByNamespace();
  }

  @Test(expected = TypeConflictException.class)
  public void transform_fails_extension_key_not_subset() {
    HashMap<String, Map<String, TypeSystemDefinition>> extensionsByNamespace = new HashMap<>();
    HashMap<String, TypeSystemDefinition> extDefinitionsByName = new HashMap<>();
    HashMap<String, TypeDefinition> baseDefinitionsByName = new HashMap<>();
    TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition();

    Directive sharedKeyDirective1 = createMockKeyDirectory("testField1");
    Directive sharedKeyDirective2 = createMockKeyDirectory("testField1");
    Directive uniqueKeyDirective = createMockKeyDirectory("testField2");

    ObjectTypeDefinition baseObjectType =
            buildObjectTypeDefinition("EntityType", singletonList(BASE_FIELD_DEFINITION));
    baseObjectType.getDirectives().add(sharedKeyDirective1);

    ObjectTypeDefinition objectTypeExtension =
            buildObjectTypeDefinition("EntityType", singletonList(EXTENSION_FIELD_DEFINITION));
    objectTypeExtension.getDirectives().addAll(Arrays.asList(sharedKeyDirective2, uniqueKeyDirective));

    typeSystemDefinition.setType(objectTypeExtension);
    baseDefinitionsByName.put("EntityType", baseObjectType);
    extDefinitionsByName.put("EntityType", typeSystemDefinition);
    extensionsByNamespace.put("testNamespace", extDefinitionsByName);

    when(unifiedXtextGraphMock.getEntityExtensionsByNamespace()).thenReturn(extensionsByNamespace);
    when(unifiedXtextGraphMock.getEntitiesByTypeName()).thenReturn(baseDefinitionsByName);

    UnifiedXtextGraph actual = subjectUnderTest.transform(unifiedXtextGraphMock);
    assertThat(actual).isSameAs(unifiedXtextGraphMock);
    verify(unifiedXtextGraphMock, times(1)).getEntitiesByTypeName();
    verify(unifiedXtextGraphMock, times(2)).getEntityExtensionsByNamespace();
  }

  @Test(expected = SharedOwnershipException.class)
  public void transform_fails_shared_field_without_external(){
    Map<String, Map<String, TypeSystemDefinition>> entityExtensionsByNamespace = new HashMap<>();
    TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition();

    ObjectTypeDefinition objectTypeExtension =
            buildObjectTypeDefinition("EntityType", singletonList(buildFieldDefinition("testField1")));
    typeSystemDefinition.setType(objectTypeExtension);
    entityExtensionsByNamespace.put("testNamespace", ImmutableMap.of("EntityType", typeSystemDefinition));

    when(unifiedXtextGraphMock.getEntityExtensionsByNamespace()).thenReturn(entityExtensionsByNamespace);

    UnifiedXtextGraph actual = subjectUnderTest.transform(unifiedXtextGraphMock);
    assertThat(actual).isSameAs(unifiedXtextGraphMock);
    verify(unifiedXtextGraphMock, times(1)).getEntitiesByTypeName();
    verify(unifiedXtextGraphMock, times(2)).getEntityExtensionsByNamespace();
  }

  @Test(expected = ExternalFieldNotFoundInBaseException.class)
  public void transform_fails_external_field_not_in_base(){
    Map<String, Map<String, TypeSystemDefinition>> entityExtensionsByNamespace = new HashMap<>();
    TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition();

    ObjectTypeDefinition objectTypeExtension = buildObjectTypeDefinition("EntityType",
                    Arrays.asList(EXTENSION_FIELD_DEFINITION, buildFieldDefinition("BadField", singletonList(buildDirective(buildDirectiveDefinition(FEDERATION_EXTERNAL_DIRECTIVE), null)))));

    typeSystemDefinition.setType(objectTypeExtension);
    entityExtensionsByNamespace.put("testNamespace", ImmutableMap.of("EntityType", typeSystemDefinition));
    when(unifiedXtextGraphMock.getEntityExtensionsByNamespace()).thenReturn(entityExtensionsByNamespace);

    UnifiedXtextGraph actual = subjectUnderTest.transform(unifiedXtextGraphMock);
    assertThat(actual).isSameAs(unifiedXtextGraphMock);
    verify(unifiedXtextGraphMock, times(1)).getEntitiesByTypeName();
    verify(unifiedXtextGraphMock, times(2)).getEntityExtensionsByNamespace();
  }

  private Directive createMockKeyDirectory(String fieldSet) {
    DirectiveDefinition keyDirectiveDefinition1 = buildDirectiveDefinition(FEDERATION_KEY_DIRECTIVE);
    ArgumentImpl fieldsArgument = Mockito.mock(ArgumentImpl.class);
    ValueWithVariable valueWithVariableMock = Mockito.mock(ValueWithVariable.class);
    List<Argument> fooKey = Arrays.asList(fieldsArgument);

    Mockito.when(valueWithVariableMock.getStringValue()).thenReturn(fieldSet);
    Mockito.when(fieldsArgument.getValueWithVariable()).thenReturn(valueWithVariableMock);

    return buildDirective(keyDirectiveDefinition1, fooKey);
  }
}
