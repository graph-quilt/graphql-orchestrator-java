package com.intuit.graphql.orchestrator.utils;

import static graphql.schema.FieldCoordinates.coordinates;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.schema.FieldCoordinates;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequiredFieldsCollectorTest {

  private static final String TEST_PARENT_TYPENAME = "ParentType";

  private static final Field FIELD_PRIMITIVE = Field.newField("stringField").build();

  private static final Field FIELD_OBJECT =
      Field.newField("objectField")
          .selectionSet(
              SelectionSet.newSelectionSet()
                  .selection(Field.newField("subField1").build())
                  .selection(Field.newField("subField2").build())
                  .build())
          .build();

  private static final Field REQD_FIELD_1 = Field.newField("reqdField1").build();
  private static final Field REQD_FIELD_2 = Field.newField("reqdField2").build();
  private static final Field REQD_FIELD_3 = Field.newField("reqdField3").build();

  FieldCoordinates FIELD_COORDINATE_STRFIELD = coordinates(TEST_PARENT_TYPENAME, "stringField");
  FieldCoordinates FIELD_COORDINATE_OBJFIELD = coordinates(TEST_PARENT_TYPENAME, "objectField");

  @Mock private ServiceMetadata serviceMetadataMock;
  @Mock private FieldResolverContext fieldResolverContextStringFieldMock;
  @Mock private FieldResolverContext fieldResolverContextObjectFieldMock;
  private RequiredFieldsCollector subjectUnderTest;

  @Before
  public void setup() {
    when(fieldResolverContextStringFieldMock.getRequiredFields())
        .thenReturn(ImmutableSet.of("reqdField1", "reqdField2"));
    when(fieldResolverContextObjectFieldMock.getRequiredFields())
        .thenReturn(ImmutableSet.of("reqdField3"));

    when(serviceMetadataMock.getFieldResolverContext(FIELD_COORDINATE_STRFIELD)).thenReturn(fieldResolverContextStringFieldMock);
    when(serviceMetadataMock.getFieldResolverContext(FIELD_COORDINATE_OBJFIELD)).thenReturn(fieldResolverContextObjectFieldMock);
  }

  @Test
  public void get_oneRequiredFieldIsSelected_exclusedTheSelectedField() {
    subjectUnderTest =
        RequiredFieldsCollector.builder()
            .parentTypeName(TEST_PARENT_TYPENAME)
            .serviceMetadata(serviceMetadataMock)
            .fieldsWithResolver(ImmutableSet.of(FIELD_PRIMITIVE, FIELD_OBJECT))
            .excludedFields(ImmutableMap.of(
                "stringField", FIELD_PRIMITIVE,
                "objectField", FIELD_OBJECT,
                "reqdField1", REQD_FIELD_1
            ))
            .build();
    Set<Field> actual = subjectUnderTest.get();
    assertThat(actual).hasSize(2);
    assertThat(toFieldNameSet(actual)).isEqualTo(ImmutableSet.of(REQD_FIELD_2.getName(), REQD_FIELD_3.getName())); // REQD_FIELD_1 is excluded
  }

  @Test
  public void get_allRequiredFieldAreSelected_returnsEmptySet() {
    subjectUnderTest =
        RequiredFieldsCollector.builder()
            .parentTypeName(TEST_PARENT_TYPENAME)
            .serviceMetadata(serviceMetadataMock)
            .fieldsWithResolver(ImmutableSet.of(FIELD_PRIMITIVE, FIELD_OBJECT))
            .excludedFields(ImmutableMap.of(
                "stringField", FIELD_PRIMITIVE,
                "objectField", FIELD_OBJECT,
                "reqdField1", REQD_FIELD_1,
                "reqdField2", REQD_FIELD_2,
                "reqdField3", REQD_FIELD_3
            ))
            .build();
    Set<Field> actual = subjectUnderTest.get();
    assertThat(actual).hasSize(0);
  }

  @Test
  public void get_noRequiredFieldSelected_returnsAllRequiredFields() {
    subjectUnderTest =
        RequiredFieldsCollector.builder()
            .parentTypeName(TEST_PARENT_TYPENAME)
            .serviceMetadata(serviceMetadataMock)
            .fieldsWithResolver(ImmutableSet.of(FIELD_PRIMITIVE, FIELD_OBJECT))
            .excludedFields(ImmutableMap.of(
                "stringField", FIELD_PRIMITIVE,
                "objectField", FIELD_OBJECT
            ))
            .build();
    Set<Field> actual = subjectUnderTest.get();
    assertThat(actual).hasSize(3);
    assertThat(toFieldNameSet(actual))
        .isEqualTo(ImmutableSet.of(REQD_FIELD_1.getName(), REQD_FIELD_2.getName(), REQD_FIELD_3.getName())); // REQD_FIELD_1 is excluded
  }

  private Set<String> toFieldNameSet(Set<Field> fields) {
    return fields.stream()
        .map(field -> field.getName())
        .collect(Collectors.toSet());

  }

}
