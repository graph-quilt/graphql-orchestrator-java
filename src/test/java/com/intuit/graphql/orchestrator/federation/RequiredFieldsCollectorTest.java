package com.intuit.graphql.orchestrator.federation;

import static graphql.schema.FieldCoordinates.coordinates;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.schema.FieldCoordinates;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequiredFieldsCollectorTest {

  private static final String TEST_ENTITY_TYPE_NAME = "TestEntityType";

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

  private static final Field KEY_FIELD_1 = Field.newField("keyField1").build();
  private static final Field KEY_FIELD_2 = Field.newField("keyField2").build();

  FieldCoordinates FIELD_COORDINATE_STRFIELD = coordinates(TEST_ENTITY_TYPE_NAME, "stringField");
  FieldCoordinates FIELD_COORDINATE_OBJFIELD = coordinates(TEST_ENTITY_TYPE_NAME, "objectField");

  @Mock private ServiceMetadata serviceMetadataMock;

  @Mock private FederationMetadata federationMetadataMock;

  @Mock private EntityMetadata entityMetadataMock;

  @Mock private KeyDirectiveMetadata keyDirectiveMetadataMock;

  private RequiredFieldsCollector subjectUnderTest;

  @Before
  public void setup() {
    subjectUnderTest =
        RequiredFieldsCollector.builder()
            .excludeFields(Collections.emptySet())
            .parentTypeName(TEST_ENTITY_TYPE_NAME)
            .serviceMetadata(serviceMetadataMock)
            .fieldResolverContexts(Collections.emptyList())
            .fieldsWithRequiresDirective(ImmutableSet.of(FIELD_PRIMITIVE.getName(), FIELD_OBJECT.getName()))
            .build();

    when(serviceMetadataMock.isEntity(TEST_ENTITY_TYPE_NAME)).thenReturn(true);
    when(serviceMetadataMock.getFederationServiceMetadata()).thenReturn(federationMetadataMock);
    when(federationMetadataMock.getEntityMetadataByName(TEST_ENTITY_TYPE_NAME))
        .thenReturn(entityMetadataMock);

    when(federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_STRFIELD)).thenReturn(true);
    when(federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_OBJFIELD)).thenReturn(true);

    when(federationMetadataMock.getRequireFields(FIELD_COORDINATE_STRFIELD))
        .thenReturn(ImmutableSet.of(REQD_FIELD_1));
    when(federationMetadataMock.getRequireFields(FIELD_COORDINATE_OBJFIELD))
        .thenReturn(ImmutableSet.of(REQD_FIELD_2));

    when(entityMetadataMock.getKeyDirectives())
        .thenReturn(Collections.singletonList(keyDirectiveMetadataMock));
    when(keyDirectiveMetadataMock.getFieldSet())
        .thenReturn(ImmutableSet.of(KEY_FIELD_1, KEY_FIELD_2));
  }

  @Test
  public void get_returnsRequiredFieldsForRequiresDirective() {
    when(entityMetadataMock.getKeyDirectives()).thenReturn(Collections.emptyList());
    Set<Field> actual = subjectUnderTest.get();
    assertThat(actual).hasSize(2);
    assertThat(actual).isEqualTo(ImmutableSet.of(REQD_FIELD_1, REQD_FIELD_2));
  }

  @Test
  public void get_returnsRequiredFieldsForKeyDirective() {
    when(federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_STRFIELD)).thenReturn(false);
    when(federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_OBJFIELD)).thenReturn(false);

    Set<Field> actual = subjectUnderTest.get();
    assertThat(actual).hasSize(2);
    assertThat(actual).isEqualTo(ImmutableSet.of(KEY_FIELD_1, KEY_FIELD_2));
  }

  @Test
  public void get_returnsRequiredFieldsForKeyDirectivesAndRequiresDirective() {
    Set<Field> actual = subjectUnderTest.get();
    assertThat(actual).hasSize(4);
    assertThat(actual)
        .isEqualTo(ImmutableSet.of(KEY_FIELD_1, KEY_FIELD_2, REQD_FIELD_1, REQD_FIELD_2));
  }

  @Test
  public void get_returnsRequiredFieldsForKeyDirectivesAndRequiresDirectiveWithoutExcludeFields() {
    Set<String> excludeFields = ImmutableSet.of(KEY_FIELD_2.getName(), REQD_FIELD_1.getName());

    subjectUnderTest =
        RequiredFieldsCollector.builder()
            .excludeFields(excludeFields)
            .parentTypeName(TEST_ENTITY_TYPE_NAME)
            .serviceMetadata(serviceMetadataMock)
            .fieldResolverContexts(Collections.emptyList())
            .fieldsWithRequiresDirective(ImmutableSet.of(FIELD_PRIMITIVE.getName(), FIELD_OBJECT.getName()))
            .build();

    Set<Field> actual = subjectUnderTest.get();
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .isEqualTo(ImmutableSet.of(KEY_FIELD_1, REQD_FIELD_2));
  }

  @Test
  public void get_returnsRequiredFieldsForFieldResolver() {
    when(entityMetadataMock.getKeyDirectives()).thenReturn(Collections.emptyList());

    FieldResolverContext fieldResolverContextMock = mock(FieldResolverContext.class);
    when(fieldResolverContextMock.getRequiredFields())
        .thenReturn(ImmutableSet.of("reqdField"));

    subjectUnderTest =
        RequiredFieldsCollector.builder()
            .excludeFields(Collections.emptySet())
            .parentTypeName(TEST_ENTITY_TYPE_NAME)
            .serviceMetadata(serviceMetadataMock)
            .fieldResolverContexts(ImmutableList.of(fieldResolverContextMock))
            .fieldsWithRequiresDirective(Collections.emptySet())
            .build();

    Set<Field> actual = subjectUnderTest.get();
    assertThat(actual).hasSize(1);
    assertThat(actual.stream().findFirst().get().getName()).isEqualTo("reqdField");
  }

  @Test
  public void get_returnsRequiredFieldsForFieldResolverWithExcludedFields() {
    Set<String> excludeFields = ImmutableSet.of("reqdField");

    when(entityMetadataMock.getKeyDirectives()).thenReturn(Collections.emptyList());

    FieldResolverContext fieldResolverContextMock = mock(FieldResolverContext.class);
    when(fieldResolverContextMock.getRequiredFields())
        .thenReturn(ImmutableSet.of("reqdField"));

    subjectUnderTest =
        RequiredFieldsCollector.builder()
            .excludeFields(excludeFields)
            .parentTypeName(TEST_ENTITY_TYPE_NAME)
            .serviceMetadata(serviceMetadataMock)
            .fieldResolverContexts(ImmutableList.of(fieldResolverContextMock))
            .fieldsWithRequiresDirective(Collections.emptySet())
            .build();

    Set<Field> actual = subjectUnderTest.get();
    assertThat(actual).hasSize(0);
  }

}
