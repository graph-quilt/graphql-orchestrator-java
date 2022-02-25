package com.intuit.graphql.orchestrator.federation;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger.EntityMergingContext;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
}
