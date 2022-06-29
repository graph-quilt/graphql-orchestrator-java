package com.intuit.graphql.orchestrator.fieldresolver

import com.intuit.graphql.graphQL.FieldDefinition
import com.intuit.graphql.graphQL.ObjectTypeDefinition
import com.intuit.graphql.graphQL.PrimitiveType
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate

import graphql.Scalars
import graphql.language.*
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

import spock.lang.Specification

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition
import static java.util.Collections.singletonList

class FieldResolverBatchSelectionSetSupplierPrimitiveArgumentSpec extends Specification {

    private DataFetchingEnvironment dataFetchingEnvironmentMock

    private Field dfeFieldMock

    private FieldDefinition fieldDefinitionWithResolver

    private ResolverDirectiveDefinition resolverDirectiveDefinitionMock

    private final Map<String, Object> testDFEDataSource = new HashMap<>()

    private FieldResolverContext testFieldResolverContext

    private final List<DataFetchingEnvironment> dataFetchingEnvironments = new ArrayList<>()

    private FieldResolverBatchSelectionSetSupplier subject

    void setup() {
        dataFetchingEnvironmentMock = Mock(DataFetchingEnvironment.class)
        dfeFieldMock = Mock(Field.class)
        fieldDefinitionWithResolver = Mock(FieldDefinition.class)
        resolverDirectiveDefinitionMock = Mock(ResolverDirectiveDefinition.class)

        dfeFieldMock.getSelectionSet() >> null
        dataFetchingEnvironmentMock.getField() >> dfeFieldMock
        dataFetchingEnvironmentMock.getSource() >> testDFEDataSource
        dataFetchingEnvironments.add(dataFetchingEnvironmentMock)

        FieldDefinition childFieldDefinition = buildFieldDefinition("childField")
        ObjectTypeDefinition parentTypeOfFieldWithResolver = buildObjectTypeDefinition("ParentType", singletonList(childFieldDefinition))

        testFieldResolverContext = FieldResolverContext.builder()
            .parentTypeDefinition(parentTypeOfFieldWithResolver)
            .fieldDefinition(fieldDefinitionWithResolver)
            .requiresTypeNameInjection(true)
            .serviceNamespace("TESTSVC")
            .resolverDirectiveDefinition(resolverDirectiveDefinitionMock)
            .build()
    }

    void get_argumentTypeisString_sourceTypeIsIDWithNumericValue() {
        given:
        testDFEDataSource.put("id", 123456789)
        dataFetchingEnvironmentMock.getParentType() >> GraphQLObjectType.newObject()
                .name("QueryFieldType")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("id")
                    .type(Scalars.GraphQLID)
                    .build())
                .build()

        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> Collections.singletonList(
            new ResolverArgumentDefinition("argName", '$id', targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()
        Field actualField = (Field) actual.getSelections().get(0)
        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()

        then:
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        actualStringValue.getValue() == "123456789"
    }

    void get_argumentTypeisNonNullString_sourceTypeIsIDWithNumericValue() {
        given:
        testDFEDataSource.put("id", 123456789)
        dataFetchingEnvironmentMock.getParentType() >> GraphQLObjectType.newObject()
                .name("QueryFieldType")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("id")
                    .type(Scalars.GraphQLID)
                    .build())
                .build()

        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(true)

        resolverDirectiveDefinitionMock.getArguments() >> Collections.singletonList(
            new ResolverArgumentDefinition("argName", '$id', targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()
        Field actualField = (Field) actual.getSelections().get(0)
        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()

        then:
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        actualStringValue.getValue() == "123456789"
    }

    void get_argumentTypeisString_sourceTypeIsIDWithStringValue() {
        given:
        testDFEDataSource.put("id", "ID-STRING_VALUE")
        dataFetchingEnvironmentMock.getParentType() >> GraphQLObjectType.newObject()
                .name("QueryFieldType")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("id")
                    .type(Scalars.GraphQLID)
                    .build())
                .build()

        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> Collections.singletonList(
            new ResolverArgumentDefinition("argName", '$id', targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()
        Field actualField = (Field) actual.getSelections().get(0)
        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()

        then:
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        actualStringValue.getValue() == "ID-STRING_VALUE"
    }

    void get_argumentTypeisString_sourceTypeIsInt() {
        given:
        testDFEDataSource.put("intField", 123456789)
        dataFetchingEnvironmentMock.getParentType() >> GraphQLObjectType.newObject()
                .name("QueryFieldType")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("intField")
                    .type(Scalars.GraphQLInt)
                    .build())
                .build()

        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> Collections.singletonList(
            new ResolverArgumentDefinition("argName", '$intField', targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()
        Field actualField = (Field) actual.getSelections().get(0)
        IntValue actualIntValue = (IntValue) actualField.getArguments().get(0).getValue()

        then:
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        actualIntValue.getValue().intValue() == 123456789
    }

    void get_argumentTypeisString_sourceTypeIsBoolean() {
        given:
        testDFEDataSource.put("boolField", true)
        dataFetchingEnvironmentMock.getParentType() >> GraphQLObjectType.newObject()
                .name("QueryFieldType")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("boolField")
                    .type(Scalars.GraphQLBoolean)
                    .build())
                .build()

        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> Collections.singletonList(
            new ResolverArgumentDefinition("argName", '$boolField', targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()
        Field actualField = (Field) actual.getSelections().get(0)
        BooleanValue booleanValue = (BooleanValue) actualField.getArguments().get(0).getValue()

        then:
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        booleanValue.isValue()
    }

}
