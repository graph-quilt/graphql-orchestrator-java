package com.intuit.graphql.orchestrator.integration.schemastitcher

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.schema.RuntimeGraph
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher
import graphql.schema.GraphQLObjectType
import helpers.BaseIntegrationTestSpecification

import static java.util.stream.Collectors.toList

class EntityMultiServiceFieldReferences extends BaseIntegrationTestSpecification {

    static String SCHEMA_WITH_BASE_ENTITY = '''
        type Query {
            topField1(id: String): A  
            topField2: A                 
        }
        
        type A @key(fields: "a1") {
            a1: String
            a2: Int
        }
    '''

    static String SCHEMA_WITH_ENTITY_EXTENSION = '''
        type Query {
            topField3(id: String): A
            topField4(id: String): A
        }
        
        type Mutation {
            addData(id: String): A
        }
        
        type A @key(fields: "a1") @extends {
            a1: String @external
            a3: [B]
        }
        
        type B {
            b1: Boolean
        }
    '''

    def "Successful Base Entity and Extension types merging with type references updated to Base type"() {
        given:
        ServiceProvider testServiceA = createQueryMatchingService("TEST_SERVICE_A",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, SCHEMA_A, null)
        ServiceProvider testServiceB = createQueryMatchingService("TEST_SERVICE_B",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, SCHEMA_B, null)
        List<ServiceProvider> services = [testServiceA, testServiceB]

        when:
        RuntimeGraph actualRuntimeGraph = SchemaStitcher.newBuilder()
                .services(services).build().stitchGraph();

        then:
        GraphQLObjectType typeA = actualRuntimeGraph.getType("A")
        typeA == actualRuntimeGraph.getExecutableSchema().getType("A")
        typeA.getFieldDefinitions().size() == 3
        typeA.getFieldDefinitions().stream()
                .map({ graphqlFieldDefinition -> graphqlFieldDefinition.getName() })
                .collect(toList()) containsAll(["a1", "a2", "a3"])

        GraphQLObjectType queryType = actualRuntimeGraph.getOperation(Operation.QUERY)
        queryType.getFieldDefinition("topField1").getType() == typeA
        queryType.getFieldDefinition("topField2").getType() == typeA
        queryType.getFieldDefinition("topField3").getType() == typeA
        queryType.getFieldDefinition("topField4").getType() == typeA

        GraphQLObjectType mutationType = actualRuntimeGraph.getOperation(Operation.MUTATION)
        mutationType.getFieldDefinition("addData").getType() == typeA

        where:
        SCHEMA_A                     | SCHEMA_B
        SCHEMA_WITH_ENTITY_EXTENSION | SCHEMA_WITH_BASE_ENTITY
        SCHEMA_WITH_BASE_ENTITY      | SCHEMA_WITH_ENTITY_EXTENSION
    }
}
