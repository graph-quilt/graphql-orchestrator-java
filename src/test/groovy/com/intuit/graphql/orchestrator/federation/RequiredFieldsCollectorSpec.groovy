package com.intuit.graphql.orchestrator.federation

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityMetadata
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata
import com.intuit.graphql.orchestrator.schema.ServiceMetadata
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import graphql.language.Field
import graphql.language.SelectionSet
import graphql.schema.FieldCoordinates
import spock.lang.Specification

import static graphql.schema.FieldCoordinates.coordinates

class RequiredFieldsCollectorSpec extends Specification {

    private static final String TEST_ENTITY_TYPE_NAME = "TestEntityType"

    private static final Field FIELD_PRIMITIVE = Field.newField("stringField").build()

    private static final Field FIELD_OBJECT =
            Field.newField("objectField")
                    .selectionSet(
                            SelectionSet.newSelectionSet()
                                    .selection(Field.newField("subField1").build())
                                    .selection(Field.newField("subField2").build())
                                    .build())
                    .build()

    private static final Field REQD_FIELD_1 = Field.newField("reqdField1").build()
    private static final Field REQD_FIELD_2 = Field.newField("reqdField2").build()

    private static final Field KEY_FIELD_1 = Field.newField("keyField1").build()
    private static final Field KEY_FIELD_2 = Field.newField("keyField2").build()

    FieldCoordinates FIELD_COORDINATE_STRFIELD = coordinates(TEST_ENTITY_TYPE_NAME, "stringField")
    FieldCoordinates FIELD_COORDINATE_OBJFIELD = coordinates(TEST_ENTITY_TYPE_NAME, "objectField")

    private EntityMetadata entityMetadataMock

    private ServiceMetadata serviceMetadataMock

    private FederationMetadata federationMetadataMock

    private KeyDirectiveMetadata keyDirectiveMetadataMock

    private RequiredFieldsCollector subjectUnderTest

    def setup() {
        entityMetadataMock = Mock(EntityMetadata)
        serviceMetadataMock = Mock(ServiceMetadata)
        federationMetadataMock = Mock(FederationMetadata)
        keyDirectiveMetadataMock = Mock(KeyDirectiveMetadata)
    }

    private RequiredFieldsCollector buildRequiredFieldsCollector(
            Map<String, Field> excludeFields,
            List<FieldResolverContext> fieldResolverContexts,
            Set<Field> requiredDirectives) {
        return RequiredFieldsCollector.builder()
                .excludeFields(excludeFields)
                .parentTypeName(TEST_ENTITY_TYPE_NAME)
                .serviceMetadata(serviceMetadataMock)
                .fieldResolverContexts(fieldResolverContexts)
                //.fieldsWithRequiresDirective(ImmutableSet.of(FIELD_PRIMITIVE, FIELD_OBJECT))
                .fieldsWithRequiresDirective(requiredDirectives)
                .build()
    }

