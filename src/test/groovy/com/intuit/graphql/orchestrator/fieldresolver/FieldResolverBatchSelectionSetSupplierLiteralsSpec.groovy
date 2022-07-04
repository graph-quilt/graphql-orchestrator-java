package com.intuit.graphql.orchestrator.fieldresolver

import com.intuit.graphql.graphQL.*
import com.intuit.graphql.orchestrator.metadata.RenamedMetadata
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition
import com.intuit.graphql.orchestrator.schema.ServiceMetadata
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate
import graphql.Scalars
import graphql.language.*
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition
import static java.util.Collections.singletonList

class FieldResolverBatchSelectionSetSupplierLiteralsSpec extends Specification {

    private DataFetchingEnvironment dataFetchingEnvironmentMock

    private Field dfeFieldMock

    private FieldDefinition fieldDefinitionWithResolver

    private ResolverDirectiveDefinition resolverDirectiveDefinitionMock

    private ServiceMetadata serviceMetadataMock

    private GraphQLSchema graphQLSchemaMock

    private RenamedMetadata renamedMetadataMock

    private final Map<String, Object> testDFEDataSource = new HashMap<>()

    private FieldResolverContext testFieldResolverContext

    private final List<DataFetchingEnvironment> dataFetchingEnvironments = new ArrayList<>()

    private FieldResolverBatchSelectionSetSupplier subject

    void setup() {
        dataFetchingEnvironmentMock = Mock(DataFetchingEnvironment.class)
        dfeFieldMock = Mock(Field.class)
        fieldDefinitionWithResolver = Mock(FieldDefinition.class)
        resolverDirectiveDefinitionMock = Mock(ResolverDirectiveDefinition.class)
        serviceMetadataMock = Mock(ServiceMetadata.class)
        graphQLSchemaMock = Mock(GraphQLSchema.class)
        renamedMetadataMock = Mock(RenamedMetadata.class)

        renamedMetadataMock.getOriginalFieldNamesByRenamedName() >> Collections.emptyMap()
        serviceMetadataMock.getRenamedMetadata() >> renamedMetadataMock

        graphQLSchemaMock.getQueryType() >> GraphQLObjectType.newObject()
                .name("Query")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("targetField")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("petById")
                        .type(GraphQLObjectType.newObject().name("Pet").build())
                        .build())
                .build()

        dfeFieldMock.getSelectionSet() >> null
        dataFetchingEnvironmentMock.getField() >> dfeFieldMock
        dataFetchingEnvironmentMock.getSource() >> testDFEDataSource
        dataFetchingEnvironmentMock.getGraphQLSchema() >> graphQLSchemaMock
        dataFetchingEnvironmentMock.getFragmentsByName() >> Collections.emptyMap()
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

    void testWithObjectLiteralsArgument() {
        given:
        testDFEDataSource.put("petId", "pet-901")

        InputObjectTypeDefinition petIdType = GraphQLFactoryDelegate.createInputObjectTypeDefinition()
        petIdType.setName("PetId")

        ObjectType objectType = GraphQLFactoryDelegate.createObjectType()
        objectType.setType(petIdType)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("petIdInputObject", '{ id : "$petId" }', objectType)
        )

        testFieldResolverContext = FieldResolverContext.builder()
                .parentTypeDefinition(testFieldResolverContext.getParentTypeDefinition())
                .fieldDefinition(fieldDefinitionWithResolver)
                .requiresTypeNameInjection(true)
                .serviceNamespace("TESTSVC")
                .resolverDirectiveDefinition(resolverDirectiveDefinitionMock)
                .requiredFields(testDFEDataSource.keySet())
                .build()

        String[] resolverSelectedFields = [ "petById" ]

        when:

        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext, serviceMetadataMock)


        then:
        graphql.language.SelectionSet actual = subject.get()

        Field actualPetByIdField = (Field)actual.getSelections().get(0)
        graphql.language.Argument actualArgument = actualPetByIdField.getArguments().get(0)
        actualArgument.getName() == "petIdInputObject"

        graphql.language.ObjectValue actualArgumentValue = (graphql.language.ObjectValue)actualArgument.getValue()
        actualArgumentValue.getObjectFields().get(0).getName() == "id"

        StringValue actualStringValue = (StringValue) actualArgumentValue.getObjectFields().get(0).getValue()
        actualStringValue.getValue() == "pet-901"
    }

    void get_argumentTypeisID_LiteralIsString() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLID.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "stringArgumentValue", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext, serviceMetadataMock)
        graphql.language.SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()
        actualStringValue.getValue() == "stringArgumentValue"
    }

    void get_argumentTypeisID_LiteralIsNumeric() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLID.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "123456789", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext, serviceMetadataMock)
        graphql.language.SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()
        actualStringValue.getValue() == "123456789"
    }

    void get_argumentTypeisString_LiteralIsString() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "stringArgumentValue", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext, serviceMetadataMock)
        graphql.language.SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()
        actualStringValue.getValue() == "stringArgumentValue"
    }

    void get_argumentTypeisNonNullString_LiteralIsString() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(true)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "stringArgumentValue", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext, serviceMetadataMock)
        graphql.language.SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()
        actualStringValue.getValue() == "stringArgumentValue"
    }

    void get_argumentTypeisString_LiteralIsInt() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "123456789", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext, serviceMetadataMock)
        graphql.language.SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()
        actualStringValue.getValue() == "123456789"
    }

    void get_argumentTypeisString_LiteralIsBoolean() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "true", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext, serviceMetadataMock)
        graphql.language.SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()
        actualStringValue.getValue() == "true"
    }

    void get_argumentTypeisInt_LiteralIsInt() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLInt.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "123456789", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext, serviceMetadataMock)
        graphql.language.SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        IntValue actualIntValue = (IntValue) actualField.getArguments().get(0).getValue()
        actualIntValue.getValue().intValue() == 123456789
    }

    void get_argumentTypeisBoolean_LiteralIsBoolean() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLBoolean.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "true", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext, serviceMetadataMock)
        graphql.language.SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        BooleanValue actualBooleanValue = (BooleanValue) actualField.getArguments().get(0).getValue()
        actualBooleanValue.isValue()
    }

    void get_argumentTypeisEnum_LiteralIsAnValidEnumValue() {
        given:
        EnumValueDefinition enumValue1 = GraphQLFactoryDelegate.createEnumValueDefinition()
        enumValue1.setEnumValue("ENUM_VALUE_1")

        EnumTypeDefinition targetArgumentType = GraphQLFactoryDelegate.createEnumTypeDefinition()
        targetArgumentType.setName("TestEnumType")
        targetArgumentType.getEnumValueDefinition().add(enumValue1)

        ObjectType targetArgNamedType = GraphQLFactoryDelegate.createObjectType()
        targetArgNamedType.setType(targetArgumentType)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "ENUM_VALUE_1", targetArgNamedType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext, serviceMetadataMock)
        graphql.language.SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        EnumValue actualEnumValue = (EnumValue) actualField.getArguments().get(0).getValue()
        actualEnumValue.getName() == "ENUM_VALUE_1"
    }

}
