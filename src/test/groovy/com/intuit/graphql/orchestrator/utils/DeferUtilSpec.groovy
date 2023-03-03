package com.intuit.graphql.orchestrator.utils

import graphql.language.*
import helpers.BaseIntegrationTestSpecification

import static com.intuit.graphql.orchestrator.deferDirective.DeferUtil.containsEnabledDeferDirective
import static com.intuit.graphql.orchestrator.deferDirective.DeferUtil.hasNonDeferredSelection

class DeferUtilSpec extends BaseIntegrationTestSpecification{
    Directive enabledDefer = Directive.newDirective().name("defer").build()
    Directive disabledDefer = Directive.newDirective()
            .name("defer")
            .argument(
                    Argument.newArgument("if", BooleanValue.of(false))
                    .build())
            .build()

    def "hasNonDeferredSelection throws exception if node is null"(){
        when:
        hasNonDeferredSelection(null)

        then:
        thrown(NullPointerException)
    }

    def "hasNonDeferredSelection return true if node has no child selections"(){
        when:
        Field node = Field.newField("testField").build()

        then:
        hasNonDeferredSelection(node)
    }

    def "hasNonDeferredSelection return true if node has child selections do not have defer"(){
        when:
            Field childField1 = Field.newField("childField").build()
            Field childField2 = Field.newField("childField").build()
            SelectionSet ss = SelectionSet.newSelectionSet()
                    .selection(childField1)
                    .selection(childField2)
                    .build()
            Field parentField = Field.newField("parentNode", ss).build()

        then:
            hasNonDeferredSelection(parentField)
    }

    def "hasNonDeferredSelection return true if node has child selections with one defer and others arent"(){
        when:
        Field childField1 = Field.newField("childField").directive(enabledDefer).build()
        Field childField2 = Field.newField("childField").build()
        SelectionSet ss = SelectionSet.newSelectionSet()
                .selection(childField1)
                .selection(childField2)
                .build()
        Field parentField = Field.newField("parentNode", ss).build()

        then:
        hasNonDeferredSelection(parentField)
    }

    def "hasNonDeferredSelection return true if node has child selections that are disabled"(){
        when:
        Field childField1 = Field.newField("childField").directive(disabledDefer).build()
        Field childField2 = Field.newField("childField").build()
        SelectionSet ss = SelectionSet.newSelectionSet()
                .selection(childField1)
                .selection(childField2)
                .build()
        Field parentField = Field.newField("parentNode", ss).build()

        then:
        hasNonDeferredSelection(parentField)
    }

    def "hasNonDeferredSelection return false if node has child selections and all are deferred"(){
        when:
        Field childField1 = Field.newField("childField1").directive(enabledDefer).build()
        Field childField2 = Field.newField("childField2").directive(enabledDefer).build()
        SelectionSet ss = SelectionSet.newSelectionSet()
                .selection(childField1)
                .selection(childField2)
                .build()
        Field parentField = Field.newField("parentNode", ss).build()

        then:
        !hasNonDeferredSelection(parentField)
    }

    def "containsEnabledDeferDirective returns false for non DirectiveContainer"() {
        when:
        Document node = Document.newDocument().build()

        then:
        !containsEnabledDeferDirective(node)
    }

    def "FragmentSpread with defer returns true"() {
        when:
        FragmentSpread node = FragmentSpread.newFragmentSpread("testFragment").directive(enabledDefer).build()

        then:
        containsEnabledDeferDirective(node)
    }

    def "FragmentSpread with disabled defer returns false"() {
        when:
        FragmentSpread node = FragmentSpread.newFragmentSpread("testFragment").directive(disabledDefer).build()

        then:
        !containsEnabledDeferDirective(node)
    }

    def "FragmentSpread without defer directive returns false"() {
        when:
        FragmentSpread node = FragmentSpread.newFragmentSpread("testFragment")
                .directive(Directive.newDirective().name("testDir").build())
                .build()

        then:
        !containsEnabledDeferDirective(node)
    }

    def "InlineFragment with defer returns true"() {
        when:
        InlineFragment node = InlineFragment.newInlineFragment().directive(enabledDefer).build()

        then:
        containsEnabledDeferDirective(node)
    }

    def "InlineFragment with disabled defer returns false"() {
        when:
        InlineFragment node = InlineFragment.newInlineFragment().directive(disabledDefer).build()

        then:
        !containsEnabledDeferDirective(node)
    }

    def "InlineFragment without defer directive returns false"() {
        when:
        InlineFragment node = InlineFragment.newInlineFragment()
                .directive(Directive.newDirective().name("testDir").build())
                .build()

        then:
        !containsEnabledDeferDirective(node)
    }

    def "Field with defer returns true"() {
        when:
        Field node = Field.newField("testField").directive(enabledDefer).build()

        then:
        containsEnabledDeferDirective(node)
    }

    def "Field with disabled defer returns false"() {
        when:
        Field node = Field.newField("testField").directive(disabledDefer).build()

        then:
        !containsEnabledDeferDirective(node)
    }

    def "Field without defer directive returns false"() {
        when:
        Field node = Field.newField("testField")
                .directive(Directive.newDirective().name("testDir").build())
                .build()

        then:
        !containsEnabledDeferDirective(node)
    }
}
