package com.intuit.graphql.orchestrator.xtext

import com.intuit.graphql.graphQL.FieldDefinition
import com.intuit.graphql.graphQL.ObjectType
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

class XTextGraphSpec extends Specification {

    private XtextGraph xtextGraph
    private ObjectTypeDefinition objectTypeDefinitionMock
    private XtextResourceSet xtextResourceSetMock
    private FieldContext fieldContextMock
    private DataFetcherContext dataFetcherContextMock

    private final String TYPE_NAME_1 = "TypeName1"
    private final String TYPE_NAME_2 = "TypeName2"

    def setup() {
        FieldDefinition fieldDefinition = buildFieldDefinition("someOTHERFieldName")
        ObjectTypeDefinition typeDefinition = buildObjectTypeDefinition(TYPE_NAME_2, Collections.singletonList(fieldDefinition))

        HashMap<Operation, ObjectTypeDefinition> operationMap = new HashMap<>()
        operationMap.put(Operation.QUERY, typeDefinition)

        xtextResourceSetMock =  Mock(XtextResourceSet.class)
        objectTypeDefinitionMock = Mock(ObjectTypeDefinition.class)
        fieldContextMock = Mock(FieldContext.class)
        dataFetcherContextMock = Mock(DataFetcherContext.class)

        xtextGraph = XtextGraph.newBuilder()
        .xtextResourceSet(xtextResourceSetMock)
        .federationMetadataByNamespace(new HashMap<String, FederationMetadata>())
        .renamedMetadataByNamespace(new HashMap<String, RenamedMetadata>())
        .entityExtensionMetadatas(new ArrayList<FederationMetadata.EntityExtensionMetadata>())
        .entityExtensionsByNamespace(new HashMap<String, Map<String, TypeSystemDefinition>>())
        .entitiesByTypeName(new HashMap<String, TypeDefinition>())
        .valueTypesByName(new HashMap<String, TypeDefinition>())
        .types(new HashMap<String, TypeDefinition>())
        .operationMap(operationMap)
        .mutation(objectTypeDefinitionMock)
        .dataFetcherContext(fieldContextMock, dataFetcherContextMock)
        .build()

        assert xtextGraph.getOperation(TYPE_NAME_2) == Operation.QUERY
    }

    def "test xTextGraph is properly formed"() {
        expect:
        xtextGraph != null
        xtextGraph.getFederationMetadataByNamespace() == new HashMap<>()
        xtextGraph.getRenamedMetadataByNamespace() == new HashMap<>()
    }

    def "addType adds the typeDefinition to the types Map correctly"() {
        given:
        FieldDefinition fieldDefinition = buildFieldDefinition("someOTHERFieldName")
        ObjectTypeDefinition typeDefinition = buildObjectTypeDefinition(TYPE_NAME_1, Collections.singletonList(fieldDefinition))

        when:
        xtextGraph.addType(typeDefinition)

        then:
        xtextGraph.getType(TYPE_NAME_1) == typeDefinition
    }

    def "removeType removes the typeDefinition from the types Map correctly"() {
        given:
        FieldDefinition fieldDefinition = buildFieldDefinition("someOTHERFieldName")
        ObjectTypeDefinition typeDefinition = buildObjectTypeDefinition(TYPE_NAME_2, Collections.singletonList(fieldDefinition))

        when:
        xtextGraph.addType(typeDefinition)
        xtextGraph.removeType(typeDefinition)

        then:
        xtextGraph.getType(TYPE_NAME_2) == null
    }

    def "getType by NamedType returns the typeDefinition correctly"() {
        given:
        FieldDefinition fieldDefinition = buildFieldDefinition("someOTHERFieldName")
        ObjectTypeDefinition typeDefinition = buildObjectTypeDefinition(TYPE_NAME_2, Collections.singletonList(fieldDefinition))
        ObjectType objectType = GraphQLFactoryDelegate.createObjectType()
        objectType.setType(typeDefinition)
        xtextGraph.addType(typeDefinition)

        expect:
        xtextGraph.getType(objectType) == typeDefinition
    }

}
