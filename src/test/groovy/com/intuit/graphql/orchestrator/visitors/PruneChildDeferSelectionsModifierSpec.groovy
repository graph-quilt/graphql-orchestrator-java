package com.intuit.graphql.orchestrator.visitors

import com.intuit.graphql.orchestrator.deferDirective.DeferOptions
import com.intuit.graphql.orchestrator.visitors.queryVisitors.PruneChildDeferSelectionsModifier
import graphql.GraphQLException
import graphql.language.*
import lombok.extern.slf4j.Slf4j
import spock.lang.Specification

@Slf4j
class PruneChildDeferSelectionsModifierSpec extends Specification {

    Directive enabledDirective = Directive.newDirective()
            .name("defer")
            .argument(Argument.newArgument("if", BooleanValue.of(true)).build())
            .build()
    Directive disabledDirective = Directive.newDirective()
            .name("defer")
            .argument(Argument.newArgument("if", BooleanValue.of(false)).build())
            .build()

    DeferOptions enabledNestedDefer = DeferOptions.builder().nestedDefersAllowed(true).build()
    DeferOptions disabledNestedDefer = DeferOptions.builder().nestedDefersAllowed(false).build()

    PruneChildDeferSelectionsModifier specToTest = PruneChildDeferSelectionsModifier.builder()
            .deferOptions(disabledNestedDefer)
            .build()

    PruneChildDeferSelectionsModifier nestedSpecToTest = PruneChildDeferSelectionsModifier.builder()
            .deferOptions(enabledNestedDefer)
            .build()

    AstTransformer astTransformer = new AstTransformer()

    def "Top Level Deferred Child Field is Removed"(){
        given:
        Field deferredField = Field.newField("topLevelDeferredChild").directive(enabledDirective).build()
        Field childField2 = Field.newField("topLevelChild").build()
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(deferredField).selection(childField2).build()
        Field root = Field.newField("rootField", selectionSet).build()

        when:
        Field result = (Field) astTransformer.transform(root, nestedSpecToTest)

        then:
        result != null
        result.getSelectionSet().getSelections().size() == 2
        ((Field)result.getSelectionSet().getSelections().get(0)).getName() == "topLevelChild"
        ((Field)result.getSelectionSet().getSelections().get(1)).getName() == "__typename"
    }

    def "Nested Deferred Child Field is Removed"(){
        given:
        Field deferredField = Field.newField("nestedDeferredChild").directive(enabledDirective).build()
        Field childField2 = Field.newField("nestedChild").build()
        SelectionSet nestedSelectionSet = SelectionSet.newSelectionSet().selection(deferredField).selection(childField2).build()
        Field topLevelChild = Field.newField("topLevelChild", nestedSelectionSet).build()

        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(topLevelChild).build()
        Field root = Field.newField("rootField", selectionSet).build()

        when:
        Field result = (Field) astTransformer.transform(root, nestedSpecToTest)

        then:
        result != null
        result.getSelectionSet().getSelections().size() == 2

        Field resultTopChild = (Field)result.getSelectionSet().getSelections().get(0)
        topLevelChild.getName() == "topLevelChild"
        ((Field)result.getSelectionSet().getSelections().get(1)).getName() == "__typename"

        resultTopChild.getSelectionSet().getSelections().size() == 2
        ((Field) resultTopChild.getSelectionSet().getSelections().get(0)).getName() == "nestedChild"
        ((Field) resultTopChild.getSelectionSet().getSelections().get(1)).getName() == "__typename"
    }

    def "Removes Disabled Defer Directive From Child Field"(){
        given:
        Field deferredField = Field.newField("topLevelDeferredChild").directive(disabledDirective).build()
        Field childField2 = Field.newField("topLevelChild").build()
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(deferredField).selection(childField2).build()
        Field root = Field.newField("rootField", selectionSet).build()

        when:
        Field result = (Field) astTransformer.transform(root, nestedSpecToTest)

        then:
        result != null
        result.getSelectionSet().getSelections().size() == 3
        ((Field)result.getSelectionSet().getSelections().get(0)).getName() == "topLevelDeferredChild"
        ((Field)result.getSelectionSet().getSelections().get(1)).getName() == "topLevelChild"
        ((Field)result.getSelectionSet().getSelections().get(2)).getName() == "__typename"
    }

