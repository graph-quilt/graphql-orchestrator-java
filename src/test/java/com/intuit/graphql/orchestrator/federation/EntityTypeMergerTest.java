package com.intuit.graphql.orchestrator.federation;

import com.google.common.collect.Sets;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeExtensionDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger.EntityMergingContext;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirective;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirectiveDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeExtensionDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createTypeSystemDefinition;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityTypeMergerTest {

  private static final FieldDefinition TEST_FIELD_DEFINITION_1 = buildFieldDefinition("testField1");
  private static final FieldDefinition TEST_FIELD_DEFINITION_2 = buildFieldDefinition("testField2");

  @Mock private EntityMergingContext entityMergingContextMock;
  @Mock private UnifiedXtextGraph unifiedXtextGraphMock;

  private EntityTypeMerger subjectUnderTest = new EntityTypeMerger();

  @Test
  public void mergeIntoBaseType_objectTypeDefinition_success() {
    TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition();

    ObjectTypeDefinition baseObjectType =
        buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_1));

    ObjectTypeDefinition objectTypeExtension =
        buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_2));

    typeSystemDefinition.setType(objectTypeExtension);

    when(entityMergingContextMock.getBaseType()).thenReturn(baseObjectType);
    when(entityMergingContextMock.getExtensionSystemDefinition()).thenReturn(typeSystemDefinition);

    TypeDefinition actual = subjectUnderTest.mergeIntoBaseType(entityMergingContextMock, unifiedXtextGraphMock);

    assertThat(actual).isSameAs(baseObjectType);
    List<FieldDefinition> actualFieldDefinitions = getFieldDefinitions(actual);
    assertThat(actualFieldDefinitions).hasSize(2);
  }

  @Test
  public void mergeIntoBaseType_objectTypeExtensionDefinition_success() {
    TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition();

    ObjectTypeDefinition baseObjectType =
        buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_1));

    ObjectTypeExtensionDefinition objectTypeExtension =
        buildObjectTypeExtensionDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_2));

    typeSystemDefinition.setTypeExtension(objectTypeExtension);

    when(entityMergingContextMock.getBaseType()).thenReturn(baseObjectType);
    when(entityMergingContextMock.getExtensionSystemDefinition()).thenReturn(typeSystemDefinition);

    TypeDefinition actual = subjectUnderTest.mergeIntoBaseType(entityMergingContextMock, unifiedXtextGraphMock);

    assertThat(actual).isSameAs(baseObjectType);
    List<FieldDefinition> actualFieldDefinitions = getFieldDefinitions(actual);
    assertThat(actualFieldDefinitions).hasSize(2);
  }

  @Test
  public void mergeIntoBaseType_prunesFieldResolverInfo_success(){
    TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition();

    FieldDefinition resolverRequiredField = buildFieldDefinition("requiredField");

    FieldDefinition entityRequiredField = buildFieldDefinition("requiredField");
    entityRequiredField.getDirectives().add(buildDirective(buildDirectiveDefinition("external"), emptyList()));

    FieldDefinition conflictingFieldResolver1 = buildFieldDefinition("testField2");

    FieldDefinition fieldResolver2  = buildFieldDefinition("ExternalFieldResolver");


    ObjectTypeDefinition baseObjectType =
            buildObjectTypeDefinition("EntityType", Arrays.asList(
                    TEST_FIELD_DEFINITION_1,
                    resolverRequiredField,
                    conflictingFieldResolver1,
                    fieldResolver2
            ));

    ObjectTypeExtensionDefinition objectTypeExtension =
            buildObjectTypeExtensionDefinition("EntityType", Arrays.asList(
                    entityRequiredField,
                    TEST_FIELD_DEFINITION_2
            ));

    typeSystemDefinition.setTypeExtension(objectTypeExtension);

    when(entityMergingContextMock.getBaseType()).thenReturn(baseObjectType);
    when(entityMergingContextMock.getExtensionSystemDefinition()).thenReturn(typeSystemDefinition);

    List<FieldResolverContext> fieldResolverContexts = new ArrayList();
    fieldResolverContexts.add(
      FieldResolverContext.builder()
        .parentTypeDefinition(baseObjectType)
        .fieldDefinition(conflictingFieldResolver1)
        .build()
    );
    fieldResolverContexts.add(
      FieldResolverContext.builder()
        .parentTypeDefinition(baseObjectType)
        .fieldDefinition(fieldResolver2)
        .build()
    );

    when(unifiedXtextGraphMock.getFieldResolverContexts()).thenReturn(fieldResolverContexts);

    Map<String, FederationMetadata> federationMetadataMap = new HashMap<>();

    FederationMetadata baseFederationMetadataMock = mock(FederationMetadata.class);
    FederationMetadata extFederationMetadataMock = mock(FederationMetadata.class);

    FederationMetadata.EntityMetadata baseEntityMetaDataMock = mock(FederationMetadata.EntityMetadata.class);
    Set<String> baseFields = Sets.newHashSet("requiredField", "testField2", "ExternalFieldResolver");
    when(baseEntityMetaDataMock.getFields()).thenReturn(baseFields);

    FederationMetadata.EntityMetadata extEntityMetaDataMock = mock(FederationMetadata.EntityMetadata.class);
    Set<String> extFields = Sets.newHashSet( "testField2");
    when(extEntityMetaDataMock.getFields()).thenReturn(extFields);

    when(baseFederationMetadataMock.getEntityMetadataByName("EntityType")).thenReturn(baseEntityMetaDataMock);
    when(extFederationMetadataMock.getEntityMetadataByName("EntityType")).thenReturn(extEntityMetaDataMock);

    federationMetadataMap.put("baseService", baseFederationMetadataMock);
    federationMetadataMap.put("extService", extFederationMetadataMock);

    when(unifiedXtextGraphMock.getFederationMetadataByNamespace()).thenReturn(federationMetadataMap);
    when(entityMergingContextMock.getTypename()).thenReturn("EntityType");
    TypeDefinition actual = subjectUnderTest.mergeIntoBaseType(entityMergingContextMock, unifiedXtextGraphMock);

    assertThat(actual).isSameAs(baseObjectType);
    List<FieldDefinition> actualFieldDefinitions = getFieldDefinitions(actual);
    assertThat(actualFieldDefinitions).hasSize(4);

    assertThat(fieldResolverContexts.size()).isEqualTo(1);
    assertThat(fieldResolverContexts.get(0).getFieldName()).isEqualTo("ExternalFieldResolver");

    assertThat(baseFields.size()).isEqualTo(2);
    assertThat(baseFields).contains("requiredField", "ExternalFieldResolver");

    assertThat(actualFieldDefinitions.stream().map(FieldDefinition::getName).collect(Collectors.toList()))
            .contains("requiredField", "testField2", "ExternalFieldResolver", "testField1");
  }

  public void mergeIntoBaseType_interfaceTypeDefinition_success() {
    // TODO
  }

  public void mergeIntoBaseType_notTheSameTypeDefinition_success() {
    // TODO
  }
}
