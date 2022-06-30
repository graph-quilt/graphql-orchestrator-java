package com.intuit.graphql.orchestrator.federation;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeExtensionDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger.EntityMergingContext;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeExtensionDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createTypeSystemDefinition;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
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

  public void mergeIntoBaseType_interfaceTypeDefinition_success() {
    // TODO
  }

  public void mergeIntoBaseType_notTheSameTypeDefinition_success() {
    // TODO
  }
}
