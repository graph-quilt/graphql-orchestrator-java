package com.intuit.graphql.orchestrator.resolverdirective

import com.intuit.graphql.orchestrator.xtext.FieldContext
import spock.lang.Specification

class ResolverArgumentTypeMismatchSpec extends Specification {

    def "error Message Without Parent Context"() {
        given:
        final ResolverArgumentTypeMismatch error = new ResolverArgumentTypeMismatch("argName",
                new FieldContext("parentType", "fieldName"), "String", "ObjectType")

        expect:
        error.message.contains("Resolver argument 'argName' in 'parentType:fieldName': Expected type 'String' to be 'ObjectType'.")
    }

    def "error Message With Parent Context"() {
        given:
        FieldContext rootContext = new FieldContext("rootType", "rootField")
        FieldContext fieldContext = new FieldContext("parentType", "parentField")

        final ResolverArgumentTypeMismatch error = new ResolverArgumentTypeMismatch("argName",
                rootContext, fieldContext, "String", "Int")

        expect:
        error.message.contains(
                "Resolver argument 'argName' in 'rootType:rootField': Expected type 'String' in 'parentType:parentField' to be 'Int'.")
    }

    def "error Message create Without Parent Context"() {
        given:
        FieldContext fieldContext = new FieldContext("parentType", "fieldName")
        final ResolverArgumentTypeMismatch error = ResolverArgumentTypeMismatch.create("argName", fieldContext, null, "String", "ObjectType" )

        expect:
        error.message.contains("Resolver argument 'argName' in 'parentType:fieldName': Expected type 'String' to be 'ObjectType'.")
    }

    def "error Message create With Parent Context"() {
        given:
        FieldContext rootContext = new FieldContext("rootType", "rootField")
        FieldContext fieldContext = new FieldContext("parentType", "parentField")

        final ResolverArgumentTypeMismatch error = ResolverArgumentTypeMismatch.create("argName", fieldContext, rootContext, "String", "Int")

        expect:
        error.message.contains(
                "Resolver argument 'argName' in 'parentType:parentField': Expected type 'String' in 'rootType:rootField' to be 'Int'.")
    }
}
