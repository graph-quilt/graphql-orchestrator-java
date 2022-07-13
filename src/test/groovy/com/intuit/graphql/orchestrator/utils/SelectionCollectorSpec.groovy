package com.intuit.graphql.orchestrator.utils

import graphql.language.*
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import spock.lang.Specification

import static java.util.Arrays.asList

class SelectionCollectorSpec extends Specification {

    private static final String TEST_FRAGMENT_NAME = "testFragment"

    private static final TypeName TEST_TYPE_NAME = TypeName.newTypeName().name("TestType").build()

    private static final Field SUB_FIELD1 = Field.newField("subField1").build()
    private static final Field SUB_FIELD2 = Field.newField("subField2").build()
    private static final Field SUB_FIELD3 = Field.newField("subField3InFragmentSpread").build()
    private static final Field SUB_FIELD4 = Field.newField("subField4InInlineFragment").build()

    private static final SelectionSet TEST_SELECTION_SET =
            SelectionSet.newSelectionSet()
                    .selection(SUB_FIELD1)
                    .selection(SUB_FIELD2)
                    .selection(SUB_FIELD1)
                    .selection(FragmentSpread.newFragmentSpread().name(TEST_FRAGMENT_NAME).build())
                    .selection(
                            InlineFragment.newInlineFragment()
                                    .selectionSet(SelectionSet.newSelectionSet().selection(SUB_FIELD4).build())
                                    .build())
                    .build()

    private SelectionCollector subjectUnderTest

    def setup() {
        Map<String, FragmentDefinition> testFragmentsByName = new HashMap<>()
        testFragmentsByName.put(
                TEST_FRAGMENT_NAME,
                FragmentDefinition.newFragmentDefinition()
                        .typeCondition(TEST_TYPE_NAME)
                        .selectionSet(SelectionSet.newSelectionSet().selection(SUB_FIELD3).build())
                        .build())

        subjectUnderTest = new SelectionCollector(testFragmentsByName)
    }

    def "can Collect Selections From Field"() {
        given:
        Map<String, Field> collectedSelections = subjectUnderTest.collectFields(TEST_SELECTION_SET)

        assert collectedSelections.size() == 4

        Collection<?> expectedSet = asList(SUB_FIELD1.getName(), SUB_FIELD2.getName(), SUB_FIELD3.getName(), SUB_FIELD4.getName())
        assert collectedSelections.keySet().containsAll(expectedSet)
    }

    def "empty Selection Set Returns Empty Set"() {
        given:
        Map<String, Field> collectedSelections = subjectUnderTest.collectFields(SelectionSet.newSelectionSet().build())

        expect:
        collectedSelections.isEmpty()
    }

    def "null Input Returns Empty Set"() {
        given:
        Map<String, Field> collectedSelections = subjectUnderTest.collectFields((SelectionSet)null)

        expect:
        collectedSelections.isEmpty()
    }

    def "unsupported Selection Throws Illegal State Exception"() {
        given:
        SelectionSet NEW_TEST_SELECTION_SET =
                TEST_SELECTION_SET.transform({ builder -> builder.selection(new NewImplementationSelection()) })

        when:
        subjectUnderTest.collectFields(NEW_TEST_SELECTION_SET)

        then:
        thrown(IllegalStateException)
    }

    /**
     * Used to test a case where in a new Implementation of Selection is introduced by
     * future versions of graphql.  See {@link #unsupportedSelectionThrowsIllegalStateException}
     */
    static class NewImplementationSelection implements Selection<NewImplementationSelection> {

        @Override
        List<Node> getChildren() {
            return null
        }

        @Override
        NodeChildrenContainer getNamedChildren() {
            return null
        }

        @Override
        NewImplementationSelection withNewChildren(NodeChildrenContainer newChildren) {
            return null
        }

        @Override
        SourceLocation getSourceLocation() {
            return null
        }

        @Override
        List<Comment> getComments() {
            return null
        }

        @Override
        IgnoredChars getIgnoredChars() {
            return null
        }

        @Override
        Map<String, String> getAdditionalData() {
            return null
        }

        @Override
        boolean isEqualTo(Node node) {
            return false
        }

        @Override
        NewImplementationSelection deepCopy() {
            return null
        }

        @Override
        TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
            return null
        }
    }
}
