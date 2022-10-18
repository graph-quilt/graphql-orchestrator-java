package com.intuit.graphql.orchestrator.schema

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata
import com.intuit.graphql.orchestrator.metadata.RenamedMetadata
import spock.lang.Specification

class ServiceMetadataImplSpec extends Specification {
    private ServiceProvider serviceProviderMock
    private RenamedMetadata renamedMetadataMock
    private FederationMetadata federationMetadataMock
    ServiceMetadataImpl serviceMetadataImpl

    def setup() {
        serviceProviderMock = Mock(ServiceProvider.class)
        renamedMetadataMock = Mock(RenamedMetadata.class)
        serviceMetadataImpl = ServiceMetadataImpl.newBuilder()
                .serviceProvider(serviceProviderMock)
                .typeMetadataMap(new HashMap<String, TypeMetadata>())
                .renamedMetadata(renamedMetadataMock)
                .federationMetadata(federationMetadataMock)
                .hasFieldResolverDefinition(false)
                .hasInterfaceOrUnion(false)
                .build()
    }

    def "newBuilder with ServiceMetadataImpl arg sets the values correctly"() {
        given:
        ServiceMetadataImpl testServiceMetadataImpl = ServiceMetadataImpl.newBuilder(serviceMetadataImpl).build()
        expect:
        !testServiceMetadataImpl.hasFieldResolverDirective()
        !testServiceMetadataImpl.isHasInterfaceOrUnion()

    }
}
