package com.intuit.graphql.orchestrator.batch;

import com.intuit.graphql.orchestrator.federation.EntityFetchingException;
import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.intuit.graphql.orchestrator.utils.FederationConstants._ENTITIES_FIELD_NAME;

public class EntityFetcherBatchResultTransformer  implements BatchResultTransformer {
    private final String providerNamespace;
    private final String entityName;
    private final String extFieldName;

    public static final String NO_ENTITY_FIELD = "Faulty entity response due to null _entities field";

    public EntityFetcherBatchResultTransformer(String providerNamespace, String entityName, String fieldName) {
        this.providerNamespace = providerNamespace;
        this.entityName = entityName;
        this.extFieldName = fieldName;
    }

    @Override
    public List<DataFetcherResult<Object>> toBatchResult(DataFetcherResult<Map<String, Object>> dataFetcherResult,
                                                         List<DataFetchingEnvironment> dataFetchingEnvironments) {
        if(dataFetcherResult.hasErrors()) {
            throw EntityFetchingException.builder()
                    .serviceNameSpace(providerNamespace)
                    .fieldName(extFieldName)
                    .parentTypeName(entityName)
                    .additionalInfo(
                        dataFetcherResult.getErrors().stream()
                        .map(GraphQLError::getMessage)
                        .reduce("", (partialString, errMsg) -> StringUtils.join(partialString, errMsg, " "))
                    )
                    .build();
        }

        List<DataFetcherResult<Object>> dataFetcherResults = new ArrayList<>();
        List<Map<String, Object>> _entities = (dataFetcherResult.getData().isEmpty()) ? Collections.emptyList() : (List<Map<String, Object>>) dataFetcherResult.getData().get(_ENTITIES_FIELD_NAME);
        if(!_entities.isEmpty()) {
            _entities.forEach(entityResult -> {
                if(entityResult != null) {
                    List<GraphQLError> errors = (dataFetcherResult.hasErrors()) ? Collections.emptyList() : dataFetcherResult.getErrors();


                    dataFetcherResults.add(DataFetcherResult.newResult()
                            .data(entityResult.get(this.extFieldName))
                            .errors(errors)
                            .build()
                    );
                }
            });
        } else {
            throw EntityFetchingException.builder()
                    .serviceNameSpace(providerNamespace)
                    .parentTypeName(entityName)
                    .fieldName(extFieldName)
                    .additionalInfo(NO_ENTITY_FIELD)
                    .build();
        }

        return dataFetcherResults;
    }
}
