package com.intuit.graphql.orchestrator.utils

import graphql.ExecutionInput
import graphql.parser.InvalidSyntaxException
import reactor.test.StepVerifier
import spock.lang.Specification

class MultiEIGeneratorSpec extends Specification {

    MultiEIGenerator multiEIGenerator

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
        multiEIGenerator = new MultiEIGenerator(ei)

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
        multiEIGenerator = new MultiEIGenerator(ei)

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
        multiEIGenerator = new MultiEIGenerator(ei)

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
        multiEIGenerator = new MultiEIGenerator(ei)

        then:
        StepVerifier.create(multiEIGenerator.generateEIs())
                .expectNextMatches( {emptyEi -> emptyEi.getQuery() == query})
                .expectError(InvalidSyntaxException.class)
                .verify()
    }
}
