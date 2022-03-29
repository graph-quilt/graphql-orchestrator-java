package com.intuit.graphql.orchestrator.utils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import graphql.language.Comment;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.IgnoredChars;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeChildrenContainer;
import graphql.language.NodeVisitor;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.SourceLocation;
import graphql.language.TypeName;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class SelectionCollectorTest {

  private static final String TEST_FRAGMENT_NAME = "testFragment";

  private static final TypeName TEST_TYPE_NAME = TypeName.newTypeName().name("TestType").build();

  private static final Field SUB_FIELD1 = Field.newField("subField1").build();
  private static final Field SUB_FIELD2 = Field.newField("subField2").build();
  private static final Field SUB_FIELD3 = Field.newField("subField3InFragmentSpread").build();
  private static final Field SUB_FIELD4 = Field.newField("subField4InInlineFragment").build();

  private static final SelectionSet TEST_SELECTION_SET =
      SelectionSet.newSelectionSet()
          .selection(SUB_FIELD1)
          .selection(SUB_FIELD2)
          .selection(FragmentSpread.newFragmentSpread().name(TEST_FRAGMENT_NAME).build())
          .selection(
              InlineFragment.newInlineFragment()
                  .selectionSet(SelectionSet.newSelectionSet().selection(SUB_FIELD4).build())
                  .build())
          .build();

  private SelectionCollector subjectUnderTest;

  @Before
  public void setup() {
    Map<String, FragmentDefinition> testFragmentsByName = new HashMap<>();
    testFragmentsByName.put(
        TEST_FRAGMENT_NAME,
        FragmentDefinition.newFragmentDefinition()
            .typeCondition(TEST_TYPE_NAME)
            .selectionSet(SelectionSet.newSelectionSet().selection(SUB_FIELD3).build())
            .build());

    subjectUnderTest = new SelectionCollector(testFragmentsByName);
  }

  @Test
  public void canCollectSelectionsFromField() {
    Map<String, Field> collectedSelections = subjectUnderTest.collectFields(TEST_SELECTION_SET);
    assertThat(collectedSelections).hasSize(4);
    Collection<?> expectedSet = asList(SUB_FIELD1.getName(), SUB_FIELD2.getName(), SUB_FIELD3.getName(), SUB_FIELD4.getName());
    assertThat(collectedSelections.keySet().containsAll(expectedSet)).isTrue();
  }

  @Test
  public void emptySelectionSetReturnsEmptySet() {
    Map<String, Field> collectedSelections = subjectUnderTest.collectFields(SelectionSet.newSelectionSet().build());
    assertThat(collectedSelections).isEmpty();
  }

  @Test
  public void nullInputReturnsEmptySet() {
    Map<String, Field> collectedSelections = subjectUnderTest.collectFields(null);
    assertThat(collectedSelections).isEmpty();
  }

  @Test(expected = IllegalStateException.class)
  public void unsupportedSelectionThrowsIllegalStateException() {
    SelectionSet NEW_TEST_SELECTION_SET =
        TEST_SELECTION_SET.transform(builder -> builder.selection(new NewImplementationSelection()));
    subjectUnderTest.collectFields(NEW_TEST_SELECTION_SET);
  }

  /**
   * Used to test a case where in a new Implementation of Selection is introduced by
   * future versions of graphql.  See {@link #unsupportedSelectionThrowsIllegalStateException}
   */
  static class NewImplementationSelection implements Selection<NewImplementationSelection> {

    @Override
    public List<Node> getChildren() {
      return null;
    }

    @Override
    public NodeChildrenContainer getNamedChildren() {
      return null;
    }

    @Override
    public NewImplementationSelection withNewChildren(NodeChildrenContainer newChildren) {
      return null;
    }

    @Override
    public SourceLocation getSourceLocation() {
      return null;
    }

    @Override
    public List<Comment> getComments() {
      return null;
    }

    @Override
    public IgnoredChars getIgnoredChars() {
      return null;
    }

    @Override
    public Map<String, String> getAdditionalData() {
      return null;
    }

    @Override
    public boolean isEqualTo(Node node) {
      return false;
    }

    @Override
    public NewImplementationSelection deepCopy() {
      return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
      return null;
    }
  }
}
