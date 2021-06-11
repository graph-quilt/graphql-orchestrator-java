package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveTestHelper.aSchema;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveTestHelper.bSchema;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveTestHelper;
import com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveTestHelper.TestService;
import graphql.language.AstTransformer;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.InlineFragment;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLUnmodifiedType;
import org.junit.Before;
import org.junit.Test;

public class NoExternalReferenceSelectionSetModifierTest {

  private GraphQLUnmodifiedType aType;
  private Field af1, af2;
  private Field b1, b2, b3, b4, b5;
  private SelectionSet selectionSet;
  private SelectionSet reverseSelectionSet;

  FieldResolverDirectiveTestHelper fieldResolverTestHelper;

  @Before
  public void setup() {
    ServiceProvider serviceA = new TestService("serviceA", aSchema, null);
    ServiceProvider serviceB = new TestService("serviceB", bSchema, null);
    fieldResolverTestHelper = new FieldResolverDirectiveTestHelper(serviceA, serviceB);
    GraphQLSchema graphQLSchema = fieldResolverTestHelper.getGraphQLSchema();
    aType = unwrapAll(graphQLSchema.getType("AObjectType"));
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
  }

  @Test
  public void canRemoveField() {
    AstTransformer astTransformer = new AstTransformer();

    // test 'a1 { af1 af2 b1 b2 b3 b4 b5 }' and remove b1..b5
    Field a1 = Field.newField("a1").selectionSet(selectionSet).build();
    Field newA1 = (Field) astTransformer
        .transform(a1, new NoExternalReferenceSelectionSetModifier((GraphQLFieldsContainer) aType));

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
    Field newA1 = (Field) astTransformer
        .transform(a1, new NoExternalReferenceSelectionSetModifier((GraphQLFieldsContainer) aType));

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
        .transform(aFragment, new NoExternalReferenceSelectionSetModifier((GraphQLFieldsContainer) aType));

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
    Field newA1 = (Field) astTransformer
        .transform(a1, new NoExternalReferenceSelectionSetModifier((GraphQLFieldsContainer) aType));

    assertThat(newA1.getSelectionSet().getSelections()).hasSize(2);
    Field f = (Field) newA1.getSelectionSet().getSelections().get(0);
    assertThat(f.getName()).isEqualTo(af2.getName());

    InlineFragment newInlineFragment = (InlineFragment) newA1.getSelectionSet().getSelections().get(1);
    assertThat(newInlineFragment.getSelectionSet().getSelections()).hasSize(1);

    // a should not be modified
    assertThat(a1.getSelectionSet().getSelections()).hasSize(4);
  }

}
