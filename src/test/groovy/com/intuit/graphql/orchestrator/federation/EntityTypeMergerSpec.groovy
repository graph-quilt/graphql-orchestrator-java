package com.intuit.graphql.orchestrator.federation

import com.google.common.collect.Sets
import com.intuit.graphql.graphQL.*
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger.EntityMergingContext
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph
import spock.lang.Specification

import java.util.stream.Collectors

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.*
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createTypeSystemDefinition
import static java.util.Collections.emptyList
import static java.util.Collections.singletonList
import static org.hamcrest.Matchers.containsInAnyOrder

class EntityTypeMergerSpec extends Specification {

    private static final FieldDefinition TEST_FIELD_DEFINITION_1 = buildFieldDefinition("testField1")
    private static final FieldDefinition TEST_FIELD_DEFINITION_2 = buildFieldDefinition("testField2")

    private EntityMergingContext entityMergingContextMock
    private UnifiedXtextGraph unifiedXtextGraphMock

    private EntityTypeMerger subjectUnderTest = new EntityTypeMerger()

    List<FieldResolverContext> fieldResolverContexts = new ArrayList();

    void setup() {
        entityMergingContextMock = Mock(EntityMergingContext.class)
        unifiedXtextGraphMock = Mock(UnifiedXtextGraph.class)
    }

    void mergeIntoBaseType_objectTypeDefinition_success() {
        given:
        unifiedXtextGraphMock.getFieldResolverContexts() >> Collections.emptyList()
        unifiedXtextGraphMock.getFederationMetadataByNamespace() >> Collections.emptyMap()
        TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition()

        ObjectTypeDefinition baseObjectType =
                buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_1))

        ObjectTypeDefinition objectTypeExtension =
                buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_2))

        typeSystemDefinition.setType(objectTypeExtension)

        entityMergingContextMock.getBaseType() >> baseObjectType
        entityMergingContextMock.getExtensionSystemDefinition() >> typeSystemDefinition

        when:
        TypeDefinition actual = subjectUnderTest.mergeIntoBaseType(entityMergingContextMock, unifiedXtextGraphMock)

        then:
        actual == baseObjectType
        List<FieldDefinition> actualFieldDefinitions = getFieldDefinitions(actual)
        actualFieldDefinitions.size() == 2
    }

    void mergeIntoBaseType_objectTypeExtensionDefinition_success() {
        given:
        unifiedXtextGraphMock.getFieldResolverContexts() >> Collections.emptyList()
        unifiedXtextGraphMock.getFederationMetadataByNamespace() >> Collections.emptyMap()
        TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition()

        ObjectTypeDefinition baseObjectType =
                buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_1))

        ObjectTypeExtensionDefinition objectTypeExtension =
                buildObjectTypeExtensionDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_2))

        typeSystemDefinition.setTypeExtension(objectTypeExtension)

        entityMergingContextMock.getBaseType() >> baseObjectType
        entityMergingContextMock.getExtensionSystemDefinition() >> typeSystemDefinition

        when:
        TypeDefinition actual = subjectUnderTest.mergeIntoBaseType(entityMergingContextMock, unifiedXtextGraphMock)

        then:
        actual == baseObjectType
        List<FieldDefinition> actualFieldDefinitions = getFieldDefinitions(actual)
        actualFieldDefinitions.size() == 2
    }

    void mergeIntoBaseType_prunesFieldResolverInfo_success(){
        given:

        TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition();

        FieldDefinition resolverRequiredField = buildFieldDefinition("requiredField");

        FieldDefinition entityRequiredField = buildFieldDefinition("requiredField");
        entityRequiredField.getDirectives().add(buildDirective(buildDirectiveDefinition("external"), emptyList()));

        FieldDefinition conflictingFieldResolver1 = buildFieldDefinition("testField2");

        FieldDefinition fieldResolver2  = buildFieldDefinition("ExternalFieldResolver");


        ObjectTypeDefinition baseObjectType =
                buildObjectTypeDefinition("EntityType", Arrays.asList(
                        TEST_FIELD_DEFINITION_1,
                        resolverRequiredField,
                        conflictingFieldResolver1,
                        fieldResolver2
                ));

        ObjectTypeExtensionDefinition objectTypeExtension =
                buildObjectTypeExtensionDefinition("EntityType", Arrays.asList(
                        entityRequiredField,
                        TEST_FIELD_DEFINITION_2
                ));

        typeSystemDefinition.setTypeExtension(objectTypeExtension);

        entityMergingContextMock.getBaseType() >> baseObjectType
        entityMergingContextMock.getExtensionSystemDefinition() >> typeSystemDefinition

        fieldResolverContexts.add(
                FieldResolverContext.builder()
                        .parentTypeDefinition(baseObjectType)
                        .fieldDefinition(conflictingFieldResolver1)
                        .build()
        );
        fieldResolverContexts.add(
                FieldResolverContext.builder()
                        .parentTypeDefinition(baseObjectType)
                        .fieldDefinition(fieldResolver2)
                        .build()
        );

        unifiedXtextGraphMock.getFieldResolverContexts() >> fieldResolverContexts

        Map<String, FederationMetadata> federationMetadataMap = new HashMap<>();

        FederationMetadata baseFederationMetadataMock = Mock(FederationMetadata.class)
        FederationMetadata extFederationMetadataMock = Mock(FederationMetadata.class)

        FederationMetadata.EntityMetadata baseEntityMetaDataMock = Mock(FederationMetadata.EntityMetadata.class);
        Set<String> baseFields = Sets.newHashSet("requiredField", "testField2", "ExternalFieldResolver");
        baseEntityMetaDataMock.getFields() >> baseFields

        FederationMetadata.EntityMetadata extEntityMetaDataMock = Mock(FederationMetadata.EntityMetadata.class);
        Set<String> extFields = Sets.newHashSet( "testField2");
        extEntityMetaDataMock.getFields() >> extFields

        baseFederationMetadataMock.getEntityMetadataByName("EntityType") >> baseEntityMetaDataMock
        extFederationMetadataMock.getEntityMetadataByName("EntityType") >> extEntityMetaDataMock

        federationMetadataMap.put("baseService", baseFederationMetadataMock);
        federationMetadataMap.put("extService", extFederationMetadataMock);

        unifiedXtextGraphMock.getFederationMetadataByNamespace() >> federationMetadataMap
        entityMergingContextMock.getTypename() >> "EntityType"

        when:
        TypeDefinition actual = subjectUnderTest.mergeIntoBaseType(entityMergingContextMock, unifiedXtextGraphMock);

        then:
        actual == baseObjectType;
        List<FieldDefinition> actualFieldDefinitions = getFieldDefinitions(actual);
        actualFieldDefinitions.size() == 4

        fieldResolverContexts.size() == 1
        fieldResolverContexts.get(0).getFieldName() == "ExternalFieldResolver"

        baseFields.size() == 2
        baseFields containsInAnyOrder("requiredField", "ExternalFieldResolver");

        List<String> actualFieldNames = actualFieldDefinitions.stream()
                .map({ fieldDefinition -> fieldDefinition.getName() })
                .collect(Collectors.toList())
        actualFieldNames containsInAnyOrder("requiredField", "testField2", "ExternalFieldResolver", "testField1");
    }

    void mergeIntoBaseType_interfaceTypeDefinition_success() {
        // TODO
    }

    void mergeIntoBaseType_notTheSameTypeDefinition_success() {
        // TODO
    }
}
