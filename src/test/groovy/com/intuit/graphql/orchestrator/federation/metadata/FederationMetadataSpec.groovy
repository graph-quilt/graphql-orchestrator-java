package com.intuit.graphql.orchestrator.federation.metadata

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestServiceProvider
import graphql.language.Field
import spock.lang.Specification

class FederationMetadataSpec extends Specification {

    def "test getBaseServiceProvider of entity extension metadata"() {
        given:
        ServiceProvider serviceProvider = TestServiceProvider.newBuilder().build()
        FederationMetadata federationMetadata = new FederationMetadata(serviceProvider)
        FederationMetadata.EntityExtensionMetadata entityExtensionMetadata = FederationMetadata.EntityExtensionMetadata.builder()
                .typeName("typeName")
                .keyDirectives(new ArrayList<KeyDirectiveMetadata>())
                .requiredFieldsByFieldName(new HashMap<String, Set<Field>>())
                .federationMetadata(federationMetadata)
                .baseEntityMetadata(FederationMetadata.EntityMetadata.builder()
                        .typeName("typeName")
                        .keyDirectives(new ArrayList<KeyDirectiveMetadata>())
                        .fields(new HashSet<String>())
                        .federationMetadata(federationMetadata)
                        .build())
                .build();
        ServiceProvider serviceProviderObserved = entityExtensionMetadata.getBaseServiceProvider()
        expect:
        serviceProviderObserved == serviceProvider
    }
}
