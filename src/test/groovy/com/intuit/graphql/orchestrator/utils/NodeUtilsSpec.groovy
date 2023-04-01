package com.intuit.graphql.orchestrator.utils

import graphql.language.Directive
import graphql.language.Field
import graphql.language.SelectionSet
import helpers.BaseIntegrationTestSpecification

import static NodeUtils.removeDirectiveFromNode

class NodeUtilsSpec extends BaseIntegrationTestSpecification {
    def "removeDirectiveFromNode throws exception if node is null"(){
        when:
            removeDirectiveFromNode(null, "")
        then:
            thrown(RuntimeException)
    }

    def "removeDirectiveFromNode throws exception if node isn't a directiveContainer"(){
        when:
        SelectionSet selectionSet = SelectionSet.newSelectionSet().build()
        removeDirectiveFromNode(selectionSet, "")
        then:
        thrown(RuntimeException)
    }

    def "removeDirectiveFromNode returns original node if directive isn't found"(){
        given:
            Directive dir1 = Directive.newDirective().name("test1").build()
            Directive dir2 = Directive.newDirective().name("test2").build()
            Field testField = Field.newField("testField")
                    .directive(dir1)
                    .directive(dir2)
                    .build()

        when:
            Field result = removeDirectiveFromNode(testField, "test3")

        then:
            result.getName() == "testField"
            result.getDirectives().size() == 2
            result.getDirectives().get(0).name == "test1"
            result.getDirectives().get(1).name == "test2"
    }

    def "removeDirectiveFromNode returns node without desired directive name"(){
        given:
        Directive dir1 = Directive.newDirective().name("test1").build()
        Directive dir2 = Directive.newDirective().name("test2").build()
        Field testField = Field.newField("testField")
                .directive(dir1)
                .directive(dir2)
                .build()

        when:
        Field result = removeDirectiveFromNode(testField, "test1")

        then:
        result.getName() == "testField"
        result.getDirectives().size() == 1
        result.getDirectives().get(0).name == "test2"
    }
}
