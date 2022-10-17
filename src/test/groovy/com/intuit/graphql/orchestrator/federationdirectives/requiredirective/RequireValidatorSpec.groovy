package com.intuit.graphql.orchestrator.federationdirectives.requiredirective

import com.intuit.graphql.graphQL.*
import com.intuit.graphql.orchestrator.federation.exceptions.DirectiveMissingRequiredArgumentException
import com.intuit.graphql.orchestrator.federation.exceptions.EmptyFieldsArgumentFederationDirective
import com.intuit.graphql.orchestrator.federation.exceptions.IncorrectDirectiveArgumentSizeException
import com.intuit.graphql.orchestrator.federation.exceptions.InvalidFieldSetReferenceException
import com.intuit.graphql.orchestrator.federation.validators.RequireValidator
import com.intuit.graphql.orchestrator.xtext.XtextGraph
import org.eclipse.emf.common.util.ECollections
import org.eclipse.emf.common.util.EList
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_FIELDS_ARGUMENT

class RequireValidatorSpec extends Specification {
    RequireValidator requireValidator = new RequireValidator()

    Directive directiveMock

    ObjectTypeDefinition objectTypeDefinitionMock

    ObjectTypeExtensionDefinition objectTypeExtensionDefinitionMock

    Argument argumentMock

    XtextGraph xtextGraphMock

    def setup() {
        directiveMock = Mock(Directive)
        objectTypeDefinitionMock = Mock(ObjectTypeDefinition)
        objectTypeExtensionDefinitionMock = Mock(ObjectTypeExtensionDefinition)
        argumentMock = Mock(Argument)
        xtextGraphMock = Mock(XtextGraph)
    }

