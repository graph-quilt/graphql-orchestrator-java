package com.intuit.graphql.orchestrator.schema.transform

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.graphQL.*
import com.intuit.graphql.graphQL.impl.ArgumentImpl
import com.intuit.graphql.orchestrator.federation.exceptions.ExternalFieldNotFoundInBaseException
import com.intuit.graphql.orchestrator.federation.exceptions.SharedOwnershipException
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph
import spock.lang.Specification
import spock.lang.Subject

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.*
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_EXTERNAL_DIRECTIVE
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createTypeSystemDefinition
import static java.util.Collections.singletonList

class FederationTransformerPostMergeSpec extends Specification {

    private final FieldDefinition BASE_FIELD_DEFINITION = buildFieldDefinition("testField1")

    private final Directive EXTERNAL_DIRECTIVE = buildDirective(buildDirectiveDefinition(FEDERATION_EXTERNAL_DIRECTIVE), null)

    private final FieldDefinition EXTENSION_FIELD_DEFINITION = buildFieldDefinition("testField1", singletonList(EXTERNAL_DIRECTIVE))

    @Subject
    private FederationTransformerPostMerge subjectUnderTest

    def setup() {
        subjectUnderTest = new FederationTransformerPostMerge()
    }

    Map<String, TypeDefinition> getEntities() {
        Map<String, TypeDefinition> entitiesByTypeName = new HashMap<>()

        ObjectTypeDefinition baseObjectType =
                buildObjectTypeDefinition("EntityType", singletonList(BASE_FIELD_DEFINITION))
        entitiesByTypeName.put("EntityType", baseObjectType)

        return entitiesByTypeName
    }

    Map<String, Map<String, TypeSystemDefinition>> getEntityExtensions()  {
        ObjectTypeDefinition objectTypeExtension =
                buildObjectTypeDefinition("EntityType", singletonList(EXTENSION_FIELD_DEFINITION))

        TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition()
        typeSystemDefinition.setType(objectTypeExtension)

        Map<String, Map<String, TypeSystemDefinition>> entityExtensionsByNamespace = new HashMap<>()

        entityExtensionsByNamespace.put("testNamespace", ImmutableMap.of("EntityType", typeSystemDefinition))

        return entityExtensionsByNamespace
    }

    def "transform success"() {
        given:
        def unifiedXtextGraphMock = Mock(UnifiedXtextGraph)

        1 * unifiedXtextGraphMock.getEntitiesByTypeName() >> getEntities()
        2 * unifiedXtextGraphMock.getEntityExtensionsByNamespace() >> getEntityExtensions()
        unifiedXtextGraphMock.getValueTypesByName() >> new HashMap<>()
        unifiedXtextGraphMock.getEntityExtensionMetadatas() >> []

        when:
        UnifiedXtextGraph actual = subjectUnderTest.transform(unifiedXtextGraphMock)

        then:
        actual.is(unifiedXtextGraphMock)
    }

    def "transform success extension key in subset"() {
        given:
        def unifiedXtextGraphMock = Mock(UnifiedXtextGraph)

        HashMap<String, Map<String, TypeSystemDefinition>> extensionsByNamespace = new HashMap<>()
        HashMap<String, TypeSystemDefinition> extDefinitionsByName = new HashMap<>()
        HashMap<String, TypeDefinition> baseDefinitionsByName = new HashMap<>()
        TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition()

        Directive sharedKeyDirective1 = createMockKeyDirectory("testField1")
        Directive sharedKeyDirective2 = createMockKeyDirectory("testField1")
        Directive uniqueKeyDirective = createMockKeyDirectory("testField2")

        ObjectTypeDefinition baseObjectType =
                buildObjectTypeDefinition("EntityType", singletonList(BASE_FIELD_DEFINITION))
        baseObjectType.getDirectives().addAll(Arrays.asList(sharedKeyDirective1,uniqueKeyDirective))

        ObjectTypeDefinition objectTypeExtension =
                buildObjectTypeDefinition("EntityType", singletonList(EXTENSION_FIELD_DEFINITION))
        objectTypeExtension.getDirectives().add(sharedKeyDirective2)

        typeSystemDefinition.setType(objectTypeExtension)
        baseDefinitionsByName.put("EntityType", baseObjectType)
        extDefinitionsByName.put("EntityType", typeSystemDefinition)
        extensionsByNamespace.put("testNamespace", extDefinitionsByName)

        1 * unifiedXtextGraphMock.getEntitiesByTypeName() >> baseDefinitionsByName
        2 * unifiedXtextGraphMock.getEntityExtensionsByNamespace() >> extensionsByNamespace
        unifiedXtextGraphMock.getValueTypesByName() >> new HashMap<>()
        unifiedXtextGraphMock.getEntityExtensionMetadatas() >> []

        when:
        UnifiedXtextGraph actual = subjectUnderTest.transform(unifiedXtextGraphMock)

        then:
        actual.is(unifiedXtextGraphMock)
    }

