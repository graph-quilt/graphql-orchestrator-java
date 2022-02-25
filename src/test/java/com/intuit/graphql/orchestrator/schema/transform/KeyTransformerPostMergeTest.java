package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KeyTransformerPostMergeTest {

  private static final FieldDefinition TEST_FIELD_DEFINITION_1 = buildFieldDefinition("testField1");
  private static final FieldDefinition TEST_FIELD_DEFINITION_2 = buildFieldDefinition("testField2");

  @Mock
  private XtextGraph xtextGraphMock;

  private final KeyTransformerPostMerge subjectUnderTest = new KeyTransformerPostMerge();

  @Before
  public void setup() {
    Map<String, TypeDefinition> entitiesByTypeName = new HashMap<>();

    Map<String, Map<String, TypeDefinition>> entityExtensionsByNamespace = new HashMap<>();
    ObjectTypeDefinition baseObjectType =
        buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_1));
    entitiesByTypeName.put("EntityType", baseObjectType);

    ObjectTypeDefinition objectTypeExtension =
        buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_2));
    entityExtensionsByNamespace.put("testNamespace", ImmutableMap.of("EntityType", objectTypeExtension));

    when(xtextGraphMock.getEntitiesByTypeName()).thenReturn(entitiesByTypeName);
    when(xtextGraphMock.getEntityExtensionsByNamespace()).thenReturn(entityExtensionsByNamespace);
  }

  @Test
  public void transform_success() {
    XtextGraph actual = subjectUnderTest.transform(xtextGraphMock);
    assertThat(actual).isSameAs(xtextGraphMock);
    verify(xtextGraphMock, times(1)).getEntitiesByTypeName();
    verify(xtextGraphMock, times(2)).getEntityExtensionsByNamespace();
  }

}
