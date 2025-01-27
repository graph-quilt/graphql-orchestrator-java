package com.intuit.graphql.orchestrator.utils

import com.intuit.graphql.graphQL.*
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.*

class XtextTypeUtilsSpec extends Specification {

    def "compare Wrapped Types Test"() {
        given:
        ListType listType = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(objectTypeDefinition)
        listType.setType(namedType); //[Arg]

        ListType outerlistType = GraphQLFactoryDelegate.createListType()
        outerlistType.setType(listType); //[[Arg]]

        expect:
        !compareTypes(listType, outerlistType)
    }

    def "compare Non Null Types Test"() {
        given:
        ListType listType = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(objectTypeDefinition)
        namedType.setNonNull(true)
        listType.setType(namedType); //[Arg!]

        ListType listType2 = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition2 = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition2.setName("Arg1")
        NamedType namedType2 = createNamedType(objectTypeDefinition2)
        listType2.setType(namedType2); //[Arg]

        expect:
        !compareTypes(listType, listType2)
    }

    def "compare Equal Wrapped Non Null Types Test"() {
        given:
        ListType listType = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(objectTypeDefinition)
        namedType.setNonNull(true)
        listType.setType(namedType)
        listType.setNonNull(true);//[Arg!]!

        ListType listType2 = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition2 = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition2.setName("Arg1")
        NamedType namedType2 = createNamedType(objectTypeDefinition2)
        namedType2.setNonNull(true)
        listType2.setType(namedType2)
        listType2.setNonNull(true); //[Arg!]!

        expect:
        compareTypes(listType, listType2)
    }

    def "generate Arguments Definition String Test"() {
        given:
        final ArgumentsDefinition argumentsDefinition = GraphQLFactoryDelegate.createArgumentsDefinition()
        InputValueDefinition ivf = GraphQLFactoryDelegate.createInputValueDefinition()
        ivf.setName("argument1")
        ivf.setDesc("desc1")
        argumentsDefinition.getInputValueDefinition().add(ivf)

        InputValueDefinition ivf2 = GraphQLFactoryDelegate.createInputValueDefinition()
        ivf2.setName("argument2")
        ivf2.setDesc("desc2")
        argumentsDefinition.getInputValueDefinition().add(ivf2)

        expect:
        ["argument1", "desc1", "argument2", "desc2"]
                .findAll({
                    toDescriptiveString(argumentsDefinition)
                            .contains(it)
                }).size() == 4
    }

    void "generate Directives String Test"() {
        given:
        FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition()

        final DirectiveDefinition directiveDefinition = GraphQLFactoryDelegate.createDirectiveDefinition()
        directiveDefinition.setName("directive1")
        directiveDefinition.setDesc("description1")

        Directive directive = GraphQLFactoryDelegate.createDirective()
        directive.setDefinition(directiveDefinition)

        final DirectiveDefinition directiveDefinition2 = GraphQLFactoryDelegate.createDirectiveDefinition()
        directiveDefinition2.setName("directive2")
        directiveDefinition2.setDesc("description2")

        Directive directive2 = GraphQLFactoryDelegate.createDirective()
        directive2.setDefinition(directiveDefinition2)

        fieldDefinition.getDirectives().add(directive)
        fieldDefinition.getDirectives().add(directive2)

        expect:
        ["directive1", "description1", "directive2", "description2"]
                .findAll({
                    toDescriptiveString(fieldDefinition.getDirectives())
                            .contains(it)
                }).size() == 4
    }

    def "throws Exception For Object Type Not Valid As Input"() {
        given:
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(objectTypeDefinition)

        ListType listType = GraphQLFactoryDelegate.createListType()
        listType.setType(namedType)

        expect:
        !isValidInputType(listType)
    }

    def "Input Object Type Is Valid As Input"() {
        given:
        InputObjectTypeDefinition inputObjectTypeDefinition = GraphQLFactoryDelegate.createInputObjectTypeDefinition()
        inputObjectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(inputObjectTypeDefinition)

        ListType listType = GraphQLFactoryDelegate.createListType()
        listType.setType(namedType)

        expect:
        isValidInputType(listType)
    }

