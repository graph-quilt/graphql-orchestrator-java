package com.intuit.graphql.orchestrator.resolverdirective

import com.intuit.graphql.orchestrator.xtext.FieldContext
import spock.lang.Specification

class ResolverArgumentLeafTypeNotSameSpec extends Specification {

    def "produces Correct Error Message Without Parent Context"() {
        given:
        final ResolverArgumentLeafTypeNotSame error = new ResolverArgumentLeafTypeNotSame(
                "argName", new FieldContext("rootObject", "rootField"), "String", "ObjectType")

        expect:
        error.message.contains("Resolver argument 'argName' in 'rootObject:rootField': Expected 'String' to be 'ObjectType'.")
    }

    def "produces Correct Error Message With Parent Context"() {
        given:
        final ResolverArgumentLeafTypeNotSame error = new ResolverArgumentLeafTypeNotSame(
                "argName", new FieldContext("rootObject", "rootField"), new FieldContext("parentObject", "parentField"),
                "String", "Int")

        expect:
        error.message.contains("Resolver argument 'argName' in 'rootObject:rootField': Expected 'String' in 'parentObject:parentField' to be 'Int'.")
    }

    def "create method produces Correct Error Message Without Parent Context"() {
        given:
        final ResolverArgumentLeafTypeNotSame error = ResolverArgumentLeafTypeNotSame.create(
                "argName", new FieldContext("rootObject", "rootField"), null, "String", "ObjectType")

        expect:
        error.message.contains("Resolver argument 'argName' in 'rootObject:rootField': Expected 'String' to be 'ObjectType'.")
    }

    def "create method produces Correct Error Message With Parent Context"() {
        given:
        final ResolverArgumentLeafTypeNotSame error = ResolverArgumentLeafTypeNotSame.create(
                "argName", new FieldContext("rootObject", "rootField"), new FieldContext("parentObject", "parentField"),
                "String", "Int")

        expect:
        error.message.contains("Resolver argument 'argName' in 'rootObject:rootField': Expected 'String' in 'parentObject:parentField' to be 'Int'.")
    }
}
