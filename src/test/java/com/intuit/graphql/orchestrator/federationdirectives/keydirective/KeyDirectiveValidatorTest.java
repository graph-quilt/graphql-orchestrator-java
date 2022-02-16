package com.intuit.graphql.orchestrator.federationdirectives.keydirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.orchestrator.federation.keydirective.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.federation.keydirective.exceptions.EmptyFieldsArgumentKeyDirective;
import com.intuit.graphql.orchestrator.federation.keydirective.exceptions.MultipleArgumentsForKeyDirective;
import com.intuit.graphql.orchestrator.federation.keydirective.exceptions.NoFieldsArgumentForKeyDirective;
import com.intuit.graphql.orchestrator.federation.keydirective.exceptions.InvalidKeyDirectiveFieldReference;
import com.intuit.graphql.orchestrator.federation.keydirective.exceptions.InvalidLocationForKeyDirective;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

public class KeyDirectiveValidatorTest {

    KeyDirectiveValidator keyDirectiveValidator = new KeyDirectiveValidator();

    @Mock
    ObjectTypeDefinition objectTypeDefinitionMock;

    @Mock
    Argument argumentMock;

    @Before
    public void setup() {
        initMocks(this);
    }


    @Test(expected = InvalidLocationForKeyDirective.class)
    public void assertValidatorThrowsExceptionForInvalidLocation() {
        FieldDefinition mockedFieldDefinition =  mock(FieldDefinition.class);
        EClass mockedEClass = mock(EClass.class);


        Mockito.when(mockedEClass.getInstanceClassName()).thenReturn("Field Definition");
        Mockito.when(mockedFieldDefinition.eClass()).thenReturn(mockedEClass);
        Mockito.when(objectTypeDefinitionMock.getName()).thenReturn("Foo Field");
        Mockito.when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedFieldDefinition);

        List<Argument> argumentList = Arrays.asList(argumentMock);
        keyDirectiveValidator.validate(objectTypeDefinitionMock, argumentList);
        Assert.fail();
    }

    @Test(expected = MultipleArgumentsForKeyDirective.class)
    public void assertValidatorThrowsExceptionForMultipleArgs() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);

        Mockito.when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        Mockito.when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);

        List<Argument> argumentList = Arrays.asList(argumentMock, argumentMock);
        keyDirectiveValidator.validate(objectTypeDefinitionMock, argumentList);

        Assert.fail();
    }

    @Test(expected = NoFieldsArgumentForKeyDirective.class)
    public void assertValidatorThrowsExceptionForInvalidArgName() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);

        Mockito.when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        Mockito.when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);

        Mockito.when(argumentMock.getName()).thenReturn("field");

        List<Argument> argumentList = Arrays.asList(argumentMock);
        keyDirectiveValidator.validate(objectTypeDefinitionMock, argumentList);

        Assert.fail();
    }

    @Test(expected = EmptyFieldsArgumentKeyDirective.class)
    public void assertValidatorThrowsExceptionForEmptyFieldsArg() {
        TypeSystemDefinition mockedTypeSystemDefinition =  mock(TypeSystemDefinition.class);
        ValueWithVariable fieldsArgMock = mock(ValueWithVariable.class);

        Mockito.when(objectTypeDefinitionMock.getName()).thenReturn("validType");
        Mockito.when(objectTypeDefinitionMock.eContainer()).thenReturn(mockedTypeSystemDefinition);

        Mockito.when(fieldsArgMock.getStringValue()).thenReturn(" ");

        Mockito.when(argumentMock.getName()).thenReturn("fields");
        Mockito.when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        List<Argument> argumentList = Arrays.asList(argumentMock);
        keyDirectiveValidator.validate(objectTypeDefinitionMock, argumentList);

        Assert.fail();
    }

    @Test(expected = InvalidKeyDirectiveFieldReference.class)
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

        Mockito.when(argumentMock.getName()).thenReturn("fields");
        Mockito.when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        List<Argument> argumentList = Arrays.asList(argumentMock);
        keyDirectiveValidator.validate(objectTypeDefinitionMock, argumentList);

        keyDirectiveValidator.validate(objectTypeDefinitionMock, argumentList);
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

        Mockito.when(argumentMock.getName()).thenReturn("fields");
        Mockito.when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        List<Argument> argumentList = Collections.singletonList(argumentMock);
        keyDirectiveValidator.validate(objectTypeDefinitionMock, argumentList);
    }

    @Test(expected = InvalidKeyDirectiveFieldReference.class)
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

        Mockito.when(argumentMock.getName()).thenReturn("fields");
        Mockito.when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        List<Argument> argumentList = Arrays.asList(argumentMock);
        keyDirectiveValidator.validate(objectTypeDefinitionMock, argumentList);

        keyDirectiveValidator.validate(objectTypeDefinitionMock, argumentList);
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

        Mockito.when(argumentMock.getName()).thenReturn("fields");
        Mockito.when(argumentMock.getValueWithVariable()).thenReturn(fieldsArgMock);

        List<Argument> argumentList = Collections.singletonList(argumentMock);
        keyDirectiveValidator.validate(objectTypeDefinitionMock, argumentList);
    }
}