    def "Enum Type Is Valid As Input"() {
        given:
        EnumTypeDefinition enumTypeDefinition = GraphQLFactoryDelegate.createEnumTypeDefinition()
        enumTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(enumTypeDefinition)

        expect:
        isValidInputType(namedType)
    }

    def "Scalar Type Is Valid As Input"() {
        given:
        PrimitiveType primitiveType = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveType.setType("String")

        expect:
        isValidInputType(primitiveType)
    }

    def "Scalar Type Definition Is Valid As Input"() {
        given:
        ScalarTypeDefinition scalarTypeDefinition = GraphQLFactoryDelegate.createScalarTypeDefinition()
        scalarTypeDefinition.setName("TestScalar")
        NamedType namedType = createNamedType(scalarTypeDefinition)

        expect:
        isValidInputType(namedType)
    }

    def "is Compatible Scalars Test Both Nullable"() {
        when:
        PrimitiveType primitiveTypeStub = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeStub.setType("String")

        PrimitiveType primitiveTypeTarget = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeTarget.setType("String")

        then:
        // both nullable
        isCompatible(primitiveTypeStub, primitiveTypeTarget)
    }

    def "is Compatible Scalars Test Stub Nullable"() {
        when:
        PrimitiveType primitiveTypeStub = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeStub.setType("String")

        PrimitiveType primitiveTypeTarget = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeTarget.setType("String")

        primitiveTypeStub.setNonNull(false)
        primitiveTypeTarget.setNonNull(true)

        then:
        isCompatible(primitiveTypeStub, primitiveTypeTarget)
    }

    def "is Compatible Scalars Test Target Nullable"() {
        when:
        PrimitiveType primitiveTypeStub = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeStub.setType("String")

        PrimitiveType primitiveTypeTarget = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeTarget.setType("String")

        // stud type should be less restrictive
        primitiveTypeStub.setNonNull(true)
        primitiveTypeTarget.setNonNull(false)

        then:
        !isCompatible(primitiveTypeStub, primitiveTypeTarget)
    }

    def "is Compatible Scalars Test Both Non Nullable"() {
        when:
        PrimitiveType primitiveTypeStub = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeStub.setType("String")

        PrimitiveType primitiveTypeTarget = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeTarget.setType("String")

        primitiveTypeStub.setNonNull(true)
        primitiveTypeTarget.setNonNull(true)

        then:
        isCompatible(primitiveTypeStub, primitiveTypeTarget)
    }

    def "is Compatible Wrapped Types Test"() {
        given:
        ListType listType = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(objectTypeDefinition)
        listType.setType(namedType); //[Arg]

        ListType outerlistType = GraphQLFactoryDelegate.createListType()
        outerlistType.setType(listType); //[[Arg]]

        expect:
        !isCompatible(listType, outerlistType)
        !isCompatible(outerlistType, listType)
    }

    def "is Compatible Wrapped Non Null Types Test"() {
        given:
        ListType listType = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(objectTypeDefinition)
        namedType.setNonNull(true)
        listType.setType(namedType); //[Arg!]

        ListType listType2 = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition2 = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition2.setName("Arg1")
        NamedType namedType2 = createNamedType(objectTypeDefinition2)
        listType2.setType(namedType2); //[Arg]

        expect:
        !isCompatible(listType, listType2)
        isCompatible(listType2, listType)
    }

    def "is Compatible Same Type Wrapping Non Null Types Test"() {
        given:
        ListType listType = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(objectTypeDefinition)
        namedType.setNonNull(true)
        listType.setType(namedType)
        listType.setNonNull(true);//[Arg!]!

        ListType listType2 = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition2 = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition2.setName("Arg1")
        NamedType namedType2 = createNamedType(objectTypeDefinition2)
        namedType2.setNonNull(true)
        listType2.setType(namedType2)
        listType2.setNonNull(true); //[Arg!]!

        expect:
        isCompatible(listType, listType2)
        isCompatible(listType2, listType)
    }

