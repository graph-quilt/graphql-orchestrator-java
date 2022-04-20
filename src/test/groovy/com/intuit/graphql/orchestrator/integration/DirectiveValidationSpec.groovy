package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.schema.SchemaParseException
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher
import helpers.BaseIntegrationTestSpecification

class DirectiveValidationSpec extends BaseIntegrationTestSpecification {

    def testSchema = """
        type  Foo1 {
            foo1: String @customDir(reason : "reason `in markdown syntax`")
        }
        
        directive @customDir(reason: String = "No longer supported") on FIELD_DEFINITION | ENUM_VALUE
        directive @customDir(reason: String = "No longer supported") on FIELD_DEFINITION | ENUM_VALUE 
    """

    def "SchemaParseException is thrown for duplicate directive definition"() {
        given:
        testService = createSimpleMockService(testSchema, Collections.emptyMap())

        when:
        SchemaStitcher schemaStitcher = SchemaStitcher.newBuilder()
                .service(testService)
                .build()
        schemaStitcher.stitchGraph()

        then:
        final SchemaParseException exception = thrown()
        exception.message.contains("Duplicate directives in schema")
    }

}
