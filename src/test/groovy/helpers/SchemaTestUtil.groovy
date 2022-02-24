package helpers

import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.errors.SchemaProblem
import spock.lang.Specification

class SchemaTestUtil extends Specification {

    static RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().build()

    static defaultOptions = SchemaGenerator.Options.defaultOptions().enforceSchemaDirectives(false)

    static SchemaParser schemaParser = new SchemaParser()

    static GraphQLSchema createGraphQLSchema(String sdl) {
        try {
            def registry = schemaParser.parse(sdl)
            return new SchemaGenerator().makeExecutableSchema(defaultOptions, registry, runtimeWiring)
        } catch (SchemaProblem e) {
            assert false: "Failed to create schema: ${e}"
            return null
        }
    }
}
