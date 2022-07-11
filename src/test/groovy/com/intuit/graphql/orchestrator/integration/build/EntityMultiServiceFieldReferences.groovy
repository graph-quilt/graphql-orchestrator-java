package com.intuit.graphql.orchestrator.integration.build


import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.schema.RuntimeGraph
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import helpers.BaseIntegrationTestSpecification

import static java.util.stream.Collectors.toList

class EntityMultiServiceFieldReferences extends BaseIntegrationTestSpecification {

    String SCHEMA_SVC_A = '''
        type Query {
            topField1(id: String): A  
            someA: A                 
        }
        
        type A @key(fields: "a1") {
            a1: String
            a2: Int
        }
    '''

    String SCHEMA_SVC_B = '''
        type Query {
            topField2(id: String): A
            topField3(id: String): A
        }
        
        type A @key(fields: "a1") @extends {
            a1: String @external
            a3: [B]
        }
        
        type B {
            b1: Boolean
        }
    '''

    ServiceProvider testServiceA, testServiceB
    List<ServiceProvider> services

    void setup() {
        testServiceA = createQueryMatchingService("testServiceA",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, SCHEMA_SVC_A, null)
        testServiceB = createQueryMatchingService("testServiceB",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, SCHEMA_SVC_B, null)
        services = [testServiceA, testServiceB]
    }

    def "test"() {
        given:
        SchemaStitcher schemaStitcher = SchemaStitcher.newBuilder()
                .services(services).build()

        when:
        RuntimeGraph actualRuntimeGraph = schemaStitcher.stitchGraph();

        then:
        GraphQLObjectType queryType = actualRuntimeGraph.getOperation(Operation.QUERY)
        GraphQLFieldDefinition actualTopField1 = queryType.getFieldDefinition("topField1")
        GraphQLFieldDefinition actualTopField2 = queryType.getFieldDefinition("topField2")
        GraphQLFieldDefinition actualTopField3 = queryType.getFieldDefinition("topField3")
        actualTopField1.getType() == actualTopField2.getType()
        actualTopField1.getType() == actualTopField3.getType()
        GraphQLObjectType aType = actualTopField1.getType()
        aType.getFieldDefinitions().size() == 3
        aType.getFieldDefinitions().stream()
                .map({ graphqlFieldDefinition -> graphqlFieldDefinition.getName() })
                .collect(toList()) containsAll(["a1","a2","a3"])
    }

}