    def "assert Validator Throws Exception For Multiple Args"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)

        objectTypeDefinitionMock.eContainer() >> mockedTypeSystemDefinition

        EList<Argument> argumentList = ECollections.asEList(argumentMock, argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock)

        then:
        thrown(IncorrectDirectiveArgumentSizeException)
    }

    def "assert Validator Throws Exception For Invalid Arg Name"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)

        objectTypeDefinitionMock.eContainer() >> mockedTypeSystemDefinition

        argumentMock.getName() >> "field"

        EList<Argument> argumentList = ECollections.asEList(argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock)

        then:
        thrown(DirectiveMissingRequiredArgumentException)
    }

    def "assert Validator Throws Exception For Empty Fields Arg"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)
        ValueWithVariable fieldsArgMock = Mock(ValueWithVariable.class)

        objectTypeDefinitionMock.getName() >> "validType"
        objectTypeDefinitionMock.eContainer() >> mockedTypeSystemDefinition

        fieldsArgMock.getStringValue() >> " "

        argumentMock.getName() >> FEDERATION_FIELDS_ARGUMENT
        argumentMock.getValueWithVariable() >> fieldsArgMock

        EList<Argument> argumentList = ECollections.asEList(argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock)

        then:
        thrown(EmptyFieldsArgumentFederationDirective)
    }

    def "assert Validator Throws Exception For Invalid Field Reference"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)
        ValueWithVariable fieldsArgMock = Mock(ValueWithVariable.class)

        FieldDefinition mockedSchemaField1 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField2 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField3 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField4 = Mock(FieldDefinition.class)

        EList<FieldDefinition> typeSchemaFields= ECollections.asEList(mockedSchemaField1,mockedSchemaField2,
                mockedSchemaField3, mockedSchemaField4)

        mockedSchemaField1.getName() >> "id"
        mockedSchemaField2.getName() >> "fooBarRef"
        mockedSchemaField3.getName() >> "batId"
        mockedSchemaField4.getName() >> "Set"

        objectTypeDefinitionMock.getName() >> "validType"
        objectTypeDefinitionMock.eContainer() >> mockedTypeSystemDefinition
        objectTypeDefinitionMock.getFieldDefinition() >> typeSchemaFields

        fieldsArgMock.getStringValue() >> "Id"

        argumentMock.getName() >> FEDERATION_FIELDS_ARGUMENT
        argumentMock.getValueWithVariable() >> fieldsArgMock

        EList<Argument> argumentList = ECollections.asEList(argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock)

        then:
        thrown(InvalidFieldSetReferenceException)
    }

    def "assert Validator No Exception For Entity With Single Field"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)
        ValueWithVariable fieldsArgMock = Mock(ValueWithVariable.class)

        FieldDefinition mockedSchemaField1 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField2 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField3 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField4 = Mock(FieldDefinition.class)

        EList<FieldDefinition> typeSchemaFields= ECollections.asEList(mockedSchemaField1,mockedSchemaField2,
                mockedSchemaField3, mockedSchemaField4)

        mockedSchemaField1.getName() >> "id"
        mockedSchemaField2.getName() >> "fooBarRef"
        mockedSchemaField3.getName() >> "batId"
        mockedSchemaField4.getName() >> "Set"

        objectTypeDefinitionMock.getName() >> "validType"
        objectTypeDefinitionMock.eContainer() >> mockedTypeSystemDefinition
        objectTypeDefinitionMock.getFieldDefinition() >> typeSchemaFields

        fieldsArgMock.getStringValue() >> "id"

        argumentMock.getName() >> FEDERATION_FIELDS_ARGUMENT
        argumentMock.getValueWithVariable() >> fieldsArgMock

        EList<Argument> argumentList = ECollections.asEList(argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock)

        then:
        noExceptionThrown()
    }

    def "assert Validator Throws Exception For Invalid Field Reference Multiple Fields"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)
        ValueWithVariable fieldsArgMock = Mock(ValueWithVariable.class)

        FieldDefinition mockedSchemaField1 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField2 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField3 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField4 = Mock(FieldDefinition.class)

        EList<FieldDefinition> typeSchemaFields= ECollections.asEList(mockedSchemaField1,mockedSchemaField2,
                mockedSchemaField3, mockedSchemaField4)

        mockedSchemaField1.getName() >> "id"
        mockedSchemaField2.getName() >> "fooBarRef"
        mockedSchemaField3.getName() >> "batId"
        mockedSchemaField4.getName() >> "Set"

        objectTypeDefinitionMock.getName() >> "validType"
        objectTypeDefinitionMock.eContainer() >> mockedTypeSystemDefinition
        objectTypeDefinitionMock.getFieldDefinition() >> typeSchemaFields

        fieldsArgMock.getStringValue() >> "batid"

        argumentMock.getName() >> FEDERATION_FIELDS_ARGUMENT
        argumentMock.getValueWithVariable() >> fieldsArgMock

        EList<Argument> argumentList = ECollections.asEList(argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock)

        then:
        thrown(InvalidFieldSetReferenceException)
    }

    def "assertValidatorNoExceptionForEntityWithMultipleFields"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)
        ValueWithVariable fieldsArgMock = Mock(ValueWithVariable.class)

        FieldDefinition mockedSchemaField1 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField2 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField3 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField4 = Mock(FieldDefinition.class)

        EList<FieldDefinition> typeSchemaFields= ECollections.asEList(mockedSchemaField1,mockedSchemaField2,
                mockedSchemaField3, mockedSchemaField4)

        mockedSchemaField1.getName() >> "id"
        mockedSchemaField2.getName() >> "fooBarRef"
        mockedSchemaField3.getName() >> "batId"
        mockedSchemaField4.getName() >> "Set"

        objectTypeDefinitionMock.getName() >> "validType"
        objectTypeDefinitionMock.eContainer() >> mockedTypeSystemDefinition
        objectTypeDefinitionMock.getFieldDefinition() >> typeSchemaFields

        fieldsArgMock.getStringValue() >> "id batId"

        argumentMock.getName() >> FEDERATION_FIELDS_ARGUMENT
        argumentMock.getValueWithVariable() >> fieldsArgMock

        EList<Argument> argumentList = ECollections.asEList(argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock)

        then:
        noExceptionThrown()
    }


    def "assert Validator Throws Exception For Multiple Args with TypeExtensionDefinition"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)

        objectTypeExtensionDefinitionMock.eContainer() >> mockedTypeSystemDefinition

        EList<Argument> argumentList = ECollections.asEList(argumentMock, argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeExtensionDefinitionMock, directiveMock)

        then:
        thrown(IncorrectDirectiveArgumentSizeException)
    }

    def "assert Validator Throws Exception For Invalid Arg Name with TypeExtensionDefinition"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)

        objectTypeExtensionDefinitionMock.eContainer() >> mockedTypeSystemDefinition

        argumentMock.getName() >> "field"

        EList<Argument> argumentList = ECollections.asEList(argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeExtensionDefinitionMock, directiveMock)

        then:
        thrown(DirectiveMissingRequiredArgumentException)
    }

    def "assert Validator Throws Exception For Empty Fields Arg With TypeExtensionDefinition"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)
        ValueWithVariable fieldsArgMock = Mock(ValueWithVariable.class)

        objectTypeExtensionDefinitionMock.getName() >> "validType"
        objectTypeExtensionDefinitionMock.eContainer() >> mockedTypeSystemDefinition

        fieldsArgMock.getStringValue() >> " "

        argumentMock.getName() >> FEDERATION_FIELDS_ARGUMENT
        argumentMock.getValueWithVariable() >> fieldsArgMock

        EList<Argument> argumentList = ECollections.asEList(argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeExtensionDefinitionMock, directiveMock)

        then:
        thrown(EmptyFieldsArgumentFederationDirective)
    }

    def "assert Validator Throws Exception For Invalid Field Reference with TypeExtensionDefinition"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)
        ValueWithVariable fieldsArgMock = Mock(ValueWithVariable.class)

        FieldDefinition mockedSchemaField1 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField2 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField3 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField4 = Mock(FieldDefinition.class)

        EList<FieldDefinition> typeSchemaFields= ECollections.asEList(mockedSchemaField1,mockedSchemaField2,
                mockedSchemaField3, mockedSchemaField4)

        mockedSchemaField1.getName() >> "id"
        mockedSchemaField2.getName() >> "fooBarRef"
        mockedSchemaField3.getName() >> "batId"
        mockedSchemaField4.getName() >> "Set"

        objectTypeExtensionDefinitionMock.getName() >> "validType"
        objectTypeExtensionDefinitionMock.eContainer() >> mockedTypeSystemDefinition
        objectTypeExtensionDefinitionMock.getFieldDefinition() >> typeSchemaFields

        fieldsArgMock.getStringValue() >> "Id"

        argumentMock.getName() >> FEDERATION_FIELDS_ARGUMENT
        argumentMock.getValueWithVariable() >> fieldsArgMock

        EList<Argument> argumentList = ECollections.asEList(argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeExtensionDefinitionMock, directiveMock)

        then:
        thrown(InvalidFieldSetReferenceException)
    }

    def "assert Validator No Exception For Entity With Single Field with TypeExtensionDefinition"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)
        ValueWithVariable fieldsArgMock = Mock(ValueWithVariable.class)

        FieldDefinition mockedSchemaField1 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField2 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField3 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField4 = Mock(FieldDefinition.class)

        EList<FieldDefinition> typeSchemaFields= ECollections.asEList(mockedSchemaField1,mockedSchemaField2,
                mockedSchemaField3, mockedSchemaField4)

        mockedSchemaField1.getName() >> "id"
        mockedSchemaField2.getName() >> "fooBarRef"
        mockedSchemaField3.getName() >> "batId"
        mockedSchemaField4.getName() >> "Set"

        objectTypeExtensionDefinitionMock.getName() >> "validType"
        objectTypeExtensionDefinitionMock.eContainer() >> mockedTypeSystemDefinition
        objectTypeExtensionDefinitionMock.getFieldDefinition() >> typeSchemaFields

        fieldsArgMock.getStringValue() >> "id"

        argumentMock.getName() >> FEDERATION_FIELDS_ARGUMENT
        argumentMock.getValueWithVariable() >> fieldsArgMock

        EList<Argument> argumentList = ECollections.asEList(argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeExtensionDefinitionMock, directiveMock)

        then:
        noExceptionThrown()
    }

    def "assert Validator Throws Exception For Invalid Field Reference Multiple Fields with TypeExtensionDefinition"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)
        ValueWithVariable fieldsArgMock = Mock(ValueWithVariable.class)

        FieldDefinition mockedSchemaField1 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField2 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField3 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField4 = Mock(FieldDefinition.class)

        EList<FieldDefinition> typeSchemaFields= ECollections.asEList(mockedSchemaField1,mockedSchemaField2,
                mockedSchemaField3, mockedSchemaField4)

        mockedSchemaField1.getName() >> "id"
        mockedSchemaField2.getName() >> "fooBarRef"
        mockedSchemaField3.getName() >> "batId"
        mockedSchemaField4.getName() >> "Set"

        objectTypeExtensionDefinitionMock.getName() >> "validType"
        objectTypeExtensionDefinitionMock.eContainer() >> mockedTypeSystemDefinition
        objectTypeExtensionDefinitionMock.getFieldDefinition() >> typeSchemaFields

        fieldsArgMock.getStringValue() >> "batid"

        argumentMock.getName() >> FEDERATION_FIELDS_ARGUMENT
        argumentMock.getValueWithVariable() >> fieldsArgMock

        EList<Argument> argumentList = ECollections.asEList(argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeExtensionDefinitionMock, directiveMock)

        then:
        thrown(InvalidFieldSetReferenceException)
    }

    def "assertValidatorNoExceptionForEntityWithMultipleFields with TypeExtensionDefinition"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)
        ValueWithVariable fieldsArgMock = Mock(ValueWithVariable.class)

        FieldDefinition mockedSchemaField1 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField2 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField3 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField4 = Mock(FieldDefinition.class)

        EList<FieldDefinition> typeSchemaFields= ECollections.asEList(mockedSchemaField1,mockedSchemaField2,
                mockedSchemaField3, mockedSchemaField4)

        mockedSchemaField1.getName() >> "id"
        mockedSchemaField2.getName() >> "fooBarRef"
        mockedSchemaField3.getName() >> "batId"
        mockedSchemaField4.getName() >> "Set"

        objectTypeExtensionDefinitionMock.getName() >> "validType"
        objectTypeExtensionDefinitionMock.eContainer() >> mockedTypeSystemDefinition
        objectTypeExtensionDefinitionMock.getFieldDefinition() >> typeSchemaFields

        fieldsArgMock.getStringValue() >> "id batId"

        argumentMock.getName() >> FEDERATION_FIELDS_ARGUMENT
        argumentMock.getValueWithVariable() >> fieldsArgMock

        EList<Argument> argumentList = ECollections.asEList(argumentMock)
        directiveMock.getArguments() >> argumentList

        when:
        requireValidator.validate(xtextGraphMock,objectTypeExtensionDefinitionMock, directiveMock)

        then:
        noExceptionThrown()
    }
}
