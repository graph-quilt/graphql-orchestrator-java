package com.intuit.graphql.orchestrator.fieldresolver;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

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
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.Field;
import graphql.language.IntValue;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FieldResolverBatchSelectionSetSupplierLiteralsTest {

    @Mock
    private DataFetchingEnvironment dataFetchingEnvironmentMock;

    @Mock
    private Field dfeFieldMock;

    @Mock
    private ObjectTypeDefinition parentTypeOfFieldWithResolver;

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

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
            new ResolverArgumentDefinition("name", "{ id : \"$petId\" }", objectType)
        ));

        String[] resolverSelectedFields = new String[] {"petById"};

        // WHEN
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext);

        // THEN
        SelectionSet actual = subject.get();

        assertThat(actual).isNotNull();
    }

    @Test
    public void get_argumentTypeisID_LiteralIsString() {
        // GIVEN
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLID.getName());
        targetArgumentType.setNonNull(false);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
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

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
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

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
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

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
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

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
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

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
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

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
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

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
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

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
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