    def "get returns Required Fields For Requires Directive"() {
        given:
        serviceMetadataMock.isEntity(TEST_ENTITY_TYPE_NAME) >> true
        serviceMetadataMock.getFederationServiceMetadata() >> federationMetadataMock
        federationMetadataMock.getEntityMetadataByName(TEST_ENTITY_TYPE_NAME) >> entityMetadataMock

        federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_STRFIELD) >> true
        federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_OBJFIELD) >> true

        federationMetadataMock.getRequireFields(FIELD_COORDINATE_STRFIELD) >> ImmutableSet.of(REQD_FIELD_1)
        federationMetadataMock.getRequireFields(FIELD_COORDINATE_OBJFIELD) >> ImmutableSet.of(REQD_FIELD_2)

        keyDirectiveMetadataMock.getFieldSet() >> ImmutableSet.of(KEY_FIELD_1, KEY_FIELD_2)
        entityMetadataMock.getKeyDirectives() >> Collections.emptyList()

        subjectUnderTest = buildRequiredFieldsCollector(Collections.emptyMap(), Collections.emptyList(), ImmutableSet.of(FIELD_PRIMITIVE, FIELD_OBJECT))

        when:
        Set<Field> actual = subjectUnderTest.get()

        then:
        actual.size() == 2
        actual == ImmutableSet.of(REQD_FIELD_1, REQD_FIELD_2)
    }

    def "get returns Required Fields For Key Directive"() {
        given:
        serviceMetadataMock.isEntity(TEST_ENTITY_TYPE_NAME) >> true
        serviceMetadataMock.getFederationServiceMetadata() >> federationMetadataMock
        federationMetadataMock.getEntityMetadataByName(TEST_ENTITY_TYPE_NAME) >> entityMetadataMock

        federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_STRFIELD) >> false
        federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_OBJFIELD) >> false

        federationMetadataMock.getRequireFields(FIELD_COORDINATE_STRFIELD) >> ImmutableSet.of(REQD_FIELD_1)
        federationMetadataMock.getRequireFields(FIELD_COORDINATE_OBJFIELD) >> ImmutableSet.of(REQD_FIELD_2)

        entityMetadataMock.getKeyDirectives() >> Collections.singletonList(keyDirectiveMetadataMock)
        keyDirectiveMetadataMock.getFieldSet() >> ImmutableSet.of(KEY_FIELD_1, KEY_FIELD_2)

        subjectUnderTest = buildRequiredFieldsCollector(Collections.emptyMap(), Collections.emptyList(), ImmutableSet.of(FIELD_PRIMITIVE, FIELD_OBJECT))

        when:
        Set<Field> actual = subjectUnderTest.get()

        then:
        actual.size() == 2
        actual == ImmutableSet.of(KEY_FIELD_1, KEY_FIELD_2)
    }

    def "get returns Required Fields For Key Directives And Requires Directive"() {
        given:
        serviceMetadataMock.isEntity(TEST_ENTITY_TYPE_NAME) >> true
        serviceMetadataMock.getFederationServiceMetadata() >> federationMetadataMock
        federationMetadataMock.getEntityMetadataByName(TEST_ENTITY_TYPE_NAME) >> entityMetadataMock

        federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_STRFIELD) >> true
        federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_OBJFIELD) >> true

        federationMetadataMock.getRequireFields(FIELD_COORDINATE_STRFIELD) >> ImmutableSet.of(REQD_FIELD_1)
        federationMetadataMock.getRequireFields(FIELD_COORDINATE_OBJFIELD) >> ImmutableSet.of(REQD_FIELD_2)

        entityMetadataMock.getKeyDirectives() >> Collections.singletonList(keyDirectiveMetadataMock)
        keyDirectiveMetadataMock.getFieldSet() >> ImmutableSet.of(KEY_FIELD_1, KEY_FIELD_2)

        subjectUnderTest = buildRequiredFieldsCollector(Collections.emptyMap(), Collections.emptyList(), ImmutableSet.of(FIELD_PRIMITIVE, FIELD_OBJECT))

        Set<Field> actual = subjectUnderTest.get()

        expect:
        actual.size() == 4
        actual == ImmutableSet.of(KEY_FIELD_1, KEY_FIELD_2, REQD_FIELD_1, REQD_FIELD_2)
    }

    def "get returns Required Fields For Key Directives And Requires Directive Without Exclude Fields"() {
        given:
        serviceMetadataMock.isEntity(TEST_ENTITY_TYPE_NAME) >> true
        serviceMetadataMock.getFederationServiceMetadata() >> federationMetadataMock
        federationMetadataMock.getEntityMetadataByName(TEST_ENTITY_TYPE_NAME) >> entityMetadataMock

        federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_STRFIELD) >> true
        federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_OBJFIELD) >> true

        federationMetadataMock.getRequireFields(FIELD_COORDINATE_STRFIELD) >> ImmutableSet.of(REQD_FIELD_1)
        federationMetadataMock.getRequireFields(FIELD_COORDINATE_OBJFIELD) >> ImmutableSet.of(REQD_FIELD_2)

        entityMetadataMock.getKeyDirectives() >> Collections.singletonList(keyDirectiveMetadataMock)
        keyDirectiveMetadataMock.getFieldSet() >> ImmutableSet.of(KEY_FIELD_1, KEY_FIELD_2)

        Map<String, Field> excludeFields = ImmutableMap.of(KEY_FIELD_2.getName(),KEY_FIELD_2, REQD_FIELD_1.getName(),REQD_FIELD_1)

        subjectUnderTest = buildRequiredFieldsCollector(excludeFields, Collections.emptyList(), ImmutableSet.of(FIELD_PRIMITIVE, FIELD_OBJECT))

        when:
        Set<Field> actual = subjectUnderTest.get()

        then:
        actual.size() == 2
        actual == ImmutableSet.of(KEY_FIELD_1, REQD_FIELD_2)
    }

    def "get returns Required Fields For Field Resolver"() {
        given:
        serviceMetadataMock.isEntity(TEST_ENTITY_TYPE_NAME) >> true
        serviceMetadataMock.getFederationServiceMetadata() >> federationMetadataMock
        federationMetadataMock.getEntityMetadataByName(TEST_ENTITY_TYPE_NAME) >> entityMetadataMock

        federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_STRFIELD) >> true
        federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_OBJFIELD) >> true

        federationMetadataMock.getRequireFields(FIELD_COORDINATE_STRFIELD) >> ImmutableSet.of(REQD_FIELD_1)
        federationMetadataMock.getRequireFields(FIELD_COORDINATE_OBJFIELD) >> ImmutableSet.of(REQD_FIELD_2)

        entityMetadataMock.getKeyDirectives() >> Collections.emptyList()
        keyDirectiveMetadataMock.getFieldSet() >> ImmutableSet.of(KEY_FIELD_1, KEY_FIELD_2)

        FieldResolverContext fieldResolverContextMock = Mock(FieldResolverContext)
        fieldResolverContextMock.getRequiredFields() >> ImmutableSet.of("reqdField")

        subjectUnderTest = buildRequiredFieldsCollector(
                Collections.emptyMap(), ImmutableList.of(fieldResolverContextMock), Collections.emptySet())

        when:
        Set<Field> actual = subjectUnderTest.get()

        then:
        actual.size() == 1
        actual.stream().findFirst().get().getName() == "reqdField"
    }

    def "get returns Required Fields For Field Resolver With Excluded Fields"() {
        given:
        serviceMetadataMock.isEntity(TEST_ENTITY_TYPE_NAME) >> true
        serviceMetadataMock.getFederationServiceMetadata() >> federationMetadataMock
        federationMetadataMock.getEntityMetadataByName(TEST_ENTITY_TYPE_NAME) >> entityMetadataMock

        federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_STRFIELD) >> true
        federationMetadataMock.hasRequiresFieldSet(FIELD_COORDINATE_OBJFIELD) >> true

        federationMetadataMock.getRequireFields(FIELD_COORDINATE_STRFIELD) >> ImmutableSet.of(REQD_FIELD_1)
        federationMetadataMock.getRequireFields(FIELD_COORDINATE_OBJFIELD) >> ImmutableSet.of(REQD_FIELD_2)

        entityMetadataMock.getKeyDirectives() >> Collections.emptyList()
        keyDirectiveMetadataMock.getFieldSet() >> ImmutableSet.of(KEY_FIELD_1, KEY_FIELD_2)

        Map<String, Field> excludeFields = ImmutableMap.of(REQD_FIELD_1.getName(),REQD_FIELD_1)

        FieldResolverContext fieldResolverContextMock = Mock(FieldResolverContext.class)
        fieldResolverContextMock.getRequiredFields() >> ImmutableSet.of("reqdField1")

        subjectUnderTest = buildRequiredFieldsCollector(
                excludeFields, ImmutableList.of(fieldResolverContextMock), Collections.emptySet())

        when:
        Set<Field> actual = subjectUnderTest.get()

        then:
        actual.size() == 0
    }

}
