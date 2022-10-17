package com.intuit.graphql.orchestrator.fieldresolver

import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.batch.GraphQLTestUtil
import graphql.language.OperationDefinition
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.TestHelper.schema

class TypeNameInjectorSpec extends Specification {

    private String schema = '''
        type Query {
          foo: Foo
          complexFoo: [Foo!]!
          fooUnion: Union
        }

        interface Foo {
          a: String
        }

        type Bar implements Foo {
          a: String
          b: String
        }

        union Union = Bar
    '''

    private RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
            .type("Foo", { wiring -> wiring.typeResolver({ env -> env.getSchema().getObjectType("Bar") }) })
            .type("Union", { wiring -> wiring.typeResolver({ env -> env.getSchema().getObjectType("Bar") }) })
            .build()

    private GraphQLSchema graphQLSchema

    def setup() {
        graphQLSchema = schema(schema, runtimeWiring)
    }

    def "adds Typename To Query with TypeNameInjector"() {
        given:
        final String query = '''
            {
                foo {
                    a
                }
            }
        '''
        final OperationDefinition queryOperation = TestHelper.query(query)

        TypenameInjector typenameInjector = new TypenameInjector()

        when:
        final OperationDefinition definition = typenameInjector
                .process(queryOperation, graphQLSchema, Collections.emptyMap(), Collections.emptyMap())

        final List<String> preOrderResult = GraphQLTestUtil
                .printPreOrder(definition, graphQLSchema, Collections.emptyMap())

        then:
        preOrderResult == ["foo", "a", "__typename"]
    }
}