    def "is Compatible Wrapped And Non Wrapped Type Test"() {
        given:
        ListType listType = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(objectTypeDefinition)
        namedType.setNonNull(true)
        listType.setType(namedType);//[Arg1]

        ObjectTypeDefinition objectTypeDefinition2 = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition2.setName("Arg1")
        NamedType namedType2 = createNamedType(objectTypeDefinition2)
        namedType2.setNonNull(true); // Arg1

        expect:
        !isCompatible(listType, namedType2)
        !isCompatible(namedType2, listType)
    }

    def "get Field Definition Throws Exception For Non Entity Extension Types"() {
        given:
        EnumTypeExtensionDefinition badExtension = GraphQLFactoryDelegate.createEnumTypeExtensionDefinition()

        when:
        getFieldDefinitions(badExtension)

        then:
        thrown(IllegalArgumentException)
    }

    def "get Field Definition Returns Empty List For Non Entity Extension Types With Default Set"() {
        given:
        EnumTypeExtensionDefinition badExtension = GraphQLFactoryDelegate.createEnumTypeExtensionDefinition()
        List<FieldDefinition> fields = getFieldDefinitions(badExtension, true)

        expect:
        fields.size() == 0
    }

    def "get Field Definition Returns Fields For Object Type Extension Definition"() {
        given:
        String fieldName = "Test Field"
        ObjectTypeExtensionDefinition objectTypeExtensionDefinition = GraphQLFactoryDelegate.createObjectTypeExtensionDefinition()
        FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition()
        fieldDefinition.setName(fieldName)

        objectTypeExtensionDefinition.getFieldDefinition().add(fieldDefinition)

        List<FieldDefinition> fieldDefinitionList = getFieldDefinitions(objectTypeExtensionDefinition)

        expect:
        fieldDefinitionList.size() == 1
        fieldDefinitionList.get(0).getName() == fieldName
    }

    def "get Field Definition Returns Fields For Interface Type Extension Definition"() {
        given:
        String fieldName = "Test Interface Field"
        InterfaceTypeExtensionDefinition interfaceTypeExtensionDefinition = GraphQLFactoryDelegate.createInterfaceTypeExtensionDefinition()
        FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition()
        fieldDefinition.setName(fieldName)

        interfaceTypeExtensionDefinition.getFieldDefinition().add(fieldDefinition)

        List<FieldDefinition> fieldDefinitionList = getFieldDefinitions(interfaceTypeExtensionDefinition)

        expect:
        fieldDefinitionList.size() == 1
        fieldDefinitionList.get(0).getName() == fieldName
    }

    def "is Object Type Extension Definition Returns True For Object Type Extension"() {
        expect:
        isObjectTypeExtensionDefinition(GraphQLFactoryDelegate.createObjectTypeExtensionDefinition())
    }

    def "is Object Type Extension Definition Returns False For Interface Type Extension"() {
        expect:
        !isObjectTypeExtensionDefinition(GraphQLFactoryDelegate.createInterfaceTypeExtensionDefinition())
    }

    def "is Object Type Extension Definition Returns False For Null"() {
        expect:
        !isObjectTypeExtensionDefinition(null)
    }

    def "is Interface Type Extension Definition Returns False For Object Type Extension"() {
        expect:
        isInterfaceTypeExtensionDefinition(GraphQLFactoryDelegate.createInterfaceTypeExtensionDefinition())
    }

    def "is Interface Type Extension Definition Returns Truee For Interface Type Extension"() {
        expect:
        !isInterfaceTypeExtensionDefinition(GraphQLFactoryDelegate.createObjectTypeExtensionDefinition())
    }

    def "is Interface Type Extension Definition Returns False For Null"() {
        expect:
        !isInterfaceTypeExtensionDefinition(null)
    }

    def "getNamedTypeName returns the name of the type definition for object type"() {
        given:
        EnumTypeDefinition enumTypeDefinition = GraphQLFactoryDelegate.createEnumTypeDefinition()
        enumTypeDefinition.setName("Arg1")
        ObjectType objectType = GraphQLFactoryDelegate.createObjectType()
        objectType.setType(enumTypeDefinition)

        expect:
        getNamedTypeName(objectType) == "Arg1"
    }