    def "transform success extension ObjectTypeExtension key in subset"() {
        given:
        def unifiedXtextGraphMock = Mock(UnifiedXtextGraph)

        HashMap<String, Map<String, TypeSystemDefinition>> extensionsByNamespace = new HashMap<>()

        HashMap<String, TypeSystemDefinition> extDefinitionsByName = new HashMap<>()
        HashMap<String, TypeDefinition> baseDefinitionsByName = new HashMap<>()
        TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition()

        Directive sharedKeyDirective1 = createMockKeyDirectory("testField1")
        Directive sharedKeyDirective2 = createMockKeyDirectory("testField1")
        Directive uniqueKeyDirective = createMockKeyDirectory("testField2")

        ObjectTypeDefinition baseObjectType =
                buildObjectTypeDefinition("EntityType", singletonList(BASE_FIELD_DEFINITION))
        baseObjectType.getDirectives().addAll(Arrays.asList(sharedKeyDirective1,uniqueKeyDirective))

        ObjectTypeExtensionDefinition objectTypeExtension =
                buildObjectTypeExtensionDefinition("EntityType", singletonList(EXTENSION_FIELD_DEFINITION))
        objectTypeExtension.getDirectives().add(sharedKeyDirective2)

        typeSystemDefinition.setTypeExtension(objectTypeExtension)
        baseDefinitionsByName.put("EntityType", baseObjectType)
        extDefinitionsByName.put("EntityType", typeSystemDefinition)
        extensionsByNamespace.put("testNamespace", extDefinitionsByName)

        1 * unifiedXtextGraphMock.getEntitiesByTypeName() >> baseDefinitionsByName
        2 * unifiedXtextGraphMock.getEntityExtensionsByNamespace() >> extensionsByNamespace
        unifiedXtextGraphMock.getValueTypesByName() >> new HashMap<>()
        unifiedXtextGraphMock.getEntityExtensionMetadatas() >> []

        when:
        UnifiedXtextGraph actual = subjectUnderTest.transform(unifiedXtextGraphMock)

        then:
        actual.is(unifiedXtextGraphMock)
    }

    def "transform adds inaccessible info"() {
        given:
        def unifiedXtextGraphMock = Mock(UnifiedXtextGraph)

        Directive inaccessibleDirective = buildDirective(buildDirectiveDefinition("inaccessible"), new ArrayList<Argument>())
        FieldDefinition inaccessibleField = buildFieldDefinition("inactiveField", singletonList(inaccessibleDirective))
        FieldDefinition accessibleField = buildFieldDefinition("activeField")

        List<FieldDefinition> inaccessibleFieldList = new ArrayList<>()
        inaccessibleFieldList.add(accessibleField)
        inaccessibleFieldList.add(inaccessibleField)

        List<FieldDefinition> fieldList = new ArrayList<>()
        fieldList.add(accessibleField)

        ObjectTypeDefinition objectTypeDefinition = buildObjectTypeDefinition("TestObject", fieldList)
        ObjectTypeDefinition InaccessibleObjectDefinition = buildObjectTypeDefinition("InaccessibleTestObject", inaccessibleFieldList)

        def valueTypesByName = new HashMap()
        valueTypesByName.put("TestObject", objectTypeDefinition)
        valueTypesByName.put("InaccessibleTestObject", InaccessibleObjectDefinition)

        Set<String> inaccessibleTypes = new HashSet()

        unifiedXtextGraphMock.getTypesWithInaccessibleFields() >> inaccessibleTypes
        unifiedXtextGraphMock.getEntityExtensionsByNamespace() >> new HashMap<>()
        unifiedXtextGraphMock.getEntityExtensionMetadatas() >> []
        unifiedXtextGraphMock.getValueTypesByName() >> valueTypesByName

        when:
        UnifiedXtextGraph actual = subjectUnderTest.transform(unifiedXtextGraphMock)

        then:
        actual.getTypesWithInaccessibleFields().size() == 1
        actual.getTypesWithInaccessibleFields().getAt(0) == "InaccessibleTestObject"
    }

