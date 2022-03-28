package com.intuit.graphql.orchestrator.fieldresolver;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.PrimitiveType;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import graphql.Scalars;
import graphql.language.BooleanValue;
import graphql.language.Field;
import graphql.language.IntValue;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
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
public class FieldResolverBatchSelectionSetSupplierPrimitiveArgumentTest {

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
    public void get_argumentTypeisString_sourceTypeIsIDWithNumericValue() {
        // GIVEN
        testDFEDataSource.put("id", 123456789);
        when(dataFetchingEnvironmentMock.getParentType())
            .thenReturn(GraphQLObjectType.newObject()
                .name("QueryFieldType")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("id")
                    .type(Scalars.GraphQLID)
                    .build())
                .build());

        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLString.getName());
        targetArgumentType.setNonNull(false);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
            new ResolverArgumentDefinition("argName", "$id", targetArgumentType)));

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
    public void get_argumentTypeisNonNullString_sourceTypeIsIDWithNumericValue() {
        // GIVEN
        testDFEDataSource.put("id", 123456789);
        when(dataFetchingEnvironmentMock.getParentType())
            .thenReturn(GraphQLObjectType.newObject()
                .name("QueryFieldType")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("id")
                    .type(Scalars.GraphQLID)
                    .build())
                .build());

        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLString.getName());
        targetArgumentType.setNonNull(true);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
            new ResolverArgumentDefinition("argName", "$id", targetArgumentType)));

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
    public void get_argumentTypeisString_sourceTypeIsIDWithStringValue() {
        // GIVEN
        testDFEDataSource.put("id", "ID-STRING_VALUE");
        when(dataFetchingEnvironmentMock.getParentType())
            .thenReturn(GraphQLObjectType.newObject()
                .name("QueryFieldType")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("id")
                    .type(Scalars.GraphQLID)
                    .build())
                .build());

        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLString.getName());
        targetArgumentType.setNonNull(false);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
            new ResolverArgumentDefinition("argName", "$id", targetArgumentType)));

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
        assertThat(actualStringValue.getValue()).isEqualTo("ID-STRING_VALUE");
    }

    @Test
    public void get_argumentTypeisString_sourceTypeIsInt() {
        // GIVEN
        testDFEDataSource.put("intField", 123456789);
        when(dataFetchingEnvironmentMock.getParentType())
            .thenReturn(GraphQLObjectType.newObject()
                .name("QueryFieldType")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("intField")
                    .type(Scalars.GraphQLInt)
                    .build())
                .build());

        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLString.getName());
        targetArgumentType.setNonNull(false);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
            new ResolverArgumentDefinition("argName", "$intField", targetArgumentType)));

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
    public void get_argumentTypeisString_sourceTypeIsBoolean() {
        // GIVEN
        testDFEDataSource.put("boolField", true);
        when(dataFetchingEnvironmentMock.getParentType())
            .thenReturn(GraphQLObjectType.newObject()
                .name("QueryFieldType")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("boolField")
                    .type(Scalars.GraphQLBoolean)
                    .build())
                .build());

        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType();
        targetArgumentType.setType(Scalars.GraphQLString.getName());
        targetArgumentType.setNonNull(false);

        when(resolverDirectiveDefinitionMock.getArguments()).thenReturn(Collections.singletonList(
            new ResolverArgumentDefinition("argName", "$boolField", targetArgumentType)));

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

        BooleanValue booleanValue = (BooleanValue) actualField.getArguments().get(0).getValue();
        assertThat(booleanValue.isValue()).isEqualTo(true);
    }

}

