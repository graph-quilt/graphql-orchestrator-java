package com.intuit.graphql.orchestrator.integration.schemastitcher

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.federation.exceptions.BaseTypeNotFoundException
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher
import helpers.BaseIntegrationTestSpecification

class EntityTypeMergingSpec extends BaseIntegrationTestSpecification {

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
        
        type A @key(fields: "a1") @extends {
            a1: String @external
            a3: [B]
        }
        
        type B {
            b1: Boolean
        }
    '''

    def "Cannot merged Base Entity Types"() {
        given:
        def anotherSchemaWithSameBaseTypeDefinition = '''
            type Query {  
                someA: A                 
            }
            
            type A @key(fields: "a1") {
                a1: String
                a2: Int
            }
        '''

        ServiceProvider testServiceA = createQueryMatchingService("TEST_SERVICE_A",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, SCHEMA_WITH_BASE_ENTITY, null)
        ServiceProvider testServiceB = createQueryMatchingService("TEST_SERVICE_B",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, anotherSchemaWithSameBaseTypeDefinition, null)
        List<ServiceProvider> services = [testServiceA, testServiceB]

        when:
        SchemaStitcher.newBuilder()
                .services(services).build().stitchGraph();

        then:
        def e = thrown(TypeConflictException.class)
        e.message == "Type [name:A, type:ObjectTypeDefinition, description:[TEST_SERVICE_B]] is conflicting with existing type [name:A, type:ObjectTypeDefinition, description:[TEST_SERVICE_A]]. Two schemas cannot own an entity."
    }

    def "Scheme Stitching fails if Base type is not found"() {
        given:

        ServiceProvider testServiceA = createQueryMatchingService("TEST_SERVICE_A",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, SCHEMA_WITH_ENTITY_EXTENSION, null)
        List<ServiceProvider> services = [testServiceA]

        when:
        SchemaStitcher.newBuilder()
                .services(services).build().stitchGraph();

        then:
        def e = thrown(BaseTypeNotFoundException.class)
        e.message == "Base type not found.  typename=A, serviceNamespace=TEST_SERVICE_A"
    }

}
