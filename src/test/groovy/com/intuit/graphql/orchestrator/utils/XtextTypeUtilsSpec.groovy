package com.intuit.graphql.orchestrator.utils

import com.intuit.graphql.graphQL.*
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.*

class XtextTypeUtilsSpec extends Specification {

    void compareWrappedTypesTest() {
        given:
        ListType listType = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(objectTypeDefinition)
        listType.setType(namedType); //[Arg]

        ListType outerlistType = GraphQLFactoryDelegate.createListType()
        outerlistType.setType(listType); //[[Arg]]

        expect:
        !XtextTypeUtils.compareTypes(listType, outerlistType)
    }

    void compareNonNullTypesTest() {
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
        !XtextTypeUtils.compareTypes(listType, listType2)
    }

    void compareEqualWrappedNonNullTypesTest() {
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
        XtextTypeUtils.compareTypes(listType, listType2)
    }

    void generateArgumentsDefinitionStringTest() {
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
        [ "argument1", "desc1", "argument2", "desc2" ]
                .findAll({toDescriptiveString(argumentsDefinition)
                        .contains(it)}).size() == 4
    }

    void generateDirectivesStringTest() {
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
        [ "directive1", "description1", "directive2", "description2" ]
                .findAll({toDescriptiveString(fieldDefinition.getDirectives())
                        .contains(it)}).size() == 4
    }

    void throwsExceptionForObjectTypeNotValidAsInput() {
        given:
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(objectTypeDefinition)

        ListType listType = GraphQLFactoryDelegate.createListType()
        listType.setType(namedType)

        expect:
        !XtextTypeUtils.isValidInputType(listType)
    }

    void InputObjectTypeIsValidAsInput() {
        given:
        InputObjectTypeDefinition inputObjectTypeDefinition = GraphQLFactoryDelegate.createInputObjectTypeDefinition()
        inputObjectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(inputObjectTypeDefinition)

        ListType listType = GraphQLFactoryDelegate.createListType()
        listType.setType(namedType)

        expect:
        XtextTypeUtils.isValidInputType(listType)
    }

    void EnumTypeIsValidAsInput() {
        given:
        EnumTypeDefinition enumTypeDefinition = GraphQLFactoryDelegate.createEnumTypeDefinition()
        enumTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(enumTypeDefinition)

        expect:
        XtextTypeUtils.isValidInputType(namedType)
    }

    void ScalarTypeIsValidAsInput() {
        given:
        PrimitiveType primitiveType = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveType.setType("String")

        expect:
        XtextTypeUtils.isValidInputType(primitiveType)
    }

    void ScalarTypeDefinitionIsValidAsInput() {
        given:
        ScalarTypeDefinition scalarTypeDefinition = GraphQLFactoryDelegate.createScalarTypeDefinition()
        scalarTypeDefinition.setName("TestScalar")
        NamedType namedType = createNamedType(scalarTypeDefinition)

        expect:
        XtextTypeUtils.isValidInputType(namedType)
    }

    void isCompatibleScalarsTestBothNullable() {
        when:
        PrimitiveType primitiveTypeStub =  GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeStub.setType("String")

        PrimitiveType primitiveTypeTarget = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeTarget.setType("String")

        then:
        // both nullable
        XtextTypeUtils.isCompatible(primitiveTypeStub, primitiveTypeTarget)
    }

    void isCompatibleScalarsTestStubNullable() {
        when:
        PrimitiveType primitiveTypeStub =  GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeStub.setType("String")

        PrimitiveType primitiveTypeTarget = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeTarget.setType("String")

        primitiveTypeStub.setNonNull(false)
        primitiveTypeTarget.setNonNull(true)

        then:
        XtextTypeUtils.isCompatible(primitiveTypeStub, primitiveTypeTarget)
    }

    void isCompatibleScalarsTestTargetNullable() {
        when:
        PrimitiveType primitiveTypeStub =  GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeStub.setType("String")

        PrimitiveType primitiveTypeTarget = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeTarget.setType("String")

        // stud type should be less restrictive
        primitiveTypeStub.setNonNull(true)
        primitiveTypeTarget.setNonNull(false)

        then:
        !XtextTypeUtils.isCompatible(primitiveTypeStub, primitiveTypeTarget)
    }

