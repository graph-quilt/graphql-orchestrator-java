package com.intuit.graphql.orchestrator.batch;

import com.intuit.graphql.orchestrator.federation.EntityFetchingException;
import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.intuit.graphql.orchestrator.utils.FederationConstants._ENTITIES_FIELD_NAME;

@Slf4j
public class EntityFetcherBatchResultTransformer  implements BatchResultTransformer {
    private final String providerNamespace;
    private final String entityName;
    private final String extFieldName;

    private final int ENTITY_PATH_IDX = 1;

    public static final String NO_ENTITY_FIELD = "Faulty entity response due to null _entities field";

    public EntityFetcherBatchResultTransformer(String providerNamespace, String entityName, String fieldName) {
        this.providerNamespace = providerNamespace;
        this.entityName = entityName;
        this.extFieldName = fieldName;
    }

    @Override
    public List<DataFetcherResult<Object>> toBatchResult(DataFetcherResult<Map<String, Object>> dataFetcherResult,
                                                         List<DataFetchingEnvironment> dataFetchingEnvironments) {
        List<DataFetcherResult<Object>> dataFetcherResults = new ArrayList<>();
        List<Map<String, Object>> _entities = (MapUtils.isEmpty(dataFetcherResult.getData())) ? Collections.emptyList() : (List<Map<String, Object>>) dataFetcherResult.getData().get(_ENTITIES_FIELD_NAME);

        if(CollectionUtils.isEmpty(_entities)) {
            //Should not happen; if it did, downstream did not create entity fetcher correctly or is not using DGS
            log.warn("Provider {} is not Federation subgraph specification compliant. _entities field missing or empty in the response.", providerNamespace);

            throw EntityFetchingException.builder()
                    .serviceNameSpace(providerNamespace)
                    .parentTypeName(entityName)
                    .fieldName(extFieldName)
                    .additionalInfo(NO_ENTITY_FIELD)
                    .build();
        }

        mapErrorsToEntities(dataFetcherResult, _entities);

        //using IntStream instead of regular for loop or atomic reference, so we can parallelize this if we want
        IntStream.range(0, _entities.size()).forEach( idx -> {

            DataFetchingEnvironment dataFetchingEnvironment = dataFetchingEnvironments.get(idx);
            Field field = dataFetchingEnvironment.getField();
            String fieldNameOrAlias = field.getAlias() == null ? field.getName() : field.getAlias();

            Map<String, Object> entityInfo = _entities.get(idx);
            Object fieldData = (entityInfo != null) ? entityInfo.get(fieldNameOrAlias) : null;
            List<GraphQLError> graphQLErrors = (List<GraphQLError>) entityInfo.getOrDefault("errors", new ArrayList<>());

            dataFetcherResults.add(DataFetcherResult.newResult()
                    .data(fieldData)
                    .errors(graphQLErrors)
                    .build()
            );
        });

        return dataFetcherResults;
    }

    private void mapErrorsToEntities( DataFetcherResult<Map<String, Object>> dataFetcherResult, List<Map<String, Object>> entityList) {
        if(dataFetcherResult.hasErrors()) {
            dataFetcherResult.getErrors().forEach(error -> {
                error.getExtensions().put("serviceNamespace", providerNamespace);
                error.getExtensions().put("parentTypename", entityName);
                error.getExtensions().put("fieldName", extFieldName);
                error.getExtensions().put("downstreamErrors", error.toSpecification());

                //Decided to ignore if errors have no path to match field resolver. This should not happen though.
                if(error.getPath() != null) {
                    //path is always _entities/entityIdx/entityField for entity fetch
                    Integer pathIndex = (Integer) error.getPath().get(ENTITY_PATH_IDX);
                    Map<String, Object> entityInfo = entityList.get(pathIndex);

                    if(entityInfo != null) {
                       List<GraphQLError> errors = (List<GraphQLError>) entityInfo.get("errors");
                       boolean containsKey = errors != null;
                       //this case should not happen, but adding check just in case
                       if(!containsKey) {
                            errors = new ArrayList<>();
                            entityInfo.put("errors" ,errors);
                       }

                       errors.add(error);
                    }
                }
            });
        }
    }
}
