package com.intuit.graphql.orchestrator.federation

import com.intuit.graphql.graphQL.*
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger.EntityMergingContext
import helpers.BaseIntegrationTestSpecification

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.*
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createTypeSystemDefinition
import static java.util.Collections.singletonList

class EntityTypeMergerSpec extends BaseIntegrationTestSpecification {

    private static final FieldDefinition TEST_FIELD_DEFINITION_1 = buildFieldDefinition("testField1")
    private static final FieldDefinition TEST_FIELD_DEFINITION_2 = buildFieldDefinition("testField2")

    private EntityMergingContext entityMergingContextMock

    private EntityTypeMerger subjectUnderTest = new EntityTypeMerger()

    void setup() {
        entityMergingContextMock = Mock(EntityMergingContext.class)
    }

    void mergeIntoBaseType_objectTypeDefinition_success() {
        given:
        TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition()

        ObjectTypeDefinition baseObjectType =
                buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_1))

        ObjectTypeDefinition objectTypeExtension =
                buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_2))

        typeSystemDefinition.setType(objectTypeExtension)

        entityMergingContextMock.getBaseType() >> baseObjectType
        entityMergingContextMock.getExtensionSystemDefinition() >> typeSystemDefinition

        when:
        TypeDefinition actual = subjectUnderTest.mergeIntoBaseType(entityMergingContextMock)

        then:
        actual == baseObjectType
        List<FieldDefinition> actualFieldDefinitions = getFieldDefinitions(actual)
        actualFieldDefinitions.size() == 2
    }

    void mergeIntoBaseType_objectTypeExtensionDefinition_success() {
        given:
        TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition()

        ObjectTypeDefinition baseObjectType =
                buildObjectTypeDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_1))

        ObjectTypeExtensionDefinition objectTypeExtension =
                buildObjectTypeExtensionDefinition("EntityType", singletonList(TEST_FIELD_DEFINITION_2))

        typeSystemDefinition.setTypeExtension(objectTypeExtension)

        entityMergingContextMock.getBaseType() >> baseObjectType
        entityMergingContextMock.getExtensionSystemDefinition() >> typeSystemDefinition

        when:
        TypeDefinition actual = subjectUnderTest.mergeIntoBaseType(entityMergingContextMock)

        then:
        actual == baseObjectType
        List<FieldDefinition> actualFieldDefinitions = getFieldDefinitions(actual)
        actualFieldDefinitions.size() == 2
    }

    void mergeIntoBaseType_interfaceTypeDefinition_success() {
        // TODO
    }

    void mergeIntoBaseType_notTheSameTypeDefinition_success() {
        // TODO
    }
}
