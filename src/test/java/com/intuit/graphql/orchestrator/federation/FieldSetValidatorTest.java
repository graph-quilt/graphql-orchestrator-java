package com.intuit.graphql.orchestrator.federation;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.exceptions.EmptyFieldsArgumentFederationDirective;
import com.intuit.graphql.orchestrator.federation.exceptions.InvalidFieldSetReferenceException;
import com.intuit.graphql.orchestrator.federation.validators.FieldSetValidator;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.parser.InvalidSyntaxException;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.junit.Test;
import org.mockito.Mockito;

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class FieldSetValidatorTest {
    FieldSetValidator fieldSetValidator = new FieldSetValidator();


    @Test(expected = NullPointerException.class)
    public void npeFromNullSourceGraphWhenCheckingEmptyFieldSet() {
        TypeDefinition typeDefinitionMock = Mockito.mock(TypeDefinition.class);
        String fieldSet = "";

        fieldSetValidator.validate(null, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE);
    }

    @Test(expected = NullPointerException.class)
    public void npeFromNullTypeDefWhenCheckingEmptyFieldSet() {
        XtextGraph sourceGraphMock = Mockito.mock(XtextGraph.class);
        String fieldSet = "";

        fieldSetValidator.validate(sourceGraphMock, null, fieldSet, FEDERATION_KEY_DIRECTIVE);
    }

    @Test(expected = EmptyFieldsArgumentFederationDirective.class)
    public void EmptyFieldsExceptionWhenCheckingEmptyFieldSetWithEmptyFieldSet() {
        XtextGraph sourceGraphMock = Mockito.mock(XtextGraph.class);
        TypeDefinition typeDefinitionMock = Mockito.mock(TypeDefinition.class);
        String fieldSet = "";

        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE);
    }

    @Test(expected = EmptyFieldsArgumentFederationDirective.class)
    public void EmptyFieldsExceptionWhenCheckingEmptyFieldSetWithNullFieldSet() {
        XtextGraph sourceGraphMock = Mockito.mock(XtextGraph.class);
        TypeDefinition typeDefinitionMock = Mockito.mock(TypeDefinition.class);

        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, null, FEDERATION_KEY_DIRECTIVE);
    }

    @Test(expected = InvalidSyntaxException.class)
    public void ExceptionFromInvalidFieldSetWhenCheckingFieldSet() {
        XtextGraph sourceGraphMock = Mockito.mock(XtextGraph.class);
        TypeDefinition typeDefinitionMock = Mockito.mock(TypeDefinition.class);
        String fieldSet = "foo bar }";

        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE);
    }

    @Test(expected = InvalidFieldSetReferenceException.class)
    public void invalidFieldReferenceExceptionWithFieldSetWithInvalidReference() {
        XtextGraph sourceGraphMock = Mockito.mock(XtextGraph.class);
        ObjectTypeDefinition typeDefinitionMock = Mockito.mock(ObjectTypeDefinition.class);
        FieldDefinition fieldDefinition1 = Mockito.mock(FieldDefinition.class);
        when(fieldDefinition1.getName()).thenReturn("foo");
        FieldDefinition fieldDefinition2 = Mockito.mock(FieldDefinition.class);
        when(fieldDefinition2.getName()).thenReturn("bar");
        FieldDefinition fieldDefinition3 = Mockito.mock(FieldDefinition.class);
        when(fieldDefinition3.getName()).thenReturn("foobar");

        EList<FieldDefinition> fieldDefinitionEList = ECollections.asEList(fieldDefinition1, fieldDefinition2, fieldDefinition3);
        when(typeDefinitionMock.getFieldDefinition()).thenReturn(fieldDefinitionEList);

        String fieldSet = "foo bar badField";

        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE);
    }

    @Test(expected = InvalidFieldSetReferenceException.class)
    public void invalidFieldReferenceExceptionWithFieldSetWithInvalidChildren() {
        XtextGraph sourceGraphMock = Mockito.mock(XtextGraph.class);

        ObjectTypeDefinition testDefinitionMock = Mockito.mock(ObjectTypeDefinition.class);
        FieldDefinition fieldDefinition1 = Mockito.mock(FieldDefinition.class);
        FieldDefinition fieldDefinition2 = Mockito.mock(FieldDefinition.class);
        FieldDefinition fieldDefinition3 = Mockito.mock(FieldDefinition.class);

        ObjectTypeDefinition fooBarTypeMock = Mockito.mock(ObjectTypeDefinition.class);
        FieldDefinition childFieldMock1 = Mockito.mock(FieldDefinition.class);
        FieldDefinition childFieldMock2 = Mockito.mock(FieldDefinition.class);

        EList<FieldDefinition> testFieldDefinitions = ECollections.asEList(fieldDefinition1, fieldDefinition2, fieldDefinition3);
        EList<FieldDefinition> foobarChildDefinitions = ECollections.asEList(childFieldMock1, childFieldMock2);

        when(fieldDefinition1.getName()).thenReturn("foo");
        when(fieldDefinition2.getName()).thenReturn("bar");
        when(fieldDefinition3.getName()).thenReturn("foobar");
        when(childFieldMock1.getName()).thenReturn("childField1");
        when(childFieldMock2.getName()).thenReturn("childField2");
        when(sourceGraphMock.getType((NamedType) any())).thenReturn(fooBarTypeMock);
        when(testDefinitionMock.getFieldDefinition()).thenReturn(testFieldDefinitions);
        when(fooBarTypeMock.getFieldDefinition()).thenReturn(foobarChildDefinitions);


        String fieldSet = "foobar { childField1 childField2 missingField}";

        fieldSetValidator.validate(sourceGraphMock, testDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE);
    }

    @Test
    public void noExceptionWithValidFieldSet() {
        XtextGraph sourceGraphMock = Mockito.mock(XtextGraph.class);
        ObjectTypeDefinition typeDefinitionMock = Mockito.mock(ObjectTypeDefinition.class);
        FieldDefinition fieldDefinition1 = Mockito.mock(FieldDefinition.class);
        when(fieldDefinition1.getName()).thenReturn("foo");
        FieldDefinition fieldDefinition2 = Mockito.mock(FieldDefinition.class);
        when(fieldDefinition2.getName()).thenReturn("bar");
        FieldDefinition fieldDefinition3 = Mockito.mock(FieldDefinition.class);
        when(fieldDefinition3.getName()).thenReturn("foobar");

        EList<FieldDefinition> fieldDefinitionEList = ECollections.asEList(fieldDefinition1, fieldDefinition2, fieldDefinition3);
        when(typeDefinitionMock.getFieldDefinition()).thenReturn(fieldDefinitionEList);

        String fieldSet = "foo bar";

        fieldSetValidator.validate(sourceGraphMock, typeDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE);
    }

    @Test
    public void noExceptionWithValidFieldSetWithChildren() {
        XtextGraph sourceGraphMock = Mockito.mock(XtextGraph.class);

        ObjectTypeDefinition testDefinitionMock = Mockito.mock(ObjectTypeDefinition.class);
        FieldDefinition fieldDefinition1 = Mockito.mock(FieldDefinition.class);
        FieldDefinition fieldDefinition2 = Mockito.mock(FieldDefinition.class);
        FieldDefinition fieldDefinition3 = Mockito.mock(FieldDefinition.class);

        ObjectTypeDefinition fooBarTypeMock = Mockito.mock(ObjectTypeDefinition.class);
        FieldDefinition childFieldMock1 = Mockito.mock(FieldDefinition.class);
        FieldDefinition childFieldMock2 = Mockito.mock(FieldDefinition.class);

        EList<FieldDefinition> testFieldDefinitions = ECollections.asEList(fieldDefinition1, fieldDefinition2, fieldDefinition3);
        EList<FieldDefinition> foobarChildDefinitions = ECollections.asEList(childFieldMock1, childFieldMock2);

        when(fieldDefinition1.getName()).thenReturn("foo");
        when(fieldDefinition2.getName()).thenReturn("bar");
        when(fieldDefinition3.getName()).thenReturn("foobar");
        when(childFieldMock1.getName()).thenReturn("childField1");
        when(childFieldMock2.getName()).thenReturn("childField2");
        when(sourceGraphMock.getType((NamedType) any())).thenReturn(fooBarTypeMock);
        when(testDefinitionMock.getFieldDefinition()).thenReturn(testFieldDefinitions);
        when(fooBarTypeMock.getFieldDefinition()).thenReturn(foobarChildDefinitions);


        String fieldSet = "foobar { childField1 childField2 }";

        fieldSetValidator.validate(sourceGraphMock, testDefinitionMock, fieldSet, FEDERATION_KEY_DIRECTIVE);
    }

}
