package com.intuit.graphql.orchestrator.federation

import com.intuit.graphql.graphQL.*
import com.intuit.graphql.orchestrator.federation.exceptions.EmptyFieldsArgumentFederationDirective
import com.intuit.graphql.orchestrator.federation.exceptions.InvalidFieldSetReferenceException
import com.intuit.graphql.orchestrator.federation.validators.FieldSetValidator
import com.intuit.graphql.orchestrator.xtext.XtextGraph
import graphql.parser.InvalidSyntaxException
import org.eclipse.emf.common.util.ECollections
import org.eclipse.emf.common.util.EList
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE

class FieldSetValidatorSpec extends Specification {
    FieldSetValidator fieldSetValidator = new FieldSetValidator()

    def "npe From Null Source Graph When Checking Empty Field Set"() {
        given:
        TypeDefinition typeDefinitionMock = Mock(TypeDefinition)
        String fieldSet = ""

        when:
        fieldSetValidator.validate(null, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(NullPointerException)
    }

    def "npe From Null Type Def When Checking Empty Field Set"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph)
        String fieldSet = ""

        when:
        fieldSetValidator.validate(sourceGraphMock, (TypeDefinition) null, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(NullPointerException)
    }

    def "Empty Fields Exception When Checking Empty Field Set With Empty Field Set"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph.class)
        TypeDefinition typeDefinitionMock = Mock(TypeDefinition.class)
        String fieldSet = ""

        when:
        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(EmptyFieldsArgumentFederationDirective)
    }

    def "Empty Fields Exception When Checking Empty Field Set With Null Field Set"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph)
        TypeDefinition typeDefinitionMock = Mock(TypeDefinition)

        when:
        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, null, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(EmptyFieldsArgumentFederationDirective)
    }

    def "Exception From Invalid Field Set When Checking Field Set"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph)
        TypeDefinition typeDefinitionMock = Mock(TypeDefinition)
        String fieldSet = "foo bar }"

        when:
        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(InvalidSyntaxException)
    }

    def "invalid Field Reference Exception With Field Set With Invalid Reference"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph)

        ObjectTypeDefinition typeDefinitionMock = Mock(ObjectTypeDefinition)
        FieldDefinition fieldDefinition1 = Mock(FieldDefinition)
        fieldDefinition1.getName() >> "foo"
        FieldDefinition fieldDefinition2 = Mock(FieldDefinition)
        fieldDefinition2.getName() >> "bar"
        FieldDefinition fieldDefinition3 = Mock(FieldDefinition)
        fieldDefinition3.getName() >> "foobar"

        EList<FieldDefinition> fieldDefinitionEList = ECollections.asEList(fieldDefinition1, fieldDefinition2, fieldDefinition3)

        typeDefinitionMock.getFieldDefinition() >> fieldDefinitionEList

        String fieldSet = "foo bar badField"

        when:
        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(InvalidFieldSetReferenceException)
    }

    def "invalid Field Reference Exception With Field Set With Invalid Children"() {
        XtextGraph sourceGraphMock = Mock(XtextGraph.class)

        ObjectTypeDefinition testDefinitionMock = Mock(ObjectTypeDefinition.class)
        FieldDefinition fieldDefinition1 = Mock(FieldDefinition.class)
        FieldDefinition fieldDefinition2 = Mock(FieldDefinition.class)
        FieldDefinition fieldDefinition3 = Mock(FieldDefinition.class)

        ObjectTypeDefinition fooBarTypeMock = Mock(ObjectTypeDefinition.class)
        FieldDefinition childFieldMock1 = Mock(FieldDefinition.class)
        FieldDefinition childFieldMock2 = Mock(FieldDefinition.class)

        EList<FieldDefinition> testFieldDefinitions = ECollections.asEList(fieldDefinition1, fieldDefinition2, fieldDefinition3)
        EList<FieldDefinition> foobarChildDefinitions = ECollections.asEList(childFieldMock1, childFieldMock2)

        fieldDefinition1.getName() >> "foo"
        fieldDefinition2.getName() >> "bar"
        fieldDefinition3.getName() >> "foobar"
        childFieldMock1.getName() >> "childField1"
        childFieldMock2.getName() >> "childField2"
        sourceGraphMock.getType(_) >> fooBarTypeMock
        testDefinitionMock.getFieldDefinition() >> testFieldDefinitions
        fooBarTypeMock.getFieldDefinition() >> foobarChildDefinitions

        String fieldSet = "foobar { childField1 childField2 missingField }"

        when:
        fieldSetValidator.validate(sourceGraphMock, testDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(InvalidFieldSetReferenceException)
    }

    def "no Exception With Valid Field Set"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph.class)

        ObjectTypeDefinition typeDefinitionMock = Mock(ObjectTypeDefinition.class)

        FieldDefinition fieldDefinition1 = Mock(FieldDefinition.class)
        fieldDefinition1.getName() >> "foo"

        FieldDefinition fieldDefinition2 = Mock(FieldDefinition.class)
        fieldDefinition2.getName() >> "bar"

        FieldDefinition fieldDefinition3 = Mock(FieldDefinition.class)
        fieldDefinition3.getName() >> "foobar"

        EList<FieldDefinition> fieldDefinitionEList = ECollections.asEList(fieldDefinition1, fieldDefinition2, fieldDefinition3)
        typeDefinitionMock.getFieldDefinition() >> fieldDefinitionEList

        String fieldSet = "foo bar"

        when:
        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        noExceptionThrown()
    }

    def "no Exception With Valid Field Set With Children"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph.class)

        ObjectTypeDefinition testDefinitionMock = Mock(ObjectTypeDefinition.class)
        FieldDefinition fieldDefinition1 = Mock(FieldDefinition.class)
        FieldDefinition fieldDefinition2 = Mock(FieldDefinition.class)
        FieldDefinition fieldDefinition3 = Mock(FieldDefinition.class)

        ObjectTypeDefinition fooBarTypeMock = Mock(ObjectTypeDefinition.class)
        FieldDefinition childFieldMock1 = Mock(FieldDefinition.class)
        FieldDefinition childFieldMock2 = Mock(FieldDefinition.class)

        EList<FieldDefinition> testFieldDefinitions = ECollections.asEList(fieldDefinition1, fieldDefinition2, fieldDefinition3)
        EList<FieldDefinition> foobarChildDefinitions = ECollections.asEList(childFieldMock1, childFieldMock2)

        fieldDefinition1.getName() >> "foo"
        fieldDefinition2.getName() >> "bar"
        fieldDefinition3.getName() >> "foobar"
        childFieldMock1.getName() >> "childField1"
        childFieldMock2.getName() >> "childField2"
        sourceGraphMock.getType(_) >> fooBarTypeMock
        testDefinitionMock.getFieldDefinition() >> testFieldDefinitions
        fooBarTypeMock.getFieldDefinition() >> foobarChildDefinitions

        String fieldSet = "foobar { childField1 childField2 }"

        when:
        fieldSetValidator.validate(sourceGraphMock, testDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        noExceptionThrown()
    }

    def "npe From Null Source Graph When Checking Empty Field Set Type Extension"() {
        given:
        TypeExtensionDefinition typeDefinitionMock = Mock(TypeExtensionDefinition.class)
        String fieldSet = ""

        when:
        fieldSetValidator.validate(null, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(NullPointerException)
    }

    def "npe From Null Type Def When Checking Empty Field Set Type Extension"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph.class)
        String fieldSet = ""

        when:
        fieldSetValidator.validate(sourceGraphMock, (TypeExtensionDefinition) null, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(NullPointerException)
    }

    def "Empty Fields Exception When Checking Empty Field Set With Empty Field Set Type Extension"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph.class)
        TypeExtensionDefinition typeDefinitionMock = Mock(TypeExtensionDefinition.class)
        String fieldSet = ""

        when:
        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(EmptyFieldsArgumentFederationDirective)
    }

    def "Empty Fields Exception When Checking Empty Field Set With Null Field Set Type Extension"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph.class)
        TypeExtensionDefinition typeDefinitionMock = Mock(TypeExtensionDefinition.class)

        when:
        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, null, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(EmptyFieldsArgumentFederationDirective)
    }

    def "Exception From Invalid Field Set When Checking Field Set Type Extension"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph.class)
        TypeExtensionDefinition typeDefinitionMock = Mock(TypeExtensionDefinition.class)
        String fieldSet = "foo bar }"

        when:
        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(InvalidSyntaxException)
    }

    def "invalid Field Reference Exception With Field Set With Invalid Reference Type Extension"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph.class)
        ObjectTypeExtensionDefinition typeDefinitionMock = Mock(ObjectTypeExtensionDefinition.class)
        FieldDefinition fieldDefinition1 = Mock(FieldDefinition.class)
        fieldDefinition1.getName() >> "foo"
        FieldDefinition fieldDefinition2 = Mock(FieldDefinition.class)
        fieldDefinition2.getName() >> "bar"
        FieldDefinition fieldDefinition3 = Mock(FieldDefinition.class)
        fieldDefinition3.getName() >> "foobar"

        EList<FieldDefinition> fieldDefinitionEList = ECollections.asEList(fieldDefinition1, fieldDefinition2, fieldDefinition3)
        typeDefinitionMock.getFieldDefinition() >> fieldDefinitionEList

        String fieldSet = "foo bar badField"

        when:
        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(InvalidFieldSetReferenceException)
    }

    def "invalid Field Reference Exception With Field Set With Invalid Children Type Extension"() {
        XtextGraph sourceGraphMock = Mock(XtextGraph.class)

        ObjectTypeExtensionDefinition testDefinitionMock = Mock(ObjectTypeExtensionDefinition.class)
        FieldDefinition fieldDefinition1 = Mock(FieldDefinition.class)
        FieldDefinition fieldDefinition2 = Mock(FieldDefinition.class)
        FieldDefinition fieldDefinition3 = Mock(FieldDefinition.class)

        ObjectTypeDefinition fooBarTypeMock = Mock(ObjectTypeDefinition.class)
        FieldDefinition childFieldMock1 = Mock(FieldDefinition.class)
        FieldDefinition childFieldMock2 = Mock(FieldDefinition.class)

        EList<FieldDefinition> testFieldDefinitions = ECollections.asEList(fieldDefinition1, fieldDefinition2, fieldDefinition3)
        EList<FieldDefinition> foobarChildDefinitions = ECollections.asEList(childFieldMock1, childFieldMock2)

        fieldDefinition1.getName() >> "foo"
        fieldDefinition2.getName() >> "bar"
        fieldDefinition3.getName() >> "foobar"
        childFieldMock1.getName() >> "childField1"
        childFieldMock2.getName() >> "childField2"
        sourceGraphMock.getType(_) >> fooBarTypeMock
        testDefinitionMock.getFieldDefinition() >> testFieldDefinitions
        fooBarTypeMock.getFieldDefinition() >> foobarChildDefinitions

        String fieldSet = "foobar { childField1 childField2 missingField }"

        when:
        fieldSetValidator.validate(sourceGraphMock, testDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        thrown(InvalidFieldSetReferenceException)
    }

    def "no Exception With Valid Field Set Type Extension"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph.class)
        ObjectTypeExtensionDefinition typeDefinitionMock = Mock(ObjectTypeExtensionDefinition.class)
        FieldDefinition fieldDefinition1 = Mock(FieldDefinition.class)
        fieldDefinition1.getName() >> "foo"
        FieldDefinition fieldDefinition2 = Mock(FieldDefinition.class)
        fieldDefinition2.getName() >> "bar"
        FieldDefinition fieldDefinition3 = Mock(FieldDefinition.class)
        fieldDefinition3.getName() >> "foobar"

        EList<FieldDefinition> fieldDefinitionEList = ECollections.asEList(fieldDefinition1, fieldDefinition2, fieldDefinition3)
        typeDefinitionMock.getFieldDefinition() >> fieldDefinitionEList

        String fieldSet = "foo bar"

        when:
        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        noExceptionThrown()
    }

    def "no Exception With Valid Field Set With Children Type Extension"() {
        given:
        XtextGraph sourceGraphMock = Mock(XtextGraph.class)

        ObjectTypeExtensionDefinition testDefinitionMock = Mock(ObjectTypeExtensionDefinition.class)
        FieldDefinition fieldDefinition1 = Mock(FieldDefinition.class)
        FieldDefinition fieldDefinition2 = Mock(FieldDefinition.class)
        FieldDefinition fieldDefinition3 = Mock(FieldDefinition.class)

        ObjectTypeDefinition fooBarTypeMock = Mock(ObjectTypeDefinition.class)
        FieldDefinition childFieldMock1 = Mock(FieldDefinition.class)
        FieldDefinition childFieldMock2 = Mock(FieldDefinition.class)

        EList<FieldDefinition> testFieldDefinitions = ECollections.asEList(fieldDefinition1, fieldDefinition2, fieldDefinition3)
        EList<FieldDefinition> foobarChildDefinitions = ECollections.asEList(childFieldMock1, childFieldMock2)

        fieldDefinition1.getName() >> "foo"
        fieldDefinition2.getName() >> "bar"
        fieldDefinition3.getName() >> "foobar"
        childFieldMock1.getName() >> "childField1"
        childFieldMock2.getName() >> "childField2"
        sourceGraphMock.getType(_) >> fooBarTypeMock
        testDefinitionMock.getFieldDefinition() >> testFieldDefinitions
        fooBarTypeMock.getFieldDefinition() >> foobarChildDefinitions

        String fieldSet = "foobar { childField1 childField2 }"

        when:
        fieldSetValidator.validate(sourceGraphMock, testDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE)

        then:
        noExceptionThrown()
    }

}
