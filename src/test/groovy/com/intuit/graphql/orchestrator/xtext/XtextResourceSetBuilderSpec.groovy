package com.intuit.graphql.orchestrator.xtext

import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.schema.SchemaParseException
import org.eclipse.xtext.resource.XtextResourceSet
import spock.lang.Specification

class XtextResourceSetBuilderSpec extends Specification {

    void buildsEmptyResourceSet() {
        given:
        XtextResourceSet emptyResourceSet = XtextResourceSetBuilder.newBuilder().build()

        expect:
        emptyResourceSet != null
    }

    void buildsSingleFileResourceSet() {
        given:
        XtextResourceSet resourceSet = XtextResourceSetBuilder.newBuilder()
                .file("top_level/eps/schema2.graphqls",TestHelper.getResourceAsString("top_level/eps/schema2.graphqls"))
                .build()

        expect:
        resourceSet != null
        resourceSet.getResources().size() == 1
    }

    void buildsMultipleFileResourceSet() {
        given:
        XtextResourceSet resourceSet = XtextResourceSetBuilder.newBuilder()
                .files(TestHelper.getFileMapFromList("top_level/eps/schema2.graphqls", "top_level/person/schema1.graphqls"))
                .build()

        expect:
        resourceSet != null
        resourceSet.getResources().size() == 2
    }

    void throwsNullPointerExceptionWhenFileIsNull() {
        given:
        XtextResourceSetBuilder builder = XtextResourceSetBuilder.newBuilder()

        when:
        builder.file("foo",null)

        then:
        thrown(NullPointerException)
    }

    void throwsExceptionWhenFileNameIsNull() {
        given:
        XtextResourceSetBuilder builder = XtextResourceSetBuilder.newBuilder()

        when:
        builder.file(      null,"foo")

        then:
        thrown(NullPointerException)
    }

    void throwsExceptionWhenValidationFailsWhenTypeIsUndefined() {
        given:
        XtextResourceSetBuilder builder = XtextResourceSetBuilder.newBuilder()
                .file("foo",  "type foo { abc: Inta }")

        when:
        builder.build()

        then:
        def exception = thrown(SchemaParseException)
        exception.message.contains("ERROR:Couldn't resolve reference to TypeDefinition 'Inta'")
    }

    void throwsExceptionWhenValidationFailsWithDuplicateTypes() {
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
