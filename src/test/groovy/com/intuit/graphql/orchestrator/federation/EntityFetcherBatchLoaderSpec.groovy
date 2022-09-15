package com.intuit.graphql.orchestrator.federation

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Sets
import com.intuit.graphql.graphQL.Argument
import com.intuit.graphql.graphQL.Directive
import com.intuit.graphql.graphQL.DirectiveDefinition
import com.intuit.graphql.graphQL.TypeDefinition
import com.intuit.graphql.graphQL.ValueWithVariable
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.batch.BatchResultTransformer
import com.intuit.graphql.orchestrator.batch.EntityFetcherBatchLoader
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata
import com.intuit.graphql.orchestrator.schema.ServiceMetadata
import graphql.ExecutionInput
import graphql.GraphQLContext
import graphql.Scalars
import graphql.execution.DataFetcherResult
import graphql.language.Field
import graphql.schema.DataFetchingEnvironment
import org.powermock.reflect.Whitebox
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicReference

import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.*

class EntityFetcherBatchLoaderSpec extends Specification {

    private ServiceProvider serviceProviderMock
    private FederationMetadata.EntityExtensionMetadata metadataMock
    private ServiceMetadata serviceMetadata;
    private Map<String, TypeDefinition> typeDefinitionMap;

    final private String extEntityField = "requestedExtEntityField"

    private EntityFetcherBatchLoader specUnderTest

    def setup(){
        serviceProviderMock = Mock(ServiceProvider)
        metadataMock = Mock(FederationMetadata.EntityExtensionMetadata)
        serviceMetadata = Mock(ServiceMetadata)

        metadataMock.getServiceProvider() >> serviceProviderMock
        metadataMock.getTypeName() >> "MOCK_ENTITY"
        serviceProviderMock.getNameSpace() >> "MOCK_PROVIDER"
    }

    def "batchloader throws exception if no key directive"(){
        given:
        metadataMock.getKeyDirectives() >> Collections.emptyList()

        when:
        specUnderTest = new EntityFetcherBatchLoader(metadataMock, serviceMetadata, typeDefinitionMap, extEntityField)

        then:
        def exception = thrown(RuntimeException)
        exception in EntityFetchingException
        exception.getMessage().contains("No Key Directive Found")
    }

    def "batchloader creates singular representation request"(){
        given:
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(KeyDirectiveMetadata.from(generateKeyDirective("keyField1")))
        metadataMock.getKeyDirectives() >> keyDirectives

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, serviceMetadata, this.typeDefinitionMap, extEntityField)

        when:
        ServiceProvider  entityServiceProviderField = Whitebox.getInternalState(specUnderTest, "entityServiceProvider")
        String  entityTypeNameField = Whitebox.getInternalState(specUnderTest, "entityTypeName")
        List<String> representationFieldTemplateField = Whitebox.getInternalState(specUnderTest, "representationFieldTemplate")
        BatchResultTransformer batchResultTransformerField = Whitebox.getInternalState(specUnderTest, "batchResultTransformer")

