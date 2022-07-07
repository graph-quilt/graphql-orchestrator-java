package com.intuit.graphql.orchestrator.datafetcher

import com.intuit.graphql.orchestrator.schema.SchemaParseException
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder
import spock.lang.Specification

import java.io.IOException

class SchemaParseExceptionSpec extends Specification {

    //  PIC::TODO:update
    def "test Thrown From Xtext Resource Set Builder"() {
        given:
        // have XtextResourceSetBuilder.createGraphqlResourceFromString throw an IOException
        // build method should throw a SchemaParseException when it catches the IOException
        XtextResourceSetBuilder spy = Spy(XtextResourceSetBuilder.newBuilder())

        String fileName = "somefile.graphqls"
        String content = "Query {id : String}}"

        spy.createGraphqlResourceFromString(_ as String, _ as String) >> { String source, String uri ->
            throw new IOException("Some IO Error")
        }

        when:
        spy.file(fileName, content).build()

        then:
        thrown(SchemaParseException)
    }

}
