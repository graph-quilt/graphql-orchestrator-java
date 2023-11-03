package com.intuit.graphql.orchestrator.utils


import graphql.language.*
import spock.lang.Specification

class QueryDirectivesUtilSpec extends Specification {
    BooleanValue booleanTrue = BooleanValue.newBooleanValue(true).build()
    BooleanValue booleanFalse = BooleanValue.newBooleanValue(false).build()
    VariableReference variableReference = VariableReference.newVariableReference().name("variable").build()

    Argument ifTrueArg = Argument.newArgument("if", booleanTrue).build()
    Argument ifFalseArg = Argument.newArgument("if", booleanFalse).build()
    Argument ifRefArg = Argument.newArgument("if", variableReference).build()

    Directive skipTrueDirective = Directive.newDirective().name("skip").argument(ifTrueArg).build()
    Directive skipFalseDirective = Directive.newDirective().name("skip").argument(ifFalseArg).build()
    Directive skipRefDirective = Directive.newDirective().name("skip").argument(ifRefArg).build()

    Directive includeTrueDirective = Directive.newDirective().name("include").argument(ifTrueArg).build()
    Directive includeFalseDirective = Directive.newDirective().name("include").argument(ifFalseArg).build()
    Directive includeRefDirective = Directive.newDirective().name("include").argument(ifRefArg).build()

    def "shouldIgnoreNode node without skip and includes returns false"(){
        given:
        Field node = Field.newField("test").build()

        when:
        boolean result = QueryDirectivesUtil.shouldIgnoreNode(node, new HashMap<>())
        then:
        !result
    }
    def "shouldIgnoreNode node with skip and if as true returns true"(){
        given:
        Field node = Field.newField("test").directive(skipTrueDirective).build()

        when:
        boolean result = QueryDirectivesUtil.shouldIgnoreNode(node, new HashMap<>())
        then:
        result
    }
    def "shouldIgnoreNode node with skip and if as false returns false"(){
        given:
        Field node = Field.newField("test").directive(skipFalseDirective).build()

        when:
        boolean result = QueryDirectivesUtil.shouldIgnoreNode(node, new HashMap<>())
        then:
        !result
    }
    def "shouldIgnoreNode node with skip and if as ref as true returns true"(){
        given:
        Field node = Field.newField("test").directive(skipRefDirective).build()

        when:
        HashMap<String, Object> variableMap = new HashMap<>()
        variableMap.put("variable", true)
        boolean result = QueryDirectivesUtil.shouldIgnoreNode(node, variableMap)
        then:
        result
    }
    def "shouldIgnoreNode node with skip and if as ref as false returns false"(){
        given:
        Field node = Field.newField("test").directive(skipRefDirective).build()

        when:
        HashMap<String, Object> variableMap = new HashMap<>()
        variableMap.put("variable", false)
        boolean result = QueryDirectivesUtil.shouldIgnoreNode(node, variableMap)
        then:
        !result
    }
    def "shouldIgnoreNode node with skip and if as ref as null returns true"(){
        given:
        Field node = Field.newField("test").directive(skipRefDirective).build()

        when:
        HashMap<String, Object> variableMap = new HashMap<>()
        boolean result = QueryDirectivesUtil.shouldIgnoreNode(node, variableMap)
        then:
        result
    }
    def "shouldIgnoreNode node with include and if as true returns false"(){
        given:
        Field node = Field.newField("test").directive(includeTrueDirective).build()

        when:
        boolean result = QueryDirectivesUtil.shouldIgnoreNode(node, new HashMap<>())
        then:
        !result
    }
    def "shouldIgnoreNode node with include and if as false returns true"(){
        given:
        Field node = Field.newField("test").directive(includeFalseDirective).build()

        when:
        boolean result = QueryDirectivesUtil.shouldIgnoreNode(node, new HashMap<>())
        then:
        result
    }
    def "shouldIgnoreNode node with include and if as ref as true returns false"(){
        given:
        Field node = Field.newField("test").directive(includeRefDirective).build()

        when:
        HashMap<String, Object> variableMap = new HashMap<>()
        variableMap.put("variable", true)
        boolean result = QueryDirectivesUtil.shouldIgnoreNode(node, variableMap)
        then:
        !result
    }
    def "shouldIgnoreNode node with include and if as ref as false returns true"(){
        given:
        Field node = Field.newField("test").directive(includeRefDirective).build()

        when:
        HashMap<String, Object> variableMap = new HashMap<>()
        variableMap.put("variable", false)
        boolean result = QueryDirectivesUtil.shouldIgnoreNode(node, variableMap)
        then:
        result
    }
    def "shouldIgnoreNode node with include and if as ref as null returns true"(){
        given:
        Field node = Field.newField("test").directive(includeRefDirective).build()

        when:
        HashMap<String, Object> variableMap = new HashMap<>()
        boolean result = QueryDirectivesUtil.shouldIgnoreNode(node, variableMap)
        then:
        result
    }
}
