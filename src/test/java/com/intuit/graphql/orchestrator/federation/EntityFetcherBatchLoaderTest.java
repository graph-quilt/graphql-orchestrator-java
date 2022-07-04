package com.intuit.graphql.orchestrator.federation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.DirectiveDefinition;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.batch.BatchResultTransformer;
import com.intuit.graphql.orchestrator.batch.EntityFetcherBatchLoader;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createArgument;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createDirective;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createDirectiveDefinition;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createValueWithVariable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntityFetcherBatchLoaderTest {

    @Mock private ServiceProvider serviceProviderMock;
    @Mock private FederationMetadata.EntityExtensionMetadata metadataMock;
    final private String extEntityField = "requestedExtEntityField";
    private EntityFetcherBatchLoader specUnderTest;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);

        when(metadataMock.getServiceProvider()).thenReturn(serviceProviderMock);
        when(metadataMock.getTypeName()).thenReturn("MOCK_ENTITY");
        when(serviceProviderMock.getNameSpace()).thenReturn("MOCK_PROVIDER");
    }

    @Test
    public void batchloader_throws_exception_if_no_key_directive(){
        when(metadataMock.getKeyDirectives()).thenReturn(Collections.emptyList());

        try {
            specUnderTest = new EntityFetcherBatchLoader(metadataMock, extEntityField);
            Assert.fail("Initialization should fail due to no key directive");
        } catch (RuntimeException ex) {
            if(ex instanceof EntityFetchingException) {
                assertThat(ex.getMessage()).contains("No Key Directive Found");
            } else {
                throw ex;
            }
        }
    }

    @Test
    public void batchloader_creates_singular_representation_request(){
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(KeyDirectiveMetadata.from(generateKeyDirective("keyField1")));
        when(metadataMock.getKeyDirectives()).thenReturn(keyDirectives);

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, extEntityField);

        ServiceProvider  entityServiceProviderField = Whitebox.getInternalState(specUnderTest, "entityServiceProvider");
        String  entityTypeNameField = Whitebox.getInternalState(specUnderTest, "entityTypeName");
        List<String> representationFieldTemplateField = Whitebox.getInternalState(specUnderTest, "representationFieldTemplate");
        BatchResultTransformer batchResultTransformerField = Whitebox.getInternalState(specUnderTest, "batchResultTransformer");

        assertThat(batchResultTransformerField).isNotNull();
        assertThat(entityServiceProviderField.getNameSpace()).isEqualTo("MOCK_PROVIDER");
        assertThat(entityTypeNameField).isEqualTo("MOCK_ENTITY");
        assertThat(representationFieldTemplateField.size()).isEqualTo(1);
        assertThat(representationFieldTemplateField.get(0)).isEqualTo("keyField1");
    }

    @Test
    public void batchloader_required_fields_are_added(){
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(KeyDirectiveMetadata.from(generateKeyDirective("keyField1")));
        when(metadataMock.getKeyDirectives()).thenReturn(keyDirectives);

        Set<Field> requiredFields = Sets.newHashSet();
        requiredFields.add(Field.newField().name("keyField1").build());
        requiredFields.add(Field.newField().name("requiredField1").build());
        requiredFields.add(Field.newField().name("requiredField2").build());

        when(metadataMock.getRequiredFields(extEntityField)).thenReturn(requiredFields);

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, extEntityField);

        ServiceProvider  entityServiceProviderField = Whitebox.getInternalState(specUnderTest, "entityServiceProvider");
        String  entityTypeNameField = Whitebox.getInternalState(specUnderTest, "entityTypeName");
        List<String> representationFieldTemplateField = Whitebox.getInternalState(specUnderTest, "representationFieldTemplate");
        BatchResultTransformer batchResultTransformerField = Whitebox.getInternalState(specUnderTest, "batchResultTransformer");

        assertThat(batchResultTransformerField).isNotNull();
        assertThat(entityServiceProviderField.getNameSpace()).isEqualTo("MOCK_PROVIDER");
        assertThat(entityTypeNameField).isEqualTo("MOCK_ENTITY");
        assertThat(representationFieldTemplateField.size()).isEqualTo(3);

        assertThat(representationFieldTemplateField.stream().anyMatch(templateField -> templateField.equals("keyField1"))).isTrue();
        assertThat(representationFieldTemplateField.stream().anyMatch(templateField -> templateField.equals("requiredField1"))).isTrue();
        assertThat(representationFieldTemplateField.stream().anyMatch(templateField -> templateField.equals("requiredField2"))).isTrue();
    }

    @Test
    public void batchloader_creates_multiple_required_fields_as_single_template(){
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(
                KeyDirectiveMetadata.from(generateKeyDirective("keyField1")),
                KeyDirectiveMetadata.from(generateKeyDirective("keyField2")),
                KeyDirectiveMetadata.from(generateKeyDirective("keyField3")),
                KeyDirectiveMetadata.from(generateKeyDirective("keyField4"))
        );

        when(metadataMock.getKeyDirectives()).thenReturn(keyDirectives);

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, extEntityField);

        ServiceProvider  entityServiceProviderField = Whitebox.getInternalState(specUnderTest, "entityServiceProvider");
        String  entityTypeNameField = Whitebox.getInternalState(specUnderTest, "entityTypeName");
        List<String> representationFieldTemplateField = Whitebox.getInternalState(specUnderTest, "representationFieldTemplate");
        BatchResultTransformer batchResultTransformerField = Whitebox.getInternalState(specUnderTest, "batchResultTransformer");

        assertThat(batchResultTransformerField).isNotNull();
        assertThat(entityServiceProviderField.getNameSpace()).isEqualTo("MOCK_PROVIDER");
        assertThat(entityTypeNameField).isEqualTo("MOCK_ENTITY");
        assertThat(representationFieldTemplateField.size()).isEqualTo(4);
        assertThat(representationFieldTemplateField.get(0)).isEqualTo("keyField1");
        assertThat(representationFieldTemplateField.get(1)).isEqualTo("keyField2");
        assertThat(representationFieldTemplateField.get(2)).isEqualTo("keyField3");
        assertThat(representationFieldTemplateField.get(3)).isEqualTo("keyField4");
    }

    @Test
    public void batchloader_retrieves_entity_for_singlular_dfe() throws Exception {
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(KeyDirectiveMetadata.from(generateKeyDirective("keyField1")));
        when(metadataMock.getKeyDirectives()).thenReturn(keyDirectives);

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, extEntityField);

        DataFetchingEnvironment dfeMock = mock(DataFetchingEnvironment.class);
        Map<String, Object> dfeDataSource = new HashMap<>();
        dfeDataSource.put("keyField1", "keyValue");

        when(dfeMock.getSource()).thenReturn(dfeDataSource);
        when(dfeMock.getField()).thenReturn(Field.newField().name(extEntityField).build());
        when(dfeMock.getContext()).thenReturn(GraphQLContext.newContext().build());

        AtomicReference<ExecutionInput> queryToProviderRef = new AtomicReference<>();

        when(serviceProviderMock.query(any(), any())).thenAnswer(invocationOnMock -> {
            queryToProviderRef.set((ExecutionInput) invocationOnMock.getArgument(0));

            HashMap<String, Object> data = new HashMap<>();
            List<HashMap<String, Object>> entities = new ArrayList<>();
            HashMap<String, Object> entity = new HashMap<>();
            entity.put(extEntityField, "ENTITY1_FIELD");
            entities.add(entity);
            data.put("data", ImmutableMap.of("_entities", entities));
            return CompletableFuture.completedFuture(data);
        });

        List<DataFetchingEnvironment> dfes = Arrays.asList(dfeMock);

        CompletionStage<List<DataFetcherResult<Object>>>  resultsFuture = specUnderTest.load(dfes);
        List<DataFetcherResult<Object>> entityResults = resultsFuture.toCompletableFuture().get();

        assertThat(entityResults.size()).isEqualTo(dfes.size());
        assertThat(entityResults.size()).isEqualTo(1);
        assertThat(entityResults.get(0).getData()).isEqualTo("ENTITY1_FIELD");

        ExecutionInput queryToProvider = queryToProviderRef.get();

        assertThat(queryToProvider).isNotNull();
        assertThat(queryToProvider.getVariables().size()).isEqualTo(1);
        assertThat(queryToProvider.getVariables().get("REPRESENTATIONS")).isNotNull();

        List<Map<String, Object>> representationVariables = (List<Map<String, Object>>) queryToProvider.getVariables().get("REPRESENTATIONS");
        assertThat(representationVariables.size()).isEqualTo(1);
        assertThat(representationVariables.get(0).get("__typename")).isEqualTo("MOCK_ENTITY");
        assertThat(representationVariables.get(0).get("keyField1")).isEqualTo("keyValue");
    }

    @Test
    public void batchloader_retrieves_entity_for_singlular_dfe_null_result() throws Exception {
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(KeyDirectiveMetadata.from(generateKeyDirective("keyField1")));
        when(metadataMock.getKeyDirectives()).thenReturn(keyDirectives);

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, extEntityField);

        DataFetchingEnvironment dfeMock = mock(DataFetchingEnvironment.class);
        Map<String, Object> dfeDataSource = new HashMap<>();
        dfeDataSource.put("keyField1", "keyValue");

        when(dfeMock.getSource()).thenReturn(dfeDataSource);
        when(dfeMock.getField()).thenReturn(Field.newField().name(extEntityField).build());
        when(dfeMock.getContext()).thenReturn(GraphQLContext.newContext().build());

        AtomicReference<ExecutionInput> queryToProviderRef = new AtomicReference<>();

        when(serviceProviderMock.query(any(), any())).thenAnswer(invocationOnMock -> {
            queryToProviderRef.set((ExecutionInput) invocationOnMock.getArgument(0));

            HashMap<String, Object> data = new HashMap<>();
            List<HashMap<String, Object>> entities = new ArrayList<>();
            HashMap<String, Object> entity = new HashMap<>();
            entity.put(extEntityField, null);
            entities.add(entity);
            data.put("data", ImmutableMap.of("_entities", entities));
            return CompletableFuture.completedFuture(data);
        });

        List<DataFetchingEnvironment> dfes = Arrays.asList(dfeMock);

        CompletionStage<List<DataFetcherResult<Object>>>  resultsFuture = specUnderTest.load(dfes);
        List<DataFetcherResult<Object>> entityResults = resultsFuture.toCompletableFuture().get();

        assertThat(entityResults.size()).isEqualTo(dfes.size());
        assertThat(entityResults.size()).isEqualTo(1);
        assertThat(entityResults.get(0).getData()).isNull();

        ExecutionInput queryToProvider = queryToProviderRef.get();

        assertThat(queryToProvider).isNotNull();
        assertThat(queryToProvider.getVariables().size()).isEqualTo(1);
        assertThat(queryToProvider.getVariables().get("REPRESENTATIONS")).isNotNull();

        List<Map<String, Object>> representationVariables = (List<Map<String, Object>>) queryToProvider.getVariables().get("REPRESENTATIONS");
        assertThat(representationVariables.size()).isEqualTo(1);
        assertThat(representationVariables.get(0).get("__typename")).isEqualTo("MOCK_ENTITY");
        assertThat(representationVariables.get(0).get("keyField1")).isEqualTo("keyValue");
    }

    @Test
    public void batchloader_retrieves_entity_for_multiple_dfes() throws Exception {
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(KeyDirectiveMetadata.from(generateKeyDirective("keyField1")));
        when(metadataMock.getKeyDirectives()).thenReturn(keyDirectives);

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, extEntityField);

        DataFetchingEnvironment dfeMock1 = mock(DataFetchingEnvironment.class);
        Map<String, Object> dfe1DataSource = new HashMap<>();
        dfe1DataSource.put("keyField1", "dfeKey1");

        when(dfeMock1.getSource()).thenReturn(dfe1DataSource);
        when(dfeMock1.getField()).thenReturn(Field.newField().name(extEntityField).build());
        when(dfeMock1.getContext()).thenReturn(GraphQLContext.newContext().build());

        DataFetchingEnvironment dfeMock2 = mock(DataFetchingEnvironment.class);
        Map<String, Object> dfe2DataSource = new HashMap<>();
        dfe2DataSource.put("keyField1", "dfeKey2");

        when(dfeMock2.getSource()).thenReturn(dfe2DataSource);
        when(dfeMock2.getField()).thenReturn(Field.newField().name(extEntityField).build());
        when(dfeMock2.getContext()).thenReturn(GraphQLContext.newContext().build());

        DataFetchingEnvironment dfeMock3 = mock(DataFetchingEnvironment.class);
        Map<String, Object> dfe3DataSource = new HashMap<>();
        dfe3DataSource.put("keyField1", "dfeKey3");

        when(dfeMock3.getSource()).thenReturn(dfe3DataSource);
        when(dfeMock3.getField()).thenReturn(Field.newField().name(extEntityField).build());
        when(dfeMock3.getContext()).thenReturn(GraphQLContext.newContext().build());

        AtomicReference<ExecutionInput> queryToProviderRef = new AtomicReference<>();

        when(serviceProviderMock.query(any(), any())).thenAnswer(invocationOnMock -> {
            queryToProviderRef.set((ExecutionInput) invocationOnMock.getArgument(0));

            Map<String, Object> data = new HashMap<>();
            List<Map<String, Object>> entities = new ArrayList<>();

            Map<String, Object> entity1 = ImmutableMap.of(extEntityField, "ENTITY1_FIELD");
            Map<String, Object> entity2 = ImmutableMap.of(extEntityField, "ENTITY2_FIELD");
            Map<String, Object> entity3 = ImmutableMap.of(extEntityField, "ENTITY3_FIELD");

            entities.add(entity1);
            entities.add(entity2);
            entities.add(entity3);

            data.put("data", ImmutableMap.of("_entities", entities));
            return CompletableFuture.completedFuture(data);
        });

        List<DataFetchingEnvironment> dfes = Arrays.asList(dfeMock1, dfeMock2, dfeMock3);

        CompletionStage<List<DataFetcherResult<Object>>>  resultsFuture = specUnderTest.load(dfes);
        List<DataFetcherResult<Object>> entityResults = resultsFuture.toCompletableFuture().get();

        assertThat(entityResults.size()).isEqualTo(dfes.size());
        assertThat(entityResults.size()).isEqualTo(3);
        assertThat(entityResults.get(0).getData()).isEqualTo("ENTITY1_FIELD");
        assertThat(entityResults.get(1).getData()).isEqualTo("ENTITY2_FIELD");
        assertThat(entityResults.get(2).getData()).isEqualTo("ENTITY3_FIELD");

        ExecutionInput queryToProvider = queryToProviderRef.get();

        assertThat(queryToProvider).isNotNull();
        assertThat(queryToProvider.getVariables().size()).isEqualTo(1);
        assertThat(queryToProvider.getVariables().get("REPRESENTATIONS")).isNotNull();

        List<Map<String, Object>> representationVariables = (List<Map<String, Object>>) queryToProvider.getVariables().get("REPRESENTATIONS");
        assertThat(representationVariables.size()).isEqualTo(3);
        assertThat(representationVariables.get(0).get("__typename")).isEqualTo("MOCK_ENTITY");
        assertThat(representationVariables.get(0).get("keyField1")).isEqualTo("dfeKey1");
        assertThat(representationVariables.get(1).get("__typename")).isEqualTo("MOCK_ENTITY");
        assertThat(representationVariables.get(1).get("keyField1")).isEqualTo("dfeKey2");
        assertThat(representationVariables.get(2).get("__typename")).isEqualTo("MOCK_ENTITY");
        assertThat(representationVariables.get(2).get("keyField1")).isEqualTo("dfeKey3");
    }

    @Test
    public void batchloader_retrieves_entity_for_multiple_dfes_one_null() throws Exception {
        List<KeyDirectiveMetadata> keyDirectives = Arrays.asList(KeyDirectiveMetadata.from(generateKeyDirective("keyField1")));
        when(metadataMock.getKeyDirectives()).thenReturn(keyDirectives);

        specUnderTest = new EntityFetcherBatchLoader(metadataMock, extEntityField);

        DataFetchingEnvironment dfeMock1 = mock(DataFetchingEnvironment.class);
        Map<String, Object> dfe1DataSource = new HashMap<>();
        dfe1DataSource.put("keyField1", "dfeKey1");

        when(dfeMock1.getSource()).thenReturn(dfe1DataSource);
        when(dfeMock1.getField()).thenReturn(Field.newField().name(extEntityField).build());
        when(dfeMock1.getContext()).thenReturn(GraphQLContext.newContext().build());

        DataFetchingEnvironment dfeMock2 = mock(DataFetchingEnvironment.class);
        Map<String, Object> dfe2DataSource = new HashMap<>();
        dfe2DataSource.put("keyField1", "dfeKey2");

        when(dfeMock2.getSource()).thenReturn(dfe2DataSource);
        when(dfeMock2.getField()).thenReturn(Field.newField().name(extEntityField).build());
        when(dfeMock2.getContext()).thenReturn(GraphQLContext.newContext().build());

        DataFetchingEnvironment dfeMock3 = mock(DataFetchingEnvironment.class);
        Map<String, Object> dfe3DataSource = new HashMap<>();
        dfe3DataSource.put("keyField1", "dfeKey3");

        when(dfeMock3.getSource()).thenReturn(dfe3DataSource);
        when(dfeMock3.getField()).thenReturn(Field.newField().name(extEntityField).build());
        when(dfeMock3.getContext()).thenReturn(GraphQLContext.newContext().build());

        AtomicReference<ExecutionInput> queryToProviderRef = new AtomicReference<>();

        when(serviceProviderMock.query(any(), any())).thenAnswer(invocationOnMock -> {
            queryToProviderRef.set((ExecutionInput) invocationOnMock.getArgument(0));

            Map<String, Object> data = new HashMap<>();
            List<Map<String, Object>> entities = new ArrayList<>();

            Map<String, Object> entity1 = ImmutableMap.of(extEntityField, "ENTITY1_FIELD");
            Map<String, Object> entity2 = new HashMap();
            entity2.put(extEntityField, null);

            Map<String, Object> entity3 = ImmutableMap.of(extEntityField, "ENTITY3_FIELD");

            entities.add(entity1);
            entities.add(entity2);
            entities.add(entity3);

            data.put("data", ImmutableMap.of("_entities", entities));
            return CompletableFuture.completedFuture(data);
        });

        List<DataFetchingEnvironment> dfes = Arrays.asList(dfeMock1, dfeMock2, dfeMock3);

        CompletionStage<List<DataFetcherResult<Object>>>  resultsFuture = specUnderTest.load(dfes);
        List<DataFetcherResult<Object>> entityResults = resultsFuture.toCompletableFuture().get();

        assertThat(entityResults.size()).isEqualTo(dfes.size());
        assertThat(entityResults.size()).isEqualTo(3);
        assertThat(entityResults.get(0).getData()).isEqualTo("ENTITY1_FIELD");
        assertThat(entityResults.get(1).getData()).isNull();
        assertThat(entityResults.get(2).getData()).isEqualTo("ENTITY3_FIELD");

        ExecutionInput queryToProvider = queryToProviderRef.get();

        assertThat(queryToProvider).isNotNull();
        assertThat(queryToProvider.getVariables().size()).isEqualTo(1);
        assertThat(queryToProvider.getVariables().get("REPRESENTATIONS")).isNotNull();

        List<Map<String, Object>> representationVariables = (List<Map<String, Object>>) queryToProvider.getVariables().get("REPRESENTATIONS");
        assertThat(representationVariables.size()).isEqualTo(3);
        assertThat(representationVariables.get(0).get("__typename")).isEqualTo("MOCK_ENTITY");
        assertThat(representationVariables.get(0).get("keyField1")).isEqualTo("dfeKey1");
        assertThat(representationVariables.get(1).get("__typename")).isEqualTo("MOCK_ENTITY");
        assertThat(representationVariables.get(1).get("keyField1")).isEqualTo("dfeKey2");
        assertThat(representationVariables.get(2).get("__typename")).isEqualTo("MOCK_ENTITY");
        assertThat(representationVariables.get(2).get("keyField1")).isEqualTo("dfeKey3");
    }

    private Directive generateKeyDirective(String fieldSet) {
        ValueWithVariable fieldsInput = createValueWithVariable();
        fieldsInput.setStringValue(fieldSet);

        Argument fieldsArgument = createArgument();
        fieldsArgument.setName("fields");
        fieldsArgument.setValueWithVariable(fieldsInput);

        DirectiveDefinition keyDefinition = createDirectiveDefinition();
        keyDefinition.setName("key");
        keyDefinition.setRepeatable(true);

        Directive keyDir = createDirective();
        keyDir.setDefinition(keyDefinition);
        keyDir.getArguments().add(fieldsArgument);
        return keyDir;
    }
}