    def "Throws exception if Field Has Defer Directive and nestedDefer is not Allowed"(){
        given:
        Field deferredField = Field.newField("topLevelDeferredChild").directive(enabledDirective).build()
        Field childField2 = Field.newField("topLevelChild").build()
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(deferredField).selection(childField2).build()
        Field root = Field.newField("rootField", selectionSet).build()

        when:
        astTransformer.transform(root, specToTest)

        then:
        def exception = thrown(GraphQLException)
        exception.getMessage() ==~ "Nested defers are currently unavailable."
    }

    def "Top Level Deferred Child FragmentSpread is Removed"(){
        given:
        FragmentSpread deferredFragment = FragmentSpread.newFragmentSpread("topLevelFragment")
                .directive(enabledDirective)
                .build()
        Field childField2 = Field.newField().name("topLevelChild").build()
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(deferredFragment).selection(childField2).build()
        Field root = Field.newField("rootField", selectionSet).build()

        when:
        Field result = (Field) astTransformer.transform(root, nestedSpecToTest)

        then:
        result != null
        result.getSelectionSet().getSelections().size() == 2
        ((Field)result.getSelectionSet().getSelections().get(0)).getName() == "topLevelChild"
        ((Field)result.getSelectionSet().getSelections().get(1)).getName() == "__typename"
    }

    def "Nested Deferred Child FragmentSpread is Removed"(){
        given:
        FragmentSpread deferredFragment = FragmentSpread.newFragmentSpread("nestedDeferredChild")
                .directive(enabledDirective)
                .build()
        Field childField2 = Field.newField().name("nestedChild").build()
        SelectionSet nestedSelectionSet = SelectionSet.newSelectionSet().selection(deferredFragment).selection(childField2).build()
        Field topLevelChild = Field.newField("topLevelChild", nestedSelectionSet).build()

        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(topLevelChild).build()
        Field root = Field.newField("rootField", selectionSet).build()

        when:
        Field result = (Field) astTransformer.transform(root, nestedSpecToTest)

        then:
        result != null
        result.getSelectionSet().getSelections().size() == 2

        Field resultTopChild = (Field)result.getSelectionSet().getSelections().get(0)
        topLevelChild.getName() == "topLevelChild"
        ((Field)result.getSelectionSet().getSelections().get(1)).getName() == "__typename"

        resultTopChild.getSelectionSet().getSelections().size() == 2
        ((Field) resultTopChild.getSelectionSet().getSelections().get(0)).getName() == "nestedChild"
        ((Field) resultTopChild.getSelectionSet().getSelections().get(1)).getName() == "__typename"

    }

    def "Removes Disabled Defer Directive From Child FragmentSpread"(){
        given:
        FragmentSpread deferredFragment = FragmentSpread.newFragmentSpread("DeferredFrag")
                .directive(disabledDirective)
                .build()
        Field childField2 = Field.newField("topLevelChild").build()
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(deferredFragment).selection(childField2).build()
        Field root = Field.newField("rootField", selectionSet).build()

        when:
        Field result = (Field) astTransformer.transform(root, nestedSpecToTest)

        then:
        result != null
        result.getSelectionSet().getSelections().size() == 3
        ((FragmentSpread)result.getSelectionSet().getSelections().get(0)).getName() == "DeferredFrag"
        ((Field)result.getSelectionSet().getSelections().get(1)).getName() == "topLevelChild"
        ((Field)result.getSelectionSet().getSelections().get(2)).getName() == "__typename"
    }

    def "Throws exception if FragmentSpread Has Defer Directive and nestedDefer is not Allowed"(){
        given:
        FragmentSpread deferredFragment = FragmentSpread.newFragmentSpread("topLevelFragment")
                .directive(enabledDirective)
                .build()
        Field childField2 = Field.newField().name("topLevelChild").build()
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(deferredFragment).selection(childField2).build()
        Field root = Field.newField("rootField", selectionSet).build()

        when:
        astTransformer.transform(root, specToTest)

        then:
        def exception = thrown(GraphQLException)
        exception.getMessage() ==~ "Nested defers are currently unavailable."
    }

