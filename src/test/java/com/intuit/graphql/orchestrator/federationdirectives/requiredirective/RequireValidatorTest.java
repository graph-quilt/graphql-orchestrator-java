package com.intuit.graphql.orchestrator.federationdirectives.requiredirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.orchestrator.federation.exceptions.DirectiveMissingRequiredArgumentException;
import com.intuit.graphql.orchestrator.federation.exceptions.EmptyFieldsArgumentFederationDirective;
import com.intuit.graphql.orchestrator.federation.exceptions.IncorrectDirectiveArgumentSizeException;
import com.intuit.graphql.orchestrator.federation.exceptions.InvalidFieldSetReferenceException;
import com.intuit.graphql.orchestrator.federation.requiresdirective.RequireValidator;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class RequireValidatorTest {
    RequireValidator requireValidator = new RequireValidator();

    @Mock
    Directive directiveMock;

    @Mock
    ObjectTypeDefinition objectTypeDefinitionMock;

    @Mock
    Argument argumentMock;

    @Mock
    XtextGraph xtextGraphMock;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test(expected = IncorrectDirectiveArgumentSizeException.class)
    public void assertValidatorThrowsExceptionForMultipleArgs() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);

        when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);

        EList<Argument> argumentList = ECollections.asEList(argumentMock, argumentMock);
        when(directiveMock.getArguments()).thenReturn(argumentList);

        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock);

        Assert.fail();
    }

    @Test(expected = DirectiveMissingRequiredArgumentException.class)
    public void assertValidatorThrowsExceptionForInvalidArgName() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);

        when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);

        when(argumentMock.getName()).thenReturn("field");

        EList<Argument> argumentList = ECollections.asEList(argumentMock);
        when(directiveMock.getArguments()).thenReturn(argumentList);


        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock);

        Assert.fail();
    }

    @Test(expected = EmptyFieldsArgumentFederationDirective.class)
    public void assertValidatorThrowsExceptionForEmptyFieldsArg() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);
        ValueWithVariable fieldsArgMock = mock(ValueWithVariable.class);

        when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);

        when(fieldsArgMock.getStringValue()).thenReturn(" ");

        when(argumentMock.getName()).thenReturn("fields");
        when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        EList<Argument> argumentList = ECollections.asEList(argumentMock);
        when(directiveMock.getArguments()).thenReturn(argumentList);

        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock);

        Assert.fail();
    }

    @Test(expected = InvalidFieldSetReferenceException.class)
    public void assertValidatorThrowsExceptionForInvalidFieldReference() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);
        ValueWithVariable fieldsArgMock = mock(ValueWithVariable.class);

        FieldDefinition mockedSchemaField1 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField2 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField3 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField4 = mock(FieldDefinition.class);

        EList<FieldDefinition> typeSchemaFields= ECollections.asEList(mockedSchemaField1,mockedSchemaField2,
                mockedSchemaField3, mockedSchemaField4);

        when(mockedSchemaField1.getName()).thenReturn("id");
        when(mockedSchemaField2.getName()).thenReturn("fooBarRef");
        when(mockedSchemaField3.getName()).thenReturn("batId");
        when(mockedSchemaField4.getName()).thenReturn("Set");

        when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);
        when(objectTypeDefinitionMock.getFieldDefinition()).thenReturn(typeSchemaFields);

        when(fieldsArgMock.getStringValue()).thenReturn("Id");

        when(argumentMock.getName()).thenReturn("fields");
        when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        EList<Argument> argumentList = ECollections.asEList(argumentMock);
        when(directiveMock.getArguments()).thenReturn(argumentList);

        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock);
        Assert.fail();
    }

    @Test
    public void assertValidatorNoExceptionForEntityWithSingleField() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);
        ValueWithVariable fieldsArgMock = mock(ValueWithVariable.class);

        FieldDefinition mockedSchemaField1 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField2 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField3 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField4 = mock(FieldDefinition.class);

        EList<FieldDefinition> typeSchemaFields= ECollections.asEList(mockedSchemaField1,mockedSchemaField2,
                mockedSchemaField3, mockedSchemaField4);

        when(mockedSchemaField1.getName()).thenReturn("id");
        when(mockedSchemaField2.getName()).thenReturn("fooBarRef");
        when(mockedSchemaField3.getName()).thenReturn("batId");
        when(mockedSchemaField4.getName()).thenReturn("Set");

        when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);
        when(objectTypeDefinitionMock.getFieldDefinition()).thenReturn(typeSchemaFields);

        when(fieldsArgMock.getStringValue()).thenReturn("id");

        when(argumentMock.getName()).thenReturn("fields");
        when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        EList<Argument> argumentList = ECollections.asEList(argumentMock);
        when(directiveMock.getArguments()).thenReturn(argumentList);
        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock);
    }

    @Test(expected = InvalidFieldSetReferenceException.class)
    public void assertValidatorThrowsExceptionForInvalidFieldReferenceMultipleFields() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);
        ValueWithVariable fieldsArgMock = mock(ValueWithVariable.class);

        FieldDefinition mockedSchemaField1 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField2 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField3 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField4 = mock(FieldDefinition.class);

        EList<FieldDefinition> typeSchemaFields= ECollections.asEList(mockedSchemaField1,mockedSchemaField2,
                mockedSchemaField3, mockedSchemaField4);

        when(mockedSchemaField1.getName()).thenReturn("id");
        when(mockedSchemaField2.getName()).thenReturn("fooBarRef");
        when(mockedSchemaField3.getName()).thenReturn("batId");
        when(mockedSchemaField4.getName()).thenReturn("Set");

        when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);
        when(objectTypeDefinitionMock.getFieldDefinition()).thenReturn(typeSchemaFields);

        when(fieldsArgMock.getStringValue()).thenReturn("id");
        when(fieldsArgMock.getStringValue()).thenReturn("batid");

        when(argumentMock.getName()).thenReturn("fields");
        when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        EList<Argument> argumentList = ECollections.asEList(argumentMock);
        when(directiveMock.getArguments()).thenReturn(argumentList);

        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock);
        Assert.fail();
    }

    @Test
    public void assertValidatorNoExceptionForEntityWithMultipleFields() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);
        ValueWithVariable fieldsArgMock = mock(ValueWithVariable.class);

        FieldDefinition mockedSchemaField1 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField2 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField3 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField4 = mock(FieldDefinition.class);

        EList<FieldDefinition> typeSchemaFields= ECollections.asEList(mockedSchemaField1,mockedSchemaField2,
                mockedSchemaField3, mockedSchemaField4);

        when(mockedSchemaField1.getName()).thenReturn("id");
        when(mockedSchemaField2.getName()).thenReturn("fooBarRef");
        when(mockedSchemaField3.getName()).thenReturn("batId");
        when(mockedSchemaField4.getName()).thenReturn("Set");

        when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);
        when(objectTypeDefinitionMock.getFieldDefinition()).thenReturn(typeSchemaFields);

        when(fieldsArgMock.getStringValue()).thenReturn("id batId");

        when(argumentMock.getName()).thenReturn("fields");
        when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        EList<Argument> argumentList = ECollections.asEList(argumentMock);
        when(directiveMock.getArguments()).thenReturn(argumentList);

        requireValidator.validate(xtextGraphMock,objectTypeDefinitionMock, directiveMock);
    }
}
