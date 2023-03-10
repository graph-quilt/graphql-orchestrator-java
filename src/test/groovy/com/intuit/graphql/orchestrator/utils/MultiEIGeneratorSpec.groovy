package com.intuit.graphql.orchestrator.utils

import com.intuit.graphql.orchestrator.deferDirective.DeferOptions
import graphql.ExecutionInput
import graphql.parser.InvalidSyntaxException
import graphql.scalar.GraphqlStringCoercing
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import reactor.test.StepVerifier
import spock.lang.Specification

class MultiEIGeneratorSpec extends Specification {

    MultiEIGenerator multiEIGenerator
    DeferOptions options = DeferOptions.builder()
            .nestedDefersAllowed(true)
            .build()

    GraphQLScalarType scalarType = GraphQLScalarType.newScalar()
        .name("scale")
        .coercing(new GraphqlStringCoercing())
        .build()

    GraphQLFieldDefinition idField = GraphQLFieldDefinition.newFieldDefinition()
            .name("id")
            .type(scalarType)
            .build()

    GraphQLFieldDefinition nameField = GraphQLFieldDefinition.newFieldDefinition()
            .name("name")
            .type(scalarType)
            .build()

    GraphQLFieldDefinition typeField = GraphQLFieldDefinition.newFieldDefinition()
            .name("type")
            .type(scalarType)
            .build()

    GraphQLObjectType petType = GraphQLObjectType.newObject()
            .name("Pet")
            .field(idField)
            .field(nameField)
            .field(typeField)
            .build()

    GraphQLFieldDefinition petsQuery = GraphQLFieldDefinition.newFieldDefinition()
            .name("pets")
            .type(petType)
            .build()

    GraphQLObjectType queryType = GraphQLObjectType.newObject()
            .name("query")
            .field(petsQuery)
            .build()

    GraphQLSchema schema = GraphQLSchema.newSchema().query(queryType).additionalType(petType).build()



    def "Generator split query correctly"() {
        given:
        String query = '''
            query getPetsDeferred {
                pets {
                    id 
                    name 
                    type @defer
                }
            }
        '''

        String deferredQuery = "query getPetsDeferred {\n" +
                "  pets {\n" +
                "    type\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"

        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()

        when:
        multiEIGenerator = new MultiEIGenerator(ei, options, schema)

        then:
        StepVerifier.create(multiEIGenerator.generateEIs())
        .expectNextMatches({ response -> response.getQuery() == query })
        .expectNextMatches({ response -> response.getQuery() == deferredQuery })
        .verifyComplete()
    }

    def "Initial EI is processed before splitting query"(){
        given:
        String query = '''
            query getPetsDeferred {
                pets {
                    id 
                    name 
                    type @defer
                }
            }
        '''

        String deferredQuery = "query getPetsDeferred {\n" +
                "  pets {\n" +
                "    type\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"

        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()

        long timeEmitted = 0;

        when:
        multiEIGenerator = new MultiEIGenerator(ei, options, schema)

        then:
        StepVerifier.create(multiEIGenerator.generateEIs())
        .expectNextMatches({ response ->
            timeEmitted = System.currentTimeMillis()
            return response.query == query && response
        })
        .expectAccessibleContext()
        .assertThat({ e -> this.multiEIGenerator.timeProcessedSplit > timeEmitted })
        .then()
        .expectNextMatches({ response -> response.getQuery() == deferredQuery })
        .verifyComplete()
    }

    def "EI w/o defer flux completes and only emits 1 object"(){
        given:
        String query = '''
            query getPetsDeferred {
                pets {
                    id 
                    name 
                    type
                }
            }
        '''

        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()

        when:
        multiEIGenerator = new MultiEIGenerator(ei, options, schema)

        then:
        StepVerifier.create(multiEIGenerator.generateEIs())
        .expectNextMatches({ response -> response.query == query})
        .verifyComplete()
    }

    def "emits error if it throws error when trying to split ei"(){
        given:
        String query = ""

        ExecutionInput ei = ExecutionInput.newExecutionInput(query).build()

        when:
        multiEIGenerator = new MultiEIGenerator(ei, options, schema)

        then:
        StepVerifier.create(multiEIGenerator.generateEIs())
                .expectNextMatches( {emptyEi -> emptyEi.getQuery() == query})
                .expectError(InvalidSyntaxException.class)
                .verify()
    }
}