    def "getNamedTypeName returns the name of the object for primitive type"() {
        given:
        PrimitiveType primitiveType = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveType.setType("Arg1")

        expect:
        getNamedTypeName(primitiveType) == "Arg1"
    }

    def "getNamedTypeName returns the name of the object for list type"() {
        given:
        ListType listType = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        listType.setType(createNamedType(objectTypeDefinition))

        expect:
        getNamedTypeName(listType) == "Arg1"
    }

    def "getNamedTypeNam returns null for a null value"() {
        expect:
        getNamedTypeName(null) == null
    }


    def "get Field Definition throws exception for enum type definition with default list disabled"() {
        given:
        String fieldName = "Test Interface Field"
        EnumTypeDefinition enumTypeDefinition = GraphQLFactoryDelegate.createEnumTypeDefinition()
        enumTypeDefinition.setName(fieldName)

        when:
        getFieldDefinitions(enumTypeDefinition, false)

        then:
        thrown(IllegalArgumentException.class)
    }

    def "compare two equal InputObjectTypes Test"() {
        given:
        PrimitiveType stringType = GraphQLFactoryDelegate.createPrimitiveType().setType("String")
        // Input Type 1
        InputValueDefinition inputValueDefinition1key = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinition1key.setName("key")
        inputValueDefinition1key.setNamedType(stringType)

        InputValueDefinition inputValueDefinition1value = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinition1value.setName("value")
        inputValueDefinition1value.setNamedType(stringType)

        InputObjectTypeDefinition inputObjectTypeDefinition1 = GraphQLFactoryDelegate.createInputObjectTypeDefinition()
        inputObjectTypeDefinition1.setName("TestInput")
        inputObjectTypeDefinition1.getInputValueDefinition().add(inputValueDefinition1key)
        inputObjectTypeDefinition1.getInputValueDefinition().add(inputValueDefinition1value)

        // Input Type 2
        InputValueDefinition inputValueDefinition2key = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinition2key.setName("key")
        inputValueDefinition2key.setNamedType(stringType)

        InputValueDefinition inputValueDefinition2value = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinition2value.setName("value")
        inputValueDefinition2value.setNamedType(stringType)

        InputObjectTypeDefinition inputObjectTypeDefinition2 = GraphQLFactoryDelegate.createInputObjectTypeDefinition()
        inputObjectTypeDefinition2.setName("TestInput")
        inputObjectTypeDefinition2.getInputValueDefinition().add(inputValueDefinition2key)
        inputObjectTypeDefinition2.getInputValueDefinition().add(inputValueDefinition2value)


        when:
        checkInputObjectTypeCompatibility(inputObjectTypeDefinition1, inputObjectTypeDefinition2)

        then:
        noExceptionThrown()
    }

    def "compare two InputObjectTypes with different InputValueDefinition Test"() {
        given:
        PrimitiveType stringType = GraphQLFactoryDelegate.createPrimitiveType().setType("String")
        // Input Type 1
        InputValueDefinition inputValueDefinition1key = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinition1key.setName("key")
        inputValueDefinition1key.setNamedType(stringType)

        InputValueDefinition inputValueDefinition1value = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinition1value.setName("value")
        inputValueDefinition1value.setNamedType(stringType)

        InputObjectTypeDefinition inputObjectTypeDefinition1 = GraphQLFactoryDelegate.createInputObjectTypeDefinition()
        inputObjectTypeDefinition1.setName("TestInput")
        inputObjectTypeDefinition1.getInputValueDefinition().add(inputValueDefinition1key)
        inputObjectTypeDefinition1.getInputValueDefinition().add(inputValueDefinition1value)

        // Input Type 2
        InputValueDefinition inputValueDefinition2key = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinition2key.setName("anotherKey")
        inputValueDefinition2key.setNamedType(stringType)

        InputValueDefinition inputValueDefinition2value = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinition2value.setName("anotherValue")
        inputValueDefinition2value.setNamedType(stringType)

        InputObjectTypeDefinition inputObjectTypeDefinition2 = GraphQLFactoryDelegate.createInputObjectTypeDefinition()
        inputObjectTypeDefinition2.setName("TestInput")
        inputObjectTypeDefinition2.getInputValueDefinition().add(inputValueDefinition2key)
        inputObjectTypeDefinition2.getInputValueDefinition().add(inputValueDefinition2value)


        when:
        checkInputObjectTypeCompatibility(inputObjectTypeDefinition1, inputObjectTypeDefinition2)

        then:
        def e = thrown(TypeConflictException)
        e.message.contains("Both types much have the same InputValueDefinition")
    }