    void isCompatibleScalarsTestBothNonNullable() {
        when:
        PrimitiveType primitiveTypeStub =  GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeStub.setType("String")

        PrimitiveType primitiveTypeTarget = GraphQLFactoryDelegate.createPrimitiveType()
        primitiveTypeTarget.setType("String")

        primitiveTypeStub.setNonNull(true)
        primitiveTypeTarget.setNonNull(true)

        then:
        XtextTypeUtils.isCompatible(primitiveTypeStub, primitiveTypeTarget)
    }

    void isCompatibleWrappedTypesTest() {
        given:
        ListType listType = GraphQLFactoryDelegate.createListType()
        ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition()
        objectTypeDefinition.setName("Arg1")
        NamedType namedType = createNamedType(objectTypeDefinition)
        listType.setType(namedType); //[Arg]

        ListType outerlistType = GraphQLFactoryDelegate.createListType()
        outerlistType.setType(listType); //[[Arg]]

        expect:
        !XtextTypeUtils.isCompatible(listType, outerlistType)
        !XtextTypeUtils.isCompatible(outerlistType, listType)
    }

    void isCompatibleWrappedNonNullTypesTest() {
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
        !XtextTypeUtils.isCompatible(listType, listType2)
        XtextTypeUtils.isCompatible(listType2, listType)
    }

    void isCompatibleSameTypeWrappingNonNullTypesTest() {
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
        XtextTypeUtils.isCompatible(listType, listType2)
        XtextTypeUtils.isCompatible(listType2, listType)
    }

    void isCompatibleWrappedAndNonWrappedTypeTest() {
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
        !XtextTypeUtils.isCompatible(listType, namedType2)
        !XtextTypeUtils.isCompatible(namedType2, listType)
    }

    void getFieldDefinitionThrowsExceptionForNonEntityExtensionTypes(){
        given:
        EnumTypeExtensionDefinition badExtension = GraphQLFactoryDelegate.createEnumTypeExtensionDefinition()

        when:
        getFieldDefinitions(badExtension)

        then:
        thrown(IllegalArgumentException)
    }

    void getFieldDefinitionReturnsEmptyListForNonEntityExtensionTypesWithDefaultSet(){
        given:
        EnumTypeExtensionDefinition badExtension = GraphQLFactoryDelegate.createEnumTypeExtensionDefinition()
        List<FieldDefinition> fields = getFieldDefinitions(badExtension, true)

        expect:
        fields.size() == 0
    }

    void getFieldDefinitionReturnsFieldsForObjectTypeExtensionDefinition(){
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

    void getFieldDefinitionReturnsFieldsForInterfaceTypeExtensionDefinition(){
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

    void isObjectTypeExtensionDefinitionReturnsTrueForObjectTypeExtension() {
        expect:
        isObjectTypeExtensionDefinition(GraphQLFactoryDelegate.createObjectTypeExtensionDefinition())
    }

    void isObjectTypeExtensionDefinitionReturnsFalseForInterfaceTypeExtension() {
        expect:
        !isObjectTypeExtensionDefinition(GraphQLFactoryDelegate.createInterfaceTypeExtensionDefinition())
    }

    void isObjectTypeExtensionDefinitionReturnsFalseForNull() {
        expect:
        !isObjectTypeExtensionDefinition(null)
    }

    void isInterfaceTypeExtensionDefinitionReturnsFalseForObjectTypeExtension() {
        expect:
        isInterfaceTypeExtensionDefinition(GraphQLFactoryDelegate.createInterfaceTypeExtensionDefinition())
    }

    void isInterfaceTypeExtensionDefinitionReturnsTrueeForInterfaceTypeExtension() {
        expect:
        !isInterfaceTypeExtensionDefinition(GraphQLFactoryDelegate.createObjectTypeExtensionDefinition())
    }

    void isInterfaceTypeExtensionDefinitionReturnsFalseForNull() {
        expect:
        !isInterfaceTypeExtensionDefinition(null)
    }

}