    def "transform fails extension key not subset"() {
        given:
        def unifiedXtextGraphMock = Mock(UnifiedXtextGraph)

        HashMap<String, Map<String, TypeSystemDefinition>> extensionsByNamespace = new HashMap<>()
        HashMap<String, TypeSystemDefinition> extDefinitionsByName = new HashMap<>()
        HashMap<String, TypeDefinition> baseDefinitionsByName = new HashMap<>()
        TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition()

        Directive sharedKeyDirective1 = createMockKeyDirectory("testField1")
        Directive sharedKeyDirective2 = createMockKeyDirectory("testField1")
        Directive uniqueKeyDirective = createMockKeyDirectory("testField2")

        ObjectTypeDefinition baseObjectType =
                buildObjectTypeDefinition("EntityType", singletonList(BASE_FIELD_DEFINITION))
        baseObjectType.getDirectives().add(sharedKeyDirective1)

        ObjectTypeDefinition objectTypeExtension =
                buildObjectTypeDefinition("EntityType", singletonList(EXTENSION_FIELD_DEFINITION))
        objectTypeExtension.getDirectives().addAll(Arrays.asList(sharedKeyDirective2, uniqueKeyDirective))

        typeSystemDefinition.setType(objectTypeExtension)
        baseDefinitionsByName.put("EntityType", baseObjectType)
        extDefinitionsByName.put("EntityType", typeSystemDefinition)
        extensionsByNamespace.put("testNamespace", extDefinitionsByName)

        1 * unifiedXtextGraphMock.getEntitiesByTypeName() >> baseDefinitionsByName
        2 * unifiedXtextGraphMock.getEntityExtensionsByNamespace() >> extensionsByNamespace
        unifiedXtextGraphMock.getEntityExtensionMetadatas() >> []

        when:
        subjectUnderTest.transform(unifiedXtextGraphMock)

        then:
        thrown(TypeConflictException)
    }

    def "transform fails shared field without external"(){
        given:
        def unifiedXtextGraphMock = Mock(UnifiedXtextGraph)

        Map<String, Map<String, TypeSystemDefinition>> entityExtensionsByNamespace = new HashMap<>()
        TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition()

        ObjectTypeDefinition objectTypeExtension =
                buildObjectTypeDefinition("EntityType", singletonList(buildFieldDefinition("testField1")))
        typeSystemDefinition.setType(objectTypeExtension)
        entityExtensionsByNamespace.put("testNamespace", ImmutableMap.of("EntityType", typeSystemDefinition))

        1 * unifiedXtextGraphMock.getEntitiesByTypeName() >> getEntities()
        2* unifiedXtextGraphMock.getEntityExtensionsByNamespace() >> entityExtensionsByNamespace
        unifiedXtextGraphMock.getEntityExtensionMetadatas() >> []

        when:
        subjectUnderTest.transform(unifiedXtextGraphMock)

        then:
        thrown(SharedOwnershipException)
    }

    def "transform fails external field not in base"(){
        given:
        def unifiedXtextGraphMock = Mock(UnifiedXtextGraph)

        Map<String, Map<String, TypeSystemDefinition>> entityExtensionsByNamespace = new HashMap<>()
        TypeSystemDefinition typeSystemDefinition = createTypeSystemDefinition()

        ObjectTypeDefinition objectTypeExtension = buildObjectTypeDefinition("EntityType",
                Arrays.asList(EXTENSION_FIELD_DEFINITION, buildFieldDefinition("BadField", singletonList(buildDirective(buildDirectiveDefinition(FEDERATION_EXTERNAL_DIRECTIVE), null)))))

        typeSystemDefinition.setType(objectTypeExtension)
        entityExtensionsByNamespace.put("testNamespace", ImmutableMap.of("EntityType", typeSystemDefinition))

        1 * unifiedXtextGraphMock.getEntitiesByTypeName() >> getEntities()
        2 * unifiedXtextGraphMock.getEntityExtensionsByNamespace() >> entityExtensionsByNamespace
        unifiedXtextGraphMock.getEntityExtensionMetadatas() >> []

        when:
        subjectUnderTest.transform(unifiedXtextGraphMock)

        then:
        thrown(ExternalFieldNotFoundInBaseException)
    }

    private Directive createMockKeyDirectory(String fieldSet) {
        DirectiveDefinition keyDirectiveDefinition1 = buildDirectiveDefinition(FEDERATION_KEY_DIRECTIVE)
        ArgumentImpl fieldsArgument = Mock(ArgumentImpl.class)
        ValueWithVariable valueWithVariableMock = Mock(ValueWithVariable.class)
        List<Argument> fooKey = Arrays.asList(fieldsArgument)

        valueWithVariableMock.getStringValue() >> fieldSet
        fieldsArgument.getValueWithVariable() >> valueWithVariableMock

        return buildDirective(keyDirectiveDefinition1, fooKey)
    }
}
