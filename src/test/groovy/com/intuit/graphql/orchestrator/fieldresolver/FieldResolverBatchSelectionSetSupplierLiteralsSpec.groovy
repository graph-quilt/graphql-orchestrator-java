package com.intuit.graphql.orchestrator.fieldresolver

import com.intuit.graphql.graphQL.*
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate
import graphql.Scalars
import graphql.language.Argument
import graphql.language.ObjectValue
import graphql.language.SelectionSet
import graphql.language.*
import graphql.schema.DataFetchingEnvironment
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition
import static java.util.Collections.singletonList

class FieldResolverBatchSelectionSetSupplierLiteralsSpec extends Specification {

    private DataFetchingEnvironment dataFetchingEnvironmentMock

    private Field dfeFieldMock

    private FieldDefinition fieldDefinitionWithResolver

    private ResolverDirectiveDefinition resolverDirectiveDefinitionMock

    private final Map<String, Object> testDFEDataSource = new HashMap<>()

    private FieldResolverContext testFieldResolverContext

    private final List<DataFetchingEnvironment> dataFetchingEnvironments = new ArrayList<>()

    private FieldResolverBatchSelectionSetSupplier subject

    def setup() {
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

    def "test With Object Literals Argument"() {
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
            testFieldResolverContext)

        then:
        SelectionSet actual = subject.get()

        Field actualPetByIdField = (Field)actual.getSelections().get(0)
        Argument actualArgument = actualPetByIdField.getArguments().get(0)
        actualArgument.getName() == "petIdInputObject"

        ObjectValue actualArgumentValue = (ObjectValue)actualArgument.getValue()
        actualArgumentValue.getObjectFields().get(0).getName() == "id"

        StringValue actualStringValue = (StringValue) actualArgumentValue.getObjectFields().get(0).getValue()
        actualStringValue.getValue() == "pet-901"
    }

    def "get argument Type is ID, Literal Is String"() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLID.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "stringArgumentValue", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()
        actualStringValue.getValue() == "stringArgumentValue"
    }

    def "get argument Type is ID, Literal Is Numeric"() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLID.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "123456789", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()
        actualStringValue.getValue() == "123456789"
    }

    def "get argument Type is String, Literal Is String"() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "stringArgumentValue", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()
        actualStringValue.getValue() == "stringArgumentValue"
    }

    def "get argument Type is Non Null String, Literal Is String"() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(true)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "stringArgumentValue", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()
        actualStringValue.getValue() == "stringArgumentValue"
    }

    def "get argument Type is String, Literal Is Int"() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "123456789", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()
        actualStringValue.getValue() == "123456789"
    }

    def "get argument Type is String, Literal Is Boolean"() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLString.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "true", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject =  new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        StringValue actualStringValue = (StringValue) actualField.getArguments().get(0).getValue()
        actualStringValue.getValue() == "true"
    }

    def "get argument Type is Int, Literal Is Int"() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLInt.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "123456789", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        IntValue actualIntValue = (IntValue) actualField.getArguments().get(0).getValue()
        actualIntValue.getValue().intValue() == 123456789
    }

    def "get argument Type is Boolean, Literal Is Boolean"() {
        given:
        PrimitiveType targetArgumentType = GraphQLFactoryDelegate.createPrimitiveType()
        targetArgumentType.setType(Scalars.GraphQLBoolean.getName())
        targetArgumentType.setNonNull(false)

        resolverDirectiveDefinitionMock.getArguments() >> singletonList(
            new ResolverArgumentDefinition("argName", "true", targetArgumentType))

        String[] resolverSelectedFields = [ "targetField" ]

        when:
        subject = new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments,
            testFieldResolverContext)
        SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        BooleanValue actualBooleanValue = (BooleanValue) actualField.getArguments().get(0).getValue()
        actualBooleanValue.isValue()
    }

    def "get argument Type is Enum, Literal Is An Valid Enum Value"() {
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
            testFieldResolverContext)
        SelectionSet actual = subject.get()

        then:
        Field actualField = (Field) actual.getSelections().get(0)
        actualField.getName() == "targetField"
        actualField.getSelectionSet() == null
        actualField.getArguments().get(0).getName() == "argName"

        EnumValue actualEnumValue = (EnumValue) actualField.getArguments().get(0).getValue()
        actualEnumValue.getName() == "ENUM_VALUE_1"
    }

}
