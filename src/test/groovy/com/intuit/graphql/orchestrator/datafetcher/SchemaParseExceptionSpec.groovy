package com.intuit.graphql.orchestrator.datafetcher

import com.intuit.graphql.orchestrator.schema.SchemaParseException
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder
import spock.lang.Specification

import java.io.IOException

class SchemaParseExceptionSpec extends Specification {

    //  PIC::TODO fix
    def "test Thrown From Xtext Resource Set Builder"() {
        given:
        // have XtextResourceSetBuilder.createGraphqlResourceFromString throw an IOException
        // build method should throw a SchemaParseException when it catches the IOException
        XtextResourceSetBuilder mock = Mock(XtextResourceSetBuilder.newBuilder())

        //doThrow(new IOException("Some IO Error")).when(mock, "createGraphqlResourceFromString", anyString(), anyString())
        mock.createGraphqlResourceFromString(_, _) >> { String source, String uri ->
            throw new IOException("Some IO Error")
        }

        when:
        mock.file("somefile.graphqls","{Query {id : String}}").build()

        then:
        thrown(SchemaParseException)
    }

}
