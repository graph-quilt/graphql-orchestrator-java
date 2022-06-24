package com.intuit.graphql.orchestrator.batch;

import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.federation.EntityFetchingException;
import com.intuit.graphql.orchestrator.federation.EntityQuery;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.introspection.Introspection;
import graphql.language.Field;
import graphql.language.InlineFragment;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.collections4.CollectionUtils;
import org.dataloader.BatchLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.IntrospectionUtil.__typenameField;

public class EntityFetcherBatchLoader implements BatchLoader<DataFetchingEnvironment, DataFetcherResult<Object>> {

    private final QueryResponseModifier queryResponseModifier = new DefaultQueryResponseModifier();
    private final BatchResultTransformer batchResultTransformer;
    private final String entityTypeName;
    private final List<String> representationFieldTemplate;
    private final ServiceProvider entityServiceProvider;

    public EntityFetcherBatchLoader(FederationMetadata.EntityExtensionMetadata metadata, String fieldName) {
        this.entityServiceProvider = metadata.getServiceProvider();
        this.entityTypeName = metadata.getTypeName();
        this.representationFieldTemplate = generateRepresentationTemplate(metadata, fieldName);
        this.batchResultTransformer = new EntityFetcherBatchResultTransformer(metadata.getServiceProvider().getNameSpace(), metadata.getTypeName(), fieldName);
    }

    @Override
    public CompletionStage<List<DataFetcherResult<Object>>> load(List<DataFetchingEnvironment> dataFetchingEnvironments) {
        DataFetchingEnvironment dfeTemplate = dataFetchingEnvironments.get(0);
        GraphQLContext graphQLContext = dfeTemplate.getContext();

        List<Map<String, Object>> representations = dataFetchingEnvironments.stream()
            .map(DataFetchingEnvironment::getSource)
            .map(source -> createRepresentation((Map<String, Object>) source))
            .collect(Collectors.toList());

        List<InlineFragment> inlineFragments = new ArrayList<>();
        inlineFragments.add(createEntityRequestInlineFragment(dfeTemplate));

        EntityQuery entityQuery = EntityQuery.builder()
            .graphQLContext(graphQLContext)
            .inlineFragments(inlineFragments)
            .variables(representations)
            .build();

    return this.entityServiceProvider
        .query(entityQuery.createExecutionInput(), graphQLContext)
        .thenApply(queryResponseModifier::modify)
        .thenApply(result -> batchResultTransformer.toBatchResult(result, dataFetchingEnvironments));
    }

    private List<String> generateRepresentationTemplate(FederationMetadata.EntityExtensionMetadata metadata, String fieldName) {
        List<String> representationFields = new ArrayList<>();

        if(CollectionUtils.isNotEmpty(metadata.getKeyDirectives())) {
            metadata.getKeyDirectives()
                    .stream()
                    .map(KeyDirectiveMetadata::getFieldSet)
                    .flatMap(Collection::stream)
                    .map(Field::getName)
                    .forEach(representationFields::add);
        } else {
            throw EntityFetchingException.builder()
                    .serviceNameSpace(metadata.getServiceProvider().getNameSpace())
                    .fieldName(fieldName)
                    .parentTypeName(metadata.getTypeName())
                    .build();
        }

        if(CollectionUtils.isNotEmpty(metadata.getRequiredFields(fieldName))) {
            metadata.getRequiredFields(fieldName)
                .stream()
                .map(Field::getName)
                .filter(reqFieldName -> !representationFields.contains(reqFieldName))
                .forEach(representationFields::add);
        }

        return representationFields;
    }

    private InlineFragment createEntityRequestInlineFragment(DataFetchingEnvironment dfe) {
        String entityTypeName = this.entityTypeName;
        Field originalField = dfe.getField();

        SelectionSet fieldSelectionSet = dfe.getField().getSelectionSet();
        if (fieldSelectionSet != null) {
            // is an object
            fieldSelectionSet =
                    fieldSelectionSet.transform(builder -> builder.selection(__typenameField));
        }

        InlineFragment.Builder inlineFragmentBuilder = InlineFragment.newInlineFragment();
        inlineFragmentBuilder.typeCondition(TypeName.newTypeName().name(entityTypeName).build());
        inlineFragmentBuilder.selectionSet(
                SelectionSet.newSelectionSet()
                        .selection(
                                Field.newField()
                                        .selectionSet(fieldSelectionSet)
                                        .name(originalField.getName())
                                        .build())
                        .build());
        return inlineFragmentBuilder.build();
    }

    private Map<String, Object> createRepresentation(
            Map<String, Object> dataSource
    ){
        Map<String, Object> entityRepresentation = new HashMap<>();
        entityRepresentation.put(Introspection.TypeNameMetaFieldDef.getName(), this.entityTypeName);

        this.representationFieldTemplate
                .forEach(fieldName -> entityRepresentation.put(fieldName, dataSource.get(fieldName)));

        return entityRepresentation;
    }
}