        then:
        batchResultTransformerField != null
        entityServiceProviderField.getNameSpace() == "MOCK_PROVIDER"
        entityTypeNameField == "MOCK_ENTITY"
        representationFieldTemplateField.size() == 1
        representationFieldTemplateField.get(0) == "keyField1"
    }

    def "batchLoader required fields are added"(){
        given:
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(KeyDirectiveMetadata.from(generateKeyDirective("keyField1")))
        metadataMock.getKeyDirectives() >> keyDirectives

        Set<Field> requiredFields = Sets.newHashSet()
        requiredFields.add(Field.newField().name("keyField1").build())
        requiredFields.add(Field.newField().name("requiredField1").build())
        requiredFields.add(Field.newField().name("requiredField2").build())

        metadataMock.getRequiredFields(extEntityField) >> requiredFields

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, serviceMetadata, typeDefinitionMap, extEntityField)

        when:
        ServiceProvider  entityServiceProviderField = Whitebox.getInternalState(specUnderTest, "entityServiceProvider")
        String  entityTypeNameField = Whitebox.getInternalState(specUnderTest, "entityTypeName")
        List<String> representationFieldTemplateField = Whitebox.getInternalState(specUnderTest, "representationFieldTemplate")
        BatchResultTransformer batchResultTransformerField = Whitebox.getInternalState(specUnderTest, "batchResultTransformer")

        then:
        batchResultTransformerField != null
        entityServiceProviderField.getNameSpace() == "MOCK_PROVIDER"
        entityTypeNameField == "MOCK_ENTITY"
        representationFieldTemplateField.size() == 3

        representationFieldTemplateField.stream().anyMatch({ templateField -> templateField.equals("keyField1") })
        representationFieldTemplateField.stream().anyMatch({ templateField -> templateField.equals("requiredField1") })
        representationFieldTemplateField.stream().anyMatch({ templateField -> templateField.equals("requiredField2") })
    }

    def "batchloader creates multiple required fields as single template"(){
        given:
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(
                KeyDirectiveMetadata.from(generateKeyDirective("keyField1")),
                KeyDirectiveMetadata.from(generateKeyDirective("keyField2")),
                KeyDirectiveMetadata.from(generateKeyDirective("keyField3")),
                KeyDirectiveMetadata.from(generateKeyDirective("keyField4"))
        )

        metadataMock.getKeyDirectives() >> keyDirectives

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, serviceMetadata,typeDefinitionMap, extEntityField)

        when:
        ServiceProvider  entityServiceProviderField = Whitebox.getInternalState(specUnderTest, "entityServiceProvider")
        String  entityTypeNameField = Whitebox.getInternalState(specUnderTest, "entityTypeName")
        List<String> representationFieldTemplateField = Whitebox.getInternalState(specUnderTest, "representationFieldTemplate")
        BatchResultTransformer batchResultTransformerField = Whitebox.getInternalState(specUnderTest, "batchResultTransformer")

        then:
        batchResultTransformerField != null
        entityServiceProviderField.getNameSpace() == "MOCK_PROVIDER"
        entityTypeNameField == "MOCK_ENTITY"
        representationFieldTemplateField.size() == 4
        representationFieldTemplateField.get(0) == "keyField1"
        representationFieldTemplateField.get(1) == "keyField2"
        representationFieldTemplateField.get(2) == "keyField3"
        representationFieldTemplateField.get(3) == "keyField4"
    }

    def "batchloader retrieves entity for singular dfe"() {
        given:
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(KeyDirectiveMetadata.from(generateKeyDirective("keyField1")))
        metadataMock.getKeyDirectives() >> keyDirectives

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, serviceMetadata, typeDefinitionMap, extEntityField)

        DataFetchingEnvironment dfeMock = Mock(DataFetchingEnvironment.class)
        Map<String, Object> dfeDataSource = new HashMap<>()
        dfeDataSource.put("keyField1", "keyValue")

        dfeMock.getSource() >> dfeDataSource
        dfeMock.getField() >> Field.newField().name(extEntityField).build()
        dfeMock.getFieldType() >> Scalars.GraphQLString
        dfeMock.getContext() >> GraphQLContext.newContext().build()

        AtomicReference<ExecutionInput> queryToProviderRef = new AtomicReference<>()

        serviceProviderMock.query(_, _) >> ({ invocationOnMock ->
            queryToProviderRef.set((ExecutionInput) invocationOnMock.get(0))

            HashMap<String, Object> data = new HashMap<>()
            List<HashMap<String, Object>> entities = new ArrayList<>()
            HashMap<String, Object> entity = new HashMap<>()
            entity.put(extEntityField, "ENTITY1_FIELD")
            entities.add(entity)
            data.put("data", ImmutableMap.of("_entities", entities))
            return CompletableFuture.completedFuture(data)
        })

        List<DataFetchingEnvironment> dfes = Arrays.asList(dfeMock)

        when:
        CompletionStage<List<DataFetcherResult<Object>>> resultsFuture = specUnderTest.load(dfes)

        then:
        List<DataFetcherResult<Object>> entityResults = resultsFuture.toCompletableFuture().get()

        entityResults.size() == dfes.size()
        entityResults.size() == 1
        entityResults.get(0).getData() == "ENTITY1_FIELD"

        ExecutionInput queryToProvider = queryToProviderRef.get()

        queryToProvider != null
        queryToProvider.getVariables().size() == 1
        queryToProvider.getVariables().get("REPRESENTATIONS") != null

        List<Map<String, Object>> representationVariables = (List<Map<String, Object>>) queryToProvider.getVariables().get("REPRESENTATIONS")

        representationVariables.size() == 1
        representationVariables.get(0).get("__typename") == "MOCK_ENTITY"
        representationVariables.get(0).get("keyField1") == "keyValue"
    }

    def "batchloader retrieves entity for singular dfe null result"() {
        given:
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(KeyDirectiveMetadata.from(generateKeyDirective("keyField1")))
        metadataMock.getKeyDirectives() >> keyDirectives

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, serviceMetadata, typeDefinitionMap, extEntityField)

        DataFetchingEnvironment dfeMock = Mock(DataFetchingEnvironment.class)
        Map<String, Object> dfeDataSource = new HashMap<>()
        dfeDataSource.put("keyField1", "keyValue")

        dfeMock.getSource() >> dfeDataSource
        dfeMock.getField() >> Field.newField().name(extEntityField).build()
        dfeMock.getFieldType() >> Scalars.GraphQLString
        dfeMock.getContext() >> GraphQLContext.newContext().build()

        AtomicReference<ExecutionInput> queryToProviderRef = new AtomicReference<>()

        serviceProviderMock.query(_, _) >> ({ invocationOnMock ->
            queryToProviderRef.set((ExecutionInput) invocationOnMock.get(0))

            HashMap<String, Object> data = new HashMap<>()
            List<HashMap<String, Object>> entities = new ArrayList<>()
            HashMap<String, Object> entity = new HashMap<>()
            entity.put(extEntityField, null)
            entities.add(entity)
            data.put("data", ImmutableMap.of("_entities", entities))
            return CompletableFuture.completedFuture(data)
        })

        List<DataFetchingEnvironment> dfes = Arrays.asList(dfeMock)

        CompletionStage<List<DataFetcherResult<Object>>> resultsFuture = specUnderTest.load(dfes)
        List<DataFetcherResult<Object>> entityResults = resultsFuture.toCompletableFuture().get()

        entityResults.size() == dfes.size()
        entityResults.size() == 1
        entityResults.get(0).getData() == null

        ExecutionInput queryToProvider = queryToProviderRef.get()

        queryToProvider != null
        queryToProvider.getVariables().size() == 1
        queryToProvider.getVariables().get("REPRESENTATIONS") != null

        List<Map<String, Object>> representationVariables = (List<Map<String, Object>>) queryToProvider.getVariables().get("REPRESENTATIONS")
        representationVariables.size() == 1
        representationVariables.get(0).get("__typename") == "MOCK_ENTITY"
        representationVariables.get(0).get("keyField1") == "keyValue"
    }

    def "batchloader retrieves entity for multiple dfes"() {
        given:
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(KeyDirectiveMetadata.from(generateKeyDirective("keyField1")))
        metadataMock.getKeyDirectives() >> keyDirectives

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, serviceMetadata, typeDefinitionMap, extEntityField)

        DataFetchingEnvironment dfeMock1 = Mock(DataFetchingEnvironment.class)
        Map<String, Object> dfe1DataSource = new HashMap<>()
        dfe1DataSource.put("keyField1", "dfeKey1")

        dfeMock1.getSource() >> dfe1DataSource
        dfeMock1.getField() >> Field.newField().name(extEntityField).build()
        dfeMock1.getFieldType() >> Scalars.GraphQLString
        dfeMock1.getContext() >> GraphQLContext.newContext().build()

        DataFetchingEnvironment dfeMock2 = Mock(DataFetchingEnvironment.class)
        Map<String, Object> dfe2DataSource = new HashMap<>()
        dfe2DataSource.put("keyField1", "dfeKey2")

        dfeMock2.getSource() >> dfe2DataSource
        dfeMock2.getField() >> Field.newField().name(extEntityField).build()
        dfeMock2.getFieldType() >> Scalars.GraphQLString
        dfeMock2.getContext() >> GraphQLContext.newContext().build()

        DataFetchingEnvironment dfeMock3 = Mock(DataFetchingEnvironment.class)
        Map<String, Object> dfe3DataSource = new HashMap<>()
        dfe3DataSource.put("keyField1", "dfeKey3")

        dfeMock3.getSource() >> dfe3DataSource
        dfeMock3.getField() >> Field.newField().name(extEntityField).build()
        dfeMock3.getContext() >> GraphQLContext.newContext().build()

        AtomicReference<ExecutionInput> queryToProviderRef = new AtomicReference<>()

        serviceProviderMock.query(_, _) >> ({ invocationOnMock ->
            queryToProviderRef.set((ExecutionInput) invocationOnMock.get(0))

            Map<String, Object> data = new HashMap<>()
            List<Map<String, Object>> entities = new ArrayList<>()

            Map<String, Object> entity1 = ImmutableMap.of(extEntityField, "ENTITY1_FIELD")
            Map<String, Object> entity2 = ImmutableMap.of(extEntityField, "ENTITY2_FIELD")
            Map<String, Object> entity3 = ImmutableMap.of(extEntityField, "ENTITY3_FIELD")

            entities.add(entity1)
            entities.add(entity2)
            entities.add(entity3)

            data.put("data", ImmutableMap.of("_entities", entities))
            return CompletableFuture.completedFuture(data)
        })

        List<DataFetchingEnvironment> dfes = Arrays.asList(dfeMock1, dfeMock2, dfeMock3)

        when:
        CompletionStage<List<DataFetcherResult<Object>>> resultsFuture = specUnderTest.load(dfes)

        then:
        List<DataFetcherResult<Object>> entityResults = resultsFuture.toCompletableFuture().get()

        entityResults.size() == dfes.size()
        entityResults.size() == 3
        entityResults.get(0).getData() == "ENTITY1_FIELD"
        entityResults.get(1).getData() == "ENTITY2_FIELD"
        entityResults.get(2).getData() == "ENTITY3_FIELD"

        ExecutionInput queryToProvider = queryToProviderRef.get()

        queryToProvider != null
        queryToProvider.getVariables().size() == 1
        queryToProvider.getVariables().get("REPRESENTATIONS") != null

        List<Map<String, Object>> representationVariables = (List<Map<String, Object>>) queryToProvider.getVariables().get("REPRESENTATIONS")

        representationVariables.size() == 3
        representationVariables.get(0).get("__typename") == "MOCK_ENTITY"
        representationVariables.get(0).get("keyField1") == "dfeKey1"
        representationVariables.get(1).get("__typename") == "MOCK_ENTITY"
        representationVariables.get(1).get("keyField1") == "dfeKey2"
        representationVariables.get(2).get("__typename") == "MOCK_ENTITY"
        representationVariables.get(2).get("keyField1") == "dfeKey3"
    }

    def "batchloader retrieves entity for multiple dfes one null"() {
        given:
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(KeyDirectiveMetadata.from(generateKeyDirective("keyField1")))
        metadataMock.getKeyDirectives() >> keyDirectives

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, serviceMetadata, typeDefinitionMap, extEntityField)

        DataFetchingEnvironment dfeMock1 = Mock(DataFetchingEnvironment.class)
        Map<String, Object> dfe1DataSource = new HashMap<>()
        dfe1DataSource.put("keyField1", "dfeKey1")

        dfeMock1.getSource() >> dfe1DataSource
        dfeMock1.getField() >> Field.newField().name(extEntityField).build()
        dfeMock1.getFieldType() >> Scalars.GraphQLString
        dfeMock1.getContext() >> GraphQLContext.newContext().build()

        DataFetchingEnvironment dfeMock2 = Mock(DataFetchingEnvironment.class)
        Map<String, Object> dfe2DataSource = new HashMap<>()
        dfe2DataSource.put("keyField1", "dfeKey2")

        dfeMock2.getSource() >> dfe2DataSource
        dfeMock2.getField() >> Field.newField().name(extEntityField).build()
        dfeMock2.getFieldType() >> Scalars.GraphQLString
        dfeMock2.getContext() >> GraphQLContext.newContext().build()

        DataFetchingEnvironment dfeMock3 = Mock(DataFetchingEnvironment.class)
        Map<String, Object> dfe3DataSource = new HashMap<>()
        dfe3DataSource.put("keyField1", "dfeKey3")

        dfeMock3.getSource() >> dfe3DataSource
        dfeMock3.getField() >> Field.newField().name(extEntityField).build()
        dfeMock3.getContext() >> GraphQLContext.newContext().build()

        AtomicReference<ExecutionInput> queryToProviderRef = new AtomicReference<>()

        serviceProviderMock.query(_, _) >> ({ invocationOnMock ->
            queryToProviderRef.set((ExecutionInput) invocationOnMock.get(0))

            Map<String, Object> data = new HashMap<>()
            List<Map<String, Object>> entities = new ArrayList<>()

            Map<String, Object> entity1 = ImmutableMap.of(extEntityField, "ENTITY1_FIELD")
            Map<String, Object> entity2 = new HashMap()
            entity2.put(extEntityField, null)

            Map<String, Object> entity3 = ImmutableMap.of(extEntityField, "ENTITY3_FIELD")

            entities.add(entity1)
            entities.add(entity2)
            entities.add(entity3)

            data.put("data", ImmutableMap.of("_entities", entities))
            return CompletableFuture.completedFuture(data)
        })

        List<DataFetchingEnvironment> dfes = Arrays.asList(dfeMock1, dfeMock2, dfeMock3)

        when:
        CompletionStage<List<DataFetcherResult<Object>>> resultsFuture = specUnderTest.load(dfes)

        then:
        List<DataFetcherResult<Object>> entityResults = resultsFuture.toCompletableFuture().get()

        entityResults.size() == dfes.size()
        entityResults.size() == 3
        entityResults.get(0).getData() == "ENTITY1_FIELD"
        entityResults.get(1).getData() == null
        entityResults.get(2).getData() == "ENTITY3_FIELD"

        ExecutionInput queryToProvider = queryToProviderRef.get()

        queryToProvider != null
        queryToProvider.getVariables().size() == 1
        queryToProvider.getVariables().get("REPRESENTATIONS") != null

        List<Map<String, Object>> representationVariables = (List<Map<String, Object>>) queryToProvider.getVariables().get("REPRESENTATIONS")
        representationVariables.size() == 3
        representationVariables.get(0).get("__typename") == "MOCK_ENTITY"
        representationVariables.get(0).get("keyField1") == "dfeKey1"
        representationVariables.get(1).get("__typename") == "MOCK_ENTITY"
        representationVariables.get(1).get("keyField1") == "dfeKey2"
        representationVariables.get(2).get("__typename") == "MOCK_ENTITY"
        representationVariables.get(2).get("keyField1") == "dfeKey3"
    }

    private Directive generateKeyDirective(String fieldSet) {
        ValueWithVariable fieldsInput = createValueWithVariable()
        fieldsInput.setStringValue(fieldSet)

        Argument fieldsArgument = createArgument()
        fieldsArgument.setName("fields")
        fieldsArgument.setValueWithVariable(fieldsInput)

        DirectiveDefinition keyDefinition = createDirectiveDefinition()
        keyDefinition.setName("key")
        keyDefinition.setRepeatable(true)

        Directive keyDir = createDirective()
        keyDir.setDefinition(keyDefinition)
        keyDir.getArguments().add(fieldsArgument)
        return keyDir
    }
}
