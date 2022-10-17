package com.intuit.graphql.orchestrator.xtext

import com.intuit.graphql.graphQL.FieldDefinition
import com.intuit.graphql.graphQL.ObjectTypeDefinition
import com.intuit.graphql.graphQL.TypeDefinition
import com.intuit.graphql.graphQL.TypeSystemDefinition
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata
import com.intuit.graphql.orchestrator.metadata.RenamedMetadata
import com.intuit.graphql.orchestrator.schema.Operation
import org.eclipse.xtext.resource.XtextResourceSet
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition

class UnifiedXTextGraphSpec extends Specification {

    private UnifiedXtextGraph unifiedXtextGraph
    private ObjectTypeDefinition objectTypeDefinitionMock
    private XtextResourceSet xtextResourceSetMock
    private FieldContext fieldContextMock
    private DataFetcherContext dataFetcherContextMock

    private final String TYPE_NAME_1 = "TypeName1"
    private final String TYPE_NAME_2 = "TypeName2"

    def setup() {
        FieldDefinition fieldDefinition = buildFieldDefinition("someOTHERFieldName")
        ObjectTypeDefinition queryTypeDefinition = buildObjectTypeDefinition(TYPE_NAME_2, Collections.singletonList(fieldDefinition))

        HashMap<Operation, ObjectTypeDefinition> operationMap = new HashMap<>()
        operationMap.put(Operation.QUERY, queryTypeDefinition)

        xtextResourceSetMock =  Mock(XtextResourceSet.class)
        objectTypeDefinitionMock = Mock(ObjectTypeDefinition.class)
        fieldContextMock = Mock(FieldContext.class)
        dataFetcherContextMock = Mock(DataFetcherContext.class)

        unifiedXtextGraph = UnifiedXtextGraph.newBuilder()
                .federationMetadataByNamespace(new HashMap<String, FederationMetadata>())
                .renamedMetadataByNamespace(new HashMap<String, RenamedMetadata>())
                .entityExtensionMetadatas(new ArrayList<FederationMetadata.EntityExtensionMetadata>())
                .entityExtensionsByNamespace(new HashMap<String, Map<String, TypeSystemDefinition>>())
                .entitiesByTypeName(new HashMap<String, TypeDefinition>())
                .valueTypesByName(new HashMap<String, TypeDefinition>())
                .types(new HashMap<String, TypeDefinition>())
                .hasInterfaceOrUnion(true)
                .renamedMetadataByNamespace(new HashMap<String, RenamedMetadata>())
                .hasFieldResolverDefinition(true)
                .query(queryTypeDefinition)
                .mutation(objectTypeDefinitionMock)
                .dataFetcherContext(fieldContextMock, dataFetcherContextMock)
                .build()
    }

    def "test unifiedXTextGraph is properly formed"() {
        expect:
        unifiedXtextGraph != null
        unifiedXtextGraph.getFederationMetadataByNamespace() == new HashMap<>()
        unifiedXtextGraph.getRenamedMetadataByNamespace() == new HashMap<>()
        unifiedXtextGraph.operationMap.size() == 2
    }

    def "requiresTypenameInjection returns true"() {
        expect:
        unifiedXtextGraph.requiresTypenameInjection()
    }

    def "addType adds the typeDefinition to the types Map correctly"() {
        given:
        FieldDefinition fieldDefinition = buildFieldDefinition("someOTHERFieldName")
        ObjectTypeDefinition typeDefinition = buildObjectTypeDefinition(TYPE_NAME_1, Collections.singletonList(fieldDefinition))

        when:
        unifiedXtextGraph.addType(typeDefinition)

        then:
        unifiedXtextGraph.getType(TYPE_NAME_1) == typeDefinition
    }

    def "removeType removes the typeDefinition from the types Map correctly"() {
        given:
        FieldDefinition fieldDefinition = buildFieldDefinition("someOTHERFieldName")
        ObjectTypeDefinition typeDefinition = buildObjectTypeDefinition(TYPE_NAME_2, Collections.singletonList(fieldDefinition))

        when:
        unifiedXtextGraph.addType(typeDefinition)
        unifiedXtextGraph.removeType(typeDefinition)

        then:
        unifiedXtextGraph.getType(TYPE_NAME_2) == null
    }

    def "addToEntityExtensionMetadatas adds to the entityExtensionMetadatas list"() {
        given:
        FederationMetadata.EntityExtensionMetadata entityExtensionMetadata = Mock(FederationMetadata.EntityExtensionMetadata.class)

        when:
        unifiedXtextGraph.addToEntityExtensionMetadatas(entityExtensionMetadata)

        then:
        unifiedXtextGraph.entityExtensionMetadatas.size() == 1
    }

}
