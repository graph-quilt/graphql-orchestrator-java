package com.intuit.graphql.orchestrator.fieldresolver;

import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.EnumValueDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InputObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.PrimitiveType;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import graphql.Scalars;
import graphql.language.Argument;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.Field;
import graphql.language.IntValue;
import graphql.language.ObjectValue;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FieldResolverBatchSelectionSetSupplierLiteralsTest {

    @Mock
    private DataFetchingEnvironment dataFetchingEnvironmentMock;

    @Mock
    private Field dfeFieldMock;

    @Mock
    private FieldDefinition fieldDefinitionWithResolver;

    @Mock
    private ResolverDirectiveDefinition resolverDirectiveDefinitionMock;

    private final Map<String, Object> testDFEDataSource = new HashMap<>();

    private FieldResolverContext testFieldResolverContext;

    private final List<DataFetchingEnvironment> dataFetchingEnvironments = new ArrayList<>();

    private FieldResolverBatchSelectionSetSupplier subject;

    @Before
    public void setup() {
        when(dfeFieldMock.getSelectionSet()).thenReturn(null);
        when(dataFetchingEnvironmentMock.getField()).thenReturn(dfeFieldMock);
        when(dataFetchingEnvironmentMock.getSource()).thenReturn(testDFEDataSource);
        dataFetchingEnvironments.add(dataFetchingEnvironmentMock);

        FieldDefinition childFieldDefinition = buildFieldDefinition("childField");
        ObjectTypeDefinition parentTypeOfFieldWithResolver = buildObjectTypeDefinition("ParentType", singletonList(childFieldDefinition));

        testFieldResolverContext = FieldResolverContext.builder()
            .parentTypeDefinition(parentTypeOfFieldWithResolver)
            .fieldDefinition(fieldDefinitionWithResolver)
            .requiresTypeNameInjection(true)
            .serviceNamespace("TESTSVC")
            .resolverDirectiveDefinition(resolverDirectiveDefinitionMock)
            .build();
    }

    @Test
    public void testWithObjectLiteralsArgument() {
        // GIVEN
        testDFEDataSource.put("petId","pet-901");

        InputObjectTypeDefinition petIdType = GraphQLFactoryDelegate.createInputObjectTypeDefinition();
        petIdType.setName("PetId");

        ObjectType objectType = GraphQLFactoryDelegate.createObjectType();
        objectType.setType(petIdType);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(singletonList(
            new ResolverArgumentDefinition("petIdInputObject", "{ id : \"$petId\" }", objectType)
        ));

        testFieldResolverContext = FieldResolverContext.builder()
                .parentTypeDefinition(testFieldResolverContext.getParentTypeDefinition())
                .fieldDefinition(fieldDefinitionWithResolver)
                .requiresTypeNameInjection(true)
                .serviceNamespace("TESTSVC")
                .resolverDirectiveDefinition(resolverDirectiveDefinitionMock)
                .requiredFields(testDFEDataSource.keySet())
                .build();

        String[] resolverSelectedFields = new String[] {"petById"};

        // WHEN
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext);

        // THEN
        SelectionSet actual = subject.get();

        Field actualPetByIdField = (Field)actual.getSelections().get(0);
        Argument actualArgument = actualPetByIdField.getArguments().get(0);
        assertThat(actualArgument.getName()).isEqualTo("petIdInputObject");
        ObjectValue actualArgumentValue = (ObjectValue)actualArgument.getValue();
        assertThat(actualArgumentValue.getObjectFields().get(0).getName()).isEqualTo("id");
        StringValue actualStringValue = (StringValue) actualArgumentValue.getObjectFields().get(0).getValue();
        assertThat(actualStringValue.getValue()).isEqualTo("pet-901");


    }

    @Test
    public void get_argumentTypeisID_LiteralIsString() {
        // GIVEN
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLID.getName());
        targetArgumentType.setNonNull(false);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(singletonList(
            new ResolverArgumentDefinition("argName", "stringArgumentValue", targetArgumentType)));

        String[] resolverSelectedFields = new String[] {"targetField"};

        // WHEN
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext);
        SelectionSet actual = subject.get();

        // THEN
        Field actualField = (Field) actual.getSelections().get(0);
        assertThat(actualField.getName()).isEqualTo("targetField");
        assertThat(actualField.getSelectionSet()).isNull();
        assertThat(actualField.getArguments().get(0).getName()).isEqualTo("argName");

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue();
        assertThat(actualStringValue.getValue()).isEqualTo("stringArgumentValue");
    }

    @Test
    public void get_argumentTypeisID_LiteralIsNumeric() {
        // GIVEN
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLID.getName());
        targetArgumentType.setNonNull(false);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(singletonList(
            new ResolverArgumentDefinition("argName", "123456789", targetArgumentType)));

        String[] resolverSelectedFields = new String[] {"targetField"};

        // WHEN
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext);
        SelectionSet actual = subject.get();

        // THEN
        Field actualField = (Field) actual.getSelections().get(0);
        assertThat(actualField.getName()).isEqualTo("targetField");
        assertThat(actualField.getSelectionSet()).isNull();
        assertThat(actualField.getArguments().get(0).getName()).isEqualTo("argName");

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue();
        assertThat(actualStringValue.getValue()).isEqualTo("123456789");
    }

    @Test
    public void get_argumentTypeisString_LiteralIsString() {
        // GIVEN
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLString.getName());
        targetArgumentType.setNonNull(false);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(singletonList(
            new ResolverArgumentDefinition("argName", "stringArgumentValue", targetArgumentType)));

        String[] resolverSelectedFields = new String[] {"targetField"};

        // WHEN
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext);
        SelectionSet actual = subject.get();

        // THEN
        Field actualField = (Field) actual.getSelections().get(0);
        assertThat(actualField.getName()).isEqualTo("targetField");
        assertThat(actualField.getSelectionSet()).isNull();
        assertThat(actualField.getArguments().get(0).getName()).isEqualTo("argName");

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue();
        assertThat(actualStringValue.getValue()).isEqualTo("stringArgumentValue");
    }

    @Test
    public void get_argumentTypeisNonNullString_LiteralIsString() {
        // GIVEN
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLString.getName());
        targetArgumentType.setNonNull(true);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(singletonList(
            new ResolverArgumentDefinition("argName", "stringArgumentValue", targetArgumentType)));

        String[] resolverSelectedFields = new String[] {"targetField"};

        // WHEN
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext);
        SelectionSet actual = subject.get();

        // THEN
        Field actualField = (Field) actual.getSelections().get(0);
        assertThat(actualField.getName()).isEqualTo("targetField");
        assertThat(actualField.getSelectionSet()).isNull();
        assertThat(actualField.getArguments().get(0).getName()).isEqualTo("argName");

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue();
        assertThat(actualStringValue.getValue()).isEqualTo("stringArgumentValue");
    }

    @Test
    public void get_argumentTypeisString_LiteralIsInt() {
        // GIVEN
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLString.getName());
        targetArgumentType.setNonNull(false);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(singletonList(
            new ResolverArgumentDefinition("argName", "123456789", targetArgumentType)));

        String[] resolverSelectedFields = new String[] {"targetField"};

        // WHEN
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext);
        SelectionSet actual = subject.get();

        // THEN
        Field actualField = (Field) actual.getSelections().get(0);
        assertThat(actualField.getName()).isEqualTo("targetField");
        assertThat(actualField.getSelectionSet()).isNull();
        assertThat(actualField.getArguments().get(0).getName()).isEqualTo("argName");

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue();
        assertThat(actualStringValue.getValue()).isEqualTo("123456789");
    }

    @Test
    public void get_argumentTypeisString_LiteralIsBoolean() {
        // GIVEN
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLString.getName());
        targetArgumentType.setNonNull(false);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(singletonList(
            new ResolverArgumentDefinition("argName", "true", targetArgumentType)));

        String[] resolverSelectedFields = new String[] {"targetField"};

        // WHEN
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext);
        SelectionSet actual = subject.get();

        // THEN
        Field actualField = (Field) actual.getSelections().get(0);
        assertThat(actualField.getName()).isEqualTo("targetField");
        assertThat(actualField.getSelectionSet()).isNull();
        assertThat(actualField.getArguments().get(0).getName()).isEqualTo("argName");

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue();
        assertThat(actualStringValue.getValue()).isEqualTo("true");
    }

    @Test
    public void get_argumentTypeisInt_LiteralIsInt() {
        // GIVEN
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLInt.getName());
        targetArgumentType.setNonNull(false);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(singletonList(
            new ResolverArgumentDefinition("argName", "123456789", targetArgumentType)));

        String[] resolverSelectedFields = new String[] {"targetField"};

        // WHEN
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext);
        SelectionSet actual = subject.get();

        // THEN
        Field actualField = (Field) actual.getSelections().get(0);
        assertThat(actualField.getName()).isEqualTo("targetField");
        assertThat(actualField.getSelectionSet()).isNull();
        assertThat(actualField.getArguments().get(0).getName()).isEqualTo("argName");

        IntValue actualIntValue = (IntValue) actualField.getArguments().get(0).getValue();
        assertThat(actualIntValue.getValue().intValue()).isEqualTo(123456789);
    }

    @Test
    public void get_argumentTypeisBoolean_LiteralIsBoolean() {
        // GIVEN
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLBoolean.getName());
        targetArgumentType.setNonNull(false);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(singletonList(
            new ResolverArgumentDefinition("argName", "true", targetArgumentType)));

        String[] resolverSelectedFields = new String[] {"targetField"};

        // WHEN
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext);
        SelectionSet actual = subject.get();

        // THEN
        Field actualField = (Field) actual.getSelections().get(0);
        assertThat(actualField.getName()).isEqualTo("targetField");
        assertThat(actualField.getSelectionSet()).isNull();
        assertThat(actualField.getArguments().get(0).getName()).isEqualTo("argName");

        BooleanValue actualBooleanValue = (BooleanValue) actualField.getArguments().get(0).getValue();
        assertThat(actualBooleanValue.isValue()).isEqualTo(true);
    }

    @Test
    public void get_argumentTypeisEnum_LiteralIsAnValidEnumValue() {
        // GIVEN
        EnumValueDefinition enumValue1 = GraphQLFactoryDelegate.createEnumValueDefinition();
        enumValue1.setEnumValue("ENUM_VALUE_1");

        EnumTypeDefinition targetArgumentType = GraphQLFactoryDelegate.createEnumTypeDefinition();
        targetArgumentType.setName("TestEnumType");
        targetArgumentType.getEnumValueDefinition().add(enumValue1);

        ObjectType targetArgNamedType = GraphQLFactoryDelegate.createObjectType();
        targetArgNamedType.setType(targetArgumentType);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(singletonList(
            new ResolverArgumentDefinition("argName", "ENUM_VALUE_1", targetArgNamedType)));

        String[] resolverSelectedFields = new String[] {"targetField"};

        // WHEN
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext);
        SelectionSet actual = subject.get();

        // THEN
        Field actualField = (Field) actual.getSelections().get(0);
        assertThat(actualField.getName()).isEqualTo("targetField");
        assertThat(actualField.getSelectionSet()).isNull();
        assertThat(actualField.getArguments().get(0).getName()).isEqualTo("argName");

        EnumValue actualEnumValue = (EnumValue) actualField.getArguments().get(0).getValue();
        assertThat(actualEnumValue.getName()).isEqualTo("ENUM_VALUE_1");
    }

}

