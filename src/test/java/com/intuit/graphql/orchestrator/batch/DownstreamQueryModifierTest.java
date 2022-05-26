package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.resolverdirective.DownstreamQueryModifierTestHelper.aSchema;
import static com.intuit.graphql.orchestrator.resolverdirective.DownstreamQueryModifierTestHelper.bSchema;
import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.unwrapAll;
import static graphql.schema.FieldCoordinates.coordinates;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.common.collect.ImmutableSet;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import com.intuit.graphql.orchestrator.resolverdirective.DownstreamQueryModifierTestHelper;
import com.intuit.graphql.orchestrator.resolverdirective.DownstreamQueryModifierTestHelper.TestService;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import graphql.language.AstTransformer;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.InlineFragment;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLSchema;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DownstreamQueryModifierTest {

  @Mock
  private ServiceMetadata serviceMetadataMock;

  private Field af1, af2;
  private Field b1, b2, b3, b4, b5;
  private SelectionSet selectionSet;
  private SelectionSet reverseSelectionSet;

  private DownstreamQueryModifier subjectUnderTest;

  @Before
  public void setup() {
    ServiceProvider serviceA = new TestService("serviceA", aSchema, null);
    ServiceProvider serviceB = new TestService("serviceB", bSchema, null);
    DownstreamQueryModifierTestHelper fieldResolverTestHelper = new DownstreamQueryModifierTestHelper(
        serviceA, serviceB);
    GraphQLSchema graphQLSchema = fieldResolverTestHelper.getGraphQLSchema();
    GraphQLFieldsContainer aType = (GraphQLFieldsContainer) unwrapAll(
        graphQLSchema.getType("AObjectType"));
    af1 = Field.newField("af1").build();
    af2 = Field.newField("af2").build();
    b1 = Field.newField("b1").build();
    b2 = Field.newField("b2").build();
    b3 = Field.newField("b3").build();
    b4 = Field.newField("b4").build();
    b5 = Field.newField("b5").build();
    selectionSet = SelectionSet.newSelectionSet().selection(af1).selection(af2)
        .selection(b1).selection(b2).selection(b3).selection(b4).selection(b5).build();
    reverseSelectionSet = SelectionSet.newSelectionSet().selection(af2).selection(b5).selection(b4).selection(b3)
        .selection(b2).selection(b1).selection(af1).build();

    when(serviceMetadataMock.isOwnedByEntityExtension(any())).thenReturn(false);

    subjectUnderTest =
        new DownstreamQueryModifier(
            aType, serviceMetadataMock, Collections.emptyMap());
  }

  @Test
  public void canRemoveField() {
    AstTransformer astTransformer = new AstTransformer();

    // test 'a1 { af1 af2 b1 b2 b3 b4 b5 }' and remove b1..b5
    Field a1 = Field.newField("a1").selectionSet(selectionSet).build();
    Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest);

    // expect modified field to be 'a1 { af1 af2}'
    assertThat(newA1.getSelectionSet().getSelections()).hasSize(2);
    Field f = (Field) newA1.getSelectionSet().getSelections().get(0);
    assertThat(f.getName()).isEqualTo(af1.getName());

    Field f2 = (Field) newA1.getSelectionSet().getSelections().get(1);
    assertThat(f2.getName()).isEqualTo(af2.getName());

    // a should not be modified
    assertThat(a1.getSelectionSet().getSelections()).hasSize(7);
  }

  @Test
  public void canRemoveFieldWithReverseSelectionSet() {
    AstTransformer astTransformer = new AstTransformer();

    // test 'a1 { af2 b5 b4 b3 b2 b1 af1 }' and remove b5..b1
    Field a1 = Field.newField("a1").selectionSet(reverseSelectionSet).build();
    Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest);

    // expect modified field to be 'a1 { af2   af1}'
    assertThat(newA1.getSelectionSet().getSelections()).hasSize(2);
    Field f = (Field) newA1.getSelectionSet().getSelections().get(0);
    assertThat(f.getName()).isEqualTo(af2.getName());

    Field f2 = (Field) newA1.getSelectionSet().getSelections().get(1);
    assertThat(f2.getName()).isEqualTo(af1.getName());

    // a should not be modified
    assertThat(a1.getSelectionSet().getSelections()).hasSize(7);
  }

  // TODO resolver defined in ObjectType
  @Test
  public void canRemoveFieldsFromFragmentDefinition() {
    //    fragment aFragment on A {
    //      af1
    //      b2  <- to be remove
    //      b1  <- to be remove
    //    }
    SelectionSet aFragmentSelectionSet = SelectionSet.newSelectionSet().selection(af1)
        .selection(b2).selection(b1).build();

    FragmentDefinition aFragment = FragmentDefinition.newFragmentDefinition()
        .name("AFragment")
        .selectionSet(aFragmentSelectionSet)
        .typeCondition(TypeName.newTypeName("AObjectType").build())
        .build();

    AstTransformer astTransformer = new AstTransformer();
    FragmentDefinition newAFragmentDefinition = (FragmentDefinition) astTransformer
        .transform(aFragment, subjectUnderTest);

    assertThat(newAFragmentDefinition.getSelectionSet().getSelections()).hasSize(1);
    Field f = (Field) newAFragmentDefinition.getSelectionSet().getSelections().get(0);
    assertThat(f.getName()).isEqualTo(af1.getName());
  }

  @Test
  public void canRemoveFieldsFromInlineFragmentWithoutInterface() {
    AstTransformer astTransformer = new AstTransformer();

    InlineFragment inlineFragment = InlineFragment.newInlineFragment()
        .selectionSet(SelectionSet.newSelectionSet().selection(af1)
            .selection(b5).selection(b4).selection(b3).build())
        .typeCondition(TypeName.newTypeName("AObjectType").build())
        .build();

    SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(af2).selection(inlineFragment)
        .selection(b2).selection(b1).build();

    Field a1 = Field.newField("a1").selectionSet(selectionSet).build();
    Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest);

    assertThat(newA1.getSelectionSet().getSelections()).hasSize(2);
    Field f = (Field) newA1.getSelectionSet().getSelections().get(0);
    assertThat(f.getName()).isEqualTo(af2.getName());

    InlineFragment newInlineFragment = (InlineFragment) newA1.getSelectionSet().getSelections().get(1);
    assertThat(newInlineFragment.getSelectionSet().getSelections()).hasSize(1);

    // a should not be modified
    assertThat(a1.getSelectionSet().getSelections()).hasSize(4);
  }

  @Test
  public void visitSelectionSet_addRequiredFields() {
    KeyDirectiveMetadata keyDirectiveDataMock = mock(KeyDirectiveMetadata.class);
    when(keyDirectiveDataMock.getFieldSet()).thenReturn(ImmutableSet.of(Field.newField("id").build()));

    EntityMetadata entityMetadataMock = mock(EntityMetadata.class);
    when(entityMetadataMock.getKeyDirectives()).thenReturn(
        Collections.singletonList(keyDirectiveDataMock));

    FederationMetadata federationMetadataMock = mock(FederationMetadata.class);
    when(federationMetadataMock.hasRequiresFieldSet(coordinates("AObjectType", "af1")))
        .thenReturn(true);
    when(federationMetadataMock.getRequireFields(coordinates("AObjectType", "af1")))
        .thenReturn(ImmutableSet.of(Field.newField("reqdField").build()));
    when(federationMetadataMock.getEntityMetadataByName("AObjectType")).thenReturn(entityMetadataMock);
    when(serviceMetadataMock.getFederationServiceMetadata()).thenReturn(federationMetadataMock);
    when(serviceMetadataMock.isEntity("AObjectType")).thenReturn(true);

    // af1 is external, should be removed
    FieldCoordinates testFieldCoordinate = coordinates("AObjectType", "af1");
    when(serviceMetadataMock.isOwnedByEntityExtension(eq(testFieldCoordinate))).thenReturn(true);

    // test '{ af1 }' and add id as key field and regdFields requiredField
    selectionSet = SelectionSet.newSelectionSet().selection(af1).build();
    Field a1 = Field.newField("a1").selectionSet(selectionSet).build();

    AstTransformer astTransformer = new AstTransformer();
    Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest);

    List<Selection> selections = newA1.getSelectionSet().getSelections();
    assertThat(selections).hasSize(2);
  }

  @Test
  public void visitSelectionSet_reqFieldNotSelected_addRequiredFields() {
    FieldResolverContext fieldResolverContextMock = mock(FieldResolverContext.class);
    when(fieldResolverContextMock.getRequiredFields())
        .thenReturn(ImmutableSet.of("reqdField"));

    FieldCoordinates testFieldCoordinate = coordinates("AObjectType", "af1");
    when(serviceMetadataMock.getFieldResolverContext(testFieldCoordinate)).thenReturn(fieldResolverContextMock);

    // test '{ a1 { af1 } }', where af1 has @resolver and requires reqdField
    Field a1 = Field.newField("a1")
        .selectionSet(SelectionSet.newSelectionSet()
            .selection(af1)
            .build())
        .build();

    AstTransformer astTransformer = new AstTransformer();
    Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest);

    // test '{ a1 { af1 reqdField} }', expected
    @SuppressWarnings("rawtypes")
    List<Selection> actualSelections = newA1.getSelectionSet().getSelections();
    assertThat(actualSelections).hasSize(2);

    assertThat(toFieldNameSet(actualSelections)).contains("af1", "reqdField");
  }

  @Test
  public void visitSelectionSet_reqFieldAlreadySelected_doesNotAddRequiredField() {
    FieldResolverContext fieldResolverContextMock = mock(FieldResolverContext.class);
    when(fieldResolverContextMock.getRequiredFields())
        .thenReturn(ImmutableSet.of("reqdField"));

    FieldCoordinates testFieldCoordinate = coordinates("AObjectType", "af1");
    when(serviceMetadataMock.getFieldResolverContext(testFieldCoordinate)).thenReturn(fieldResolverContextMock);

    // test '{ a1 { af1 reqdField} }', where af1 has @resolver and requires reqdField
    Field a1 = Field.newField("a1")
        .selectionSet(SelectionSet.newSelectionSet()
            .selection(af1)
            .selection(Field.newField("reqdField").build())
            .build())
        .build();

    AstTransformer astTransformer = new AstTransformer();
    Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest);

    // test '{ a1 { af1 reqdField} }', expected
    @SuppressWarnings("rawtypes")
    List<Selection> actualA1Selections = newA1.getSelectionSet().getSelections();
    assertThat(actualA1Selections).hasSize(2);

    assertThat(toFieldNameSet(actualA1Selections)).contains("af1", "reqdField");
  }

  @Test
  public void visitSelectionSet_noRequiredFields_doesNotAddRequiredField() {
    FieldResolverContext fieldResolverContextMock = mock(FieldResolverContext.class);
    when(fieldResolverContextMock.getRequiredFields()).thenReturn(Collections.emptySet());

    FieldCoordinates testFieldCoordinate = coordinates("AObjectType", "af1");
    when(serviceMetadataMock.getFieldResolverContext(testFieldCoordinate)).thenReturn(fieldResolverContextMock);

    // test '{ a1 { af1} }', where af1 has @resolver and requires reqdField
    Field a1 = Field.newField("a1")
        .selectionSet(SelectionSet.newSelectionSet()
            .selection(af1)
            .build())
        .build();

    AstTransformer astTransformer = new AstTransformer();
    Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest);

    // test '{ a1 { af1} }', expected
    @SuppressWarnings("rawtypes")
    List<Selection> actualA1Selections = newA1.getSelectionSet().getSelections();
    assertThat(actualA1Selections).hasSize(1);

    assertThat(toFieldNameSet(actualA1Selections)).contains("af1");
  }

  @Test
  public void visitSelectionSet_noFieldResolvers_doesNotAddRequiredField() {
    FieldCoordinates testFieldCoordinate = coordinates("AObjectType", "af1");
    when(serviceMetadataMock.getFieldResolverContext(testFieldCoordinate)).thenReturn(null);

    // test '{ a1 { af1} }', where af1 has @resolver and requires reqdField
    Field a1 = Field.newField("a1")
        .selectionSet(SelectionSet.newSelectionSet()
            .selection(af1)
            .build())
        .build();

    AstTransformer astTransformer = new AstTransformer();
    Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest);

    // test '{ a1 { af1} }', expected
    @SuppressWarnings("rawtypes")
    List<Selection> actualA1Selections = newA1.getSelectionSet().getSelections();
    assertThat(actualA1Selections).hasSize(1);

    assertThat(toFieldNameSet(actualA1Selections)).contains("af1");
  }

  private List<String> toFieldNameSet(@SuppressWarnings("rawtypes") List<Selection> fields) {
    return fields.stream()
        .filter(selection -> selection instanceof Field)
        .map(selection -> (Field) selection)
        .map(Field::getName)
        .collect(Collectors.toList());

  }


}
