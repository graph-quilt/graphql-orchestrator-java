package com.intuit.graphql.orchestrator.federationdirectives.keydirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.orchestrator.federation.exceptions.DirectiveMissingRequiredArgumentException;
import com.intuit.graphql.orchestrator.federation.exceptions.IncorrectDirectiveArgumentSizeException;
import com.intuit.graphql.orchestrator.federation.exceptions.InvalidFieldSetReferenceException;
import com.intuit.graphql.orchestrator.federation.keydirective.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.federation.exceptions.EmptyFieldsArgumentFederationDirective;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.intuit.graphql.orchestrator.utils.FederationUtils.FEDERATION_FIELDS_ARGUMENT;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

public class KeyDirectiveValidatorTest {

    KeyDirectiveValidator keyDirectiveValidator = new KeyDirectiveValidator();

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

        Mockito.when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        Mockito.when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);

        List<Argument> argumentList = Arrays.asList(argumentMock, argumentMock);
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList);

        Assert.fail();
    }

    @Test(expected = DirectiveMissingRequiredArgumentException.class)
    public void assertValidatorThrowsExceptionForInvalidArgName() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);

        Mockito.when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        Mockito.when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);

        Mockito.when(argumentMock.getName()).thenReturn("field");

        List<Argument> argumentList = Arrays.asList(argumentMock);
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList);

        Assert.fail();
    }

    @Test(expected = EmptyFieldsArgumentFederationDirective.class)
    public void assertValidatorThrowsExceptionForEmptyFieldsArg() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);
        ValueWithVariable fieldsArgMock = mock(ValueWithVariable.class);

        Mockito.when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        Mockito.when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);

        Mockito.when(fieldsArgMock.getStringValue()).thenReturn(" ");

        Mockito.when(argumentMock.getName()).thenReturn(FEDERATION_FIELDS_ARGUMENT);
        Mockito.when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        List<Argument> argumentList = Arrays.asList(argumentMock);
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList);

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

        EList<FieldDefinition> typeSchemaFields= new BasicEList();
        typeSchemaFields.add(mockedSchemaField1);
        typeSchemaFields.add(mockedSchemaField2);
        typeSchemaFields.add(mockedSchemaField3);
        typeSchemaFields.add(mockedSchemaField4);


        Mockito.when(mockedSchemaField1.getName()).thenReturn("id");
        Mockito.when(mockedSchemaField2.getName()).thenReturn("fooBarRef");
        Mockito.when(mockedSchemaField3.getName()).thenReturn("batId");
        Mockito.when(mockedSchemaField4.getName()).thenReturn("Set");

        Mockito.when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        Mockito.when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);
        Mockito.when(objectTypeDefinitionMock.getFieldDefinition()).thenReturn(typeSchemaFields);

        Mockito.when(fieldsArgMock.getStringValue()).thenReturn("Id");

        Mockito.when(argumentMock.getName()).thenReturn(FEDERATION_FIELDS_ARGUMENT);
        Mockito.when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        List<Argument> argumentList = Arrays.asList(argumentMock);
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList);

        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList);
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

        EList<FieldDefinition> typeSchemaFields= new BasicEList();
        typeSchemaFields.add(mockedSchemaField1);
        typeSchemaFields.add(mockedSchemaField2);
        typeSchemaFields.add(mockedSchemaField3);
        typeSchemaFields.add(mockedSchemaField4);


        Mockito.when(mockedSchemaField1.getName()).thenReturn("id");
        Mockito.when(mockedSchemaField2.getName()).thenReturn("fooBarRef");
        Mockito.when(mockedSchemaField3.getName()).thenReturn("batId");
        Mockito.when(mockedSchemaField4.getName()).thenReturn("Set");

        Mockito.when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        Mockito.when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);
        Mockito.when(objectTypeDefinitionMock.getFieldDefinition()).thenReturn(typeSchemaFields);

        Mockito.when(fieldsArgMock.getStringValue()).thenReturn("id");

        Mockito.when(argumentMock.getName()).thenReturn(FEDERATION_FIELDS_ARGUMENT);
        Mockito.when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        List<Argument> argumentList = Collections.singletonList(argumentMock);
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList);
    }

    @Test(expected = InvalidFieldSetReferenceException.class)
    public void assertValidatorThrowsExceptionForInvalidFieldReferenceMultipleFields() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);
        ValueWithVariable fieldsArgMock = mock(ValueWithVariable.class);

        FieldDefinition mockedSchemaField1 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField2 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField3 = mock(FieldDefinition.class);
        FieldDefinition mockedSchemaField4 = mock(FieldDefinition.class);

        EList<FieldDefinition> typeSchemaFields= new BasicEList();
        typeSchemaFields.add(mockedSchemaField1);
        typeSchemaFields.add(mockedSchemaField2);
        typeSchemaFields.add(mockedSchemaField3);
        typeSchemaFields.add(mockedSchemaField4);


        Mockito.when(mockedSchemaField1.getName()).thenReturn("id");
        Mockito.when(mockedSchemaField2.getName()).thenReturn("fooBarRef");
        Mockito.when(mockedSchemaField3.getName()).thenReturn("batId");
        Mockito.when(mockedSchemaField4.getName()).thenReturn("Set");

        Mockito.when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        Mockito.when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);
        Mockito.when(objectTypeDefinitionMock.getFieldDefinition()).thenReturn(typeSchemaFields);

        Mockito.when(fieldsArgMock.getStringValue()).thenReturn("id");
        Mockito.when(fieldsArgMock.getStringValue()).thenReturn("batid");

        Mockito.when(argumentMock.getName()).thenReturn(FEDERATION_FIELDS_ARGUMENT);
        Mockito.when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        List<Argument> argumentList = Arrays.asList(argumentMock);
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList);

        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList);
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

        EList<FieldDefinition> typeSchemaFields= new BasicEList();
        typeSchemaFields.add(mockedSchemaField1);
        typeSchemaFields.add(mockedSchemaField2);
        typeSchemaFields.add(mockedSchemaField3);
        typeSchemaFields.add(mockedSchemaField4);


        Mockito.when(mockedSchemaField1.getName()).thenReturn("id");
        Mockito.when(mockedSchemaField2.getName()).thenReturn("fooBarRef");
        Mockito.when(mockedSchemaField3.getName()).thenReturn("batId");
        Mockito.when(mockedSchemaField4.getName()).thenReturn("Set");

        Mockito.when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        Mockito.when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);
        Mockito.when(objectTypeDefinitionMock.getFieldDefinition()).thenReturn(typeSchemaFields);

        Mockito.when(fieldsArgMock.getStringValue()).thenReturn("id batId");

        Mockito.when(argumentMock.getName()).thenReturn(FEDERATION_FIELDS_ARGUMENT);
        Mockito.when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        List<Argument> argumentList = Collections.singletonList(argumentMock);
        keyDirectiveValidator.validate(xtextGraphMock,objectTypeDefinitionMock, argumentList);
    }
}
