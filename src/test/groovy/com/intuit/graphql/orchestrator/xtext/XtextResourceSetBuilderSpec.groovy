package com.intuit.graphql.orchestrator.xtext

import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.schema.SchemaParseException
import org.eclipse.xtext.resource.XtextResourceSet
import spock.lang.Specification

class XtextResourceSetBuilderSpec extends Specification {

    def "builds Empty Resource Set"() {
        given:
        XtextResourceSet emptyResourceSet = XtextResourceSetBuilder.newBuilder().build()

        expect:
        emptyResourceSet != null
    }

    def "builds Single File Resource Set"() {
        given:
        XtextResourceSet resourceSet = XtextResourceSetBuilder.newBuilder()
                .file("top_level/eps/schema2.graphqls",TestHelper.getResourceAsString("top_level/eps/schema2.graphqls"))
                .build()

        expect:
        resourceSet != null
        resourceSet.getResources().size() == 1
    }

    def "builds Multiple File Resource Set"() {
        given:
        XtextResourceSet resourceSet = XtextResourceSetBuilder.newBuilder()
                .files(TestHelper.getFileMapFromList("top_level/eps/schema2.graphqls", "top_level/person/schema1.graphqls"))
                .build()

        expect:
        resourceSet != null
        resourceSet.getResources().size() == 2
    }

    def "throws Null Pointer Exception When File Is Null"() {
        given:
        XtextResourceSetBuilder builder = XtextResourceSetBuilder.newBuilder()

        when:
        builder.file("foo",null)

        then:
        thrown(NullPointerException)
    }

    def "throws Exception When File Name Is Null"() {
        given:
        XtextResourceSetBuilder builder = XtextResourceSetBuilder.newBuilder()

        when:
        builder.file(      null,"foo")

        then:
        thrown(NullPointerException)
    }

    def "throws Exception When Validation Fails When Type Is Undefined"() {
        given:
        XtextResourceSetBuilder builder = XtextResourceSetBuilder.newBuilder()
                .file("foo",  "type foo { abc: Inta }")

        when:
        builder.build()

        then:
        def exception = thrown(SchemaParseException)
        exception.message.contains("ERROR:Couldn't resolve reference to TypeDefinition 'Inta'")
    }

    def "throws Exception When Validation Fails With Duplicate Types"() {
        given:
        XtextResourceSetBuilder builder = XtextResourceSetBuilder.newBuilder()
                .file("foo",  "type foo { bar: String } type foo { abc: Int }")

        when:
        builder.build()

        then:
        def exception = thrown(SchemaParseException.class)
        exception.message.contains("Duplicate name in schema: foo")
    }

}
