package com.intuit.graphql.orchestrator.federationdirectives.keydirective

import com.intuit.graphql.graphQL.*
import com.intuit.graphql.orchestrator.federation.exceptions.DirectiveMissingRequiredArgumentException
import com.intuit.graphql.orchestrator.federation.exceptions.EmptyFieldsArgumentFederationDirective
import com.intuit.graphql.orchestrator.federation.exceptions.IncorrectDirectiveArgumentSizeException
import com.intuit.graphql.orchestrator.federation.exceptions.InvalidFieldSetReferenceException
import com.intuit.graphql.orchestrator.federation.validators.KeyDirectiveValidator
import com.intuit.graphql.orchestrator.xtext.XtextGraph
import org.eclipse.emf.common.util.BasicEList
import org.eclipse.emf.common.util.EList
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import spock.lang.Specification

import java.util.Arrays
import java.util.Collections
import java.util.List

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_FIELDS_ARGUMENT
import static org.mockito.Mockito.mock
import static org.mockito.MockitoAnnotations.initMocks

class KeyDirectiveValidatorSpec extends Specification {

    KeyDirectiveValidator keyDirectiveValidator = new KeyDirectiveValidator()

    ObjectTypeDefinition objectTypeDefinitionMock

    Argument argumentMock

    XtextGraph xtextGraphMock

    def setup() {
        objectTypeDefinitionMock = Mock(ObjectTypeDefinition)

        argumentMock = Mock(Argument)

        xtextGraphMock = Mock(XtextGraph)
    }

    def "assert Validator Throws Exception For Multiple Args"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)

        objectTypeDefinitionMock.getName() >> "validType"
        objectTypeDefinitionMock.eContainer() >> mockedTypeSystemDefinition

        List<Argument> argumentList = Arrays.asList(argumentMock, argumentMock)

        when:
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList)

        then:
        thrown(IncorrectDirectiveArgumentSizeException)
    }

    def "assert Validator Throws Exception For Invalid Arg Name"() {
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)

        objectTypeDefinitionMock.getName() >> "validType"
        objectTypeDefinitionMock.eContainer() >> mockedTypeSystemDefinition

        argumentMock.getName() >> "field"

        List<Argument> argumentList = Arrays.asList(argumentMock)

        when:
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList)

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

        List<Argument> argumentList = Arrays.asList(argumentMock)

        when:
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList)

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

        EList<FieldDefinition> typeSchemaFields= new BasicEList()
        typeSchemaFields.add(mockedSchemaField1)
        typeSchemaFields.add(mockedSchemaField2)
        typeSchemaFields.add(mockedSchemaField3)
        typeSchemaFields.add(mockedSchemaField4)

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

        List<Argument> argumentList = Arrays.asList(argumentMock)

        when:
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList)

        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList)

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

        EList<FieldDefinition> typeSchemaFields= new BasicEList()
        typeSchemaFields.add(mockedSchemaField1)
        typeSchemaFields.add(mockedSchemaField2)
        typeSchemaFields.add(mockedSchemaField3)
        typeSchemaFields.add(mockedSchemaField4)

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

        List<Argument> argumentList = Collections.singletonList(argumentMock)

        when:
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList)

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

        EList<FieldDefinition> typeSchemaFields= new BasicEList()
        typeSchemaFields.add(mockedSchemaField1)
        typeSchemaFields.add(mockedSchemaField2)
        typeSchemaFields.add(mockedSchemaField3)
        typeSchemaFields.add(mockedSchemaField4)

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

        List<Argument> argumentList = Arrays.asList(argumentMock)

        when:
        keyDirectiveValidator.validate(xtextGraphMock, objectTypeDefinitionMock, argumentList)

        then:
        thrown(InvalidFieldSetReferenceException)
    }

    def "assert Validator No Exception For Entity With Multiple Fields"() {
        given:
        TypeSystemDefinition mockedTypeSystemDefinition = Mock(TypeSystemDefinition.class)
        ValueWithVariable fieldsArgMock = Mock(ValueWithVariable.class)

        FieldDefinition mockedSchemaField1 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField2 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField3 = Mock(FieldDefinition.class)
        FieldDefinition mockedSchemaField4 = Mock(FieldDefinition.class)

        EList<FieldDefinition> typeSchemaFields= new BasicEList()
        typeSchemaFields.add(mockedSchemaField1)
        typeSchemaFields.add(mockedSchemaField2)
        typeSchemaFields.add(mockedSchemaField3)
        typeSchemaFields.add(mockedSchemaField4)

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

        List<Argument> argumentList = Collections.singletonList(argumentMock)

        when:
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList)

        then:
        noExceptionThrown()
    }
}