    def "Top Level Deferred Child InlineFragment is Removed"(){
        given:
        SelectionSet deferredSelectionSet = SelectionSet.newSelectionSet()
                .selection(Field.newField("deferredFragField").build())
                .build()
        InlineFragment deferredFragment = InlineFragment.newInlineFragment().selectionSet(deferredSelectionSet)
                .directive(enabledDirective)
                .build()
        Field childField2 = Field.newField().name("topLevelChild").build()
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(deferredFragment).selection(childField2).build()
        Field root = Field.newField("rootField", selectionSet).build()

        when:
        Field result = (Field) astTransformer.transform(root, nestedSpecToTest)

        then:
        result != null
        result.getSelectionSet().getSelections().size() == 2
        ((Field)result.getSelectionSet().getSelections().get(0)).getName() == "topLevelChild"
        ((Field)result.getSelectionSet().getSelections().get(1)).getName() == "__typename"
    }

    def "Nested Deferred Child InlineFragment is Removed"(){
        given:
        SelectionSet deferredSelectionSet = SelectionSet.newSelectionSet()
                .selection(Field.newField("deferredFragField").build())
                .build()
        InlineFragment deferredFragment = InlineFragment.newInlineFragment().selectionSet(deferredSelectionSet)
                .directive(enabledDirective)
                .build()
        Field childField2 = Field.newField().name("nestedChild").build()
        SelectionSet nestedSelectionSet = SelectionSet.newSelectionSet().selection(deferredFragment).selection(childField2).build()
        Field topLevelChild = Field.newField("topLevelChild", nestedSelectionSet).build()

        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(topLevelChild).build()
        Field root = Field.newField("rootField", selectionSet).build()

        when:
        Field result = (Field) astTransformer.transform(root, nestedSpecToTest)

        then:
        result != null
        result.getSelectionSet().getSelections().size() == 2

        Field resultTopChild = (Field)result.getSelectionSet().getSelections().get(0)
        topLevelChild.getName() == "topLevelChild"
        ((Field)result.getSelectionSet().getSelections().get(1)).getName() == "__typename"

        resultTopChild.getSelectionSet().getSelections().size() == 2
        ((Field) resultTopChild.getSelectionSet().getSelections().get(0)).getName() == "nestedChild"
        ((Field) resultTopChild.getSelectionSet().getSelections().get(1)).getName() == "__typename"

    }

    def "Removes Disabled Defer Directive From Child InlineFragment"(){
        given:
        SelectionSet deferredSelectionSet = SelectionSet.newSelectionSet()
                .selection(Field.newField("deferredFragField").build())
                .build()
        InlineFragment deferredFragment = InlineFragment.newInlineFragment().selectionSet(deferredSelectionSet)
                .directive(disabledDirective)
                .build()
        Field childField2 = Field.newField("topLevelChild").build()
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(deferredFragment).selection(childField2).build()
        Field root = Field.newField("rootField", selectionSet).build()

        when:
        Field result = (Field) astTransformer.transform(root, nestedSpecToTest)

        then:
        result != null
        result.getSelectionSet().getSelections().size() == 3
        InlineFragment fragment = (InlineFragment)result.getSelectionSet().getSelections().get(0)
        ((Field)fragment.getSelectionSet().getSelections().get(0)).getName() == "deferredFragField"
        ((Field)fragment.getSelectionSet().getSelections().get(1)).getName() == "__typename"
        ((Field)result.getSelectionSet().getSelections().get(1)).getName() == "topLevelChild"
        ((Field)result.getSelectionSet().getSelections().get(2)).getName() == "__typename"
    }

    def "Throws exception if InlineFragment Has Defer Directive and nestedDefer is not Allowed"(){
        given:
        SelectionSet deferredSelectionSet = SelectionSet.newSelectionSet()
                .selection(Field.newField("deferredFragField").build())
                .build()
        InlineFragment deferredFragment = InlineFragment.newInlineFragment().selectionSet(deferredSelectionSet)
                .directive(enabledDirective)
                .build()
        Field childField2 = Field.newField().name("topLevelChild").build()
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(deferredFragment).selection(childField2).build()
        Field root = Field.newField("rootField", selectionSet).build()

        when:
        astTransformer.transform(root, specToTest)

        then:
        def exception = thrown(GraphQLException)
        exception.getMessage() ==~ "Nested defers are currently unavailable."
    }
}