    def "compare two InputObjectTypes with different InputValueDefinitions Size Test"() {
        given:
        PrimitiveType stringType = GraphQLFactoryDelegate.createPrimitiveType().setType("String")
        // Input Type 1
        InputValueDefinition inputValueDefinition1key = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinition1key.setName("key")
        inputValueDefinition1key.setNamedType(stringType)

        InputValueDefinition inputValueDefinition1value = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinition1value.setName("value")
        inputValueDefinition1value.setNamedType(stringType)

        InputObjectTypeDefinition inputObjectTypeDefinition1 = GraphQLFactoryDelegate.createInputObjectTypeDefinition()
        inputObjectTypeDefinition1.setName("TestInput")
        inputObjectTypeDefinition1.getInputValueDefinition().add(inputValueDefinition1key)
        inputObjectTypeDefinition1.getInputValueDefinition().add(inputValueDefinition1value)

        // Input Type 2
        InputValueDefinition inputValueDefinition2key = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinition2key.setName("anotherKey")
        inputValueDefinition2key.setNamedType(stringType)

        // only one InputValueDefinition

        InputObjectTypeDefinition inputObjectTypeDefinition2 = GraphQLFactoryDelegate.createInputObjectTypeDefinition()
        inputObjectTypeDefinition2.setName("TestInput")
        inputObjectTypeDefinition2.getInputValueDefinition().add(inputValueDefinition2key)


        when:
        checkInputObjectTypeCompatibility(inputObjectTypeDefinition1, inputObjectTypeDefinition2)

        then:
        def e = thrown(TypeConflictException)
        e.message.contains("Both types must be of the same size")
    }

    def "compare incoming InputObjectType to an existing non InputObjectType test"() {
        given:
        PrimitiveType stringType = GraphQLFactoryDelegate.createPrimitiveType().setType("String")
        // Object Type
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()

        // Input Type
        InputValueDefinition inputValueDefinition2key = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinition2key.setName("anotherKey")
        inputValueDefinition2key.setNamedType(stringType)

        // only one InputValueDefinition

        InputObjectTypeDefinition inputObjectTypeDefinition2 = GraphQLFactoryDelegate.createInputObjectTypeDefinition()
        inputObjectTypeDefinition2.setName("TestInput")
        inputObjectTypeDefinition2.getInputValueDefinition().add(inputValueDefinition2key)


        when:
        checkInputObjectTypeCompatibility(objectTypeDefinition, inputObjectTypeDefinition2)

        then:
        def e = thrown(TypeConflictException)
        e.message.contains("Both types must be of the same type")
    }

    def "compare incoming non InputObjectType to an existing InputObjectType test"() {
        given:
        PrimitiveType stringType = GraphQLFactoryDelegate.createPrimitiveType().setType("String")
        // Input Type
        InputValueDefinition inputValueDefinitionkey = GraphQLFactoryDelegate.createInputValueDefinition()
        inputValueDefinitionkey.setName("key")
        inputValueDefinitionkey.setNamedType(stringType)

        // only one InputValueDefinition

        InputObjectTypeDefinition inputObjectTypeDefinition = GraphQLFactoryDelegate.createInputObjectTypeDefinition()
        inputObjectTypeDefinition.setName("TestInput")
        inputObjectTypeDefinition.getInputValueDefinition().add(inputValueDefinitionkey)

        // Object Type
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()



        when:
        checkInputObjectTypeCompatibility(inputObjectTypeDefinition, objectTypeDefinition)

        then:
        def e = thrown(TypeConflictException)
        e.message.contains("Both types must be of the same type")
    }
}
