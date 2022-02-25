package com.intuit.graphql.orchestrator.federation;

import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.getErrors;

import com.intuit.graphql.orchestrator.batch.BatchResultTransformer;
import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldsContainer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EntityBatchResultTransformer implements BatchResultTransformer {

  @Override
  public List<DataFetcherResult<Object>> toBatchResult(DataFetcherResult<Map<String, Object>> result,
      List<DataFetchingEnvironment> keys) {
    Map<FieldCoordinates, Object> entityValuesByTypeName = extractEntityValues(result);
    return keys.stream()
        .map(key -> toSingleResult(entityValuesByTypeName, result ,key))
        .collect(Collectors.toList());

  }

  private Map<FieldCoordinates, Object> extractEntityValues(DataFetcherResult<Map<String, Object>> result) {
    if (result.getData() == null) {
      return Collections.emptyMap();
    }
    // TODO DO a Typecheck
    List<Map<String, Object>> entitiesValue = (List<Map<String, Object>>) result.getData().get("_entities");
    if (entitiesValue == null) {
      return Collections.emptyMap();
    }
    Map<FieldCoordinates, Object> output = new HashMap<>();
    entitiesValue.forEach(stringObjectMap -> {
      for (String fieldName : stringObjectMap.keySet()) {
        Object object = stringObjectMap.get(fieldName);
        Map<String, Object> dataItem;
        if (object instanceof List) {
          dataItem = (Map<String, Object>) ((List) object).get(0);
        } else {
          dataItem = (Map<String, Object>) object;
        }
        String typeName = (String) dataItem.get("__typename");
        output.put(FieldCoordinates.coordinates(typeName, fieldName), stringObjectMap.get(fieldName));
      }
    });
    return output;
  }

  public static DataFetcherResult<Object> toSingleResult(Map<FieldCoordinates, Object> entityValuesByTypeName,
      DataFetcherResult<Map<String, Object>> result, DataFetchingEnvironment environment) {

    GraphQLFieldsContainer graphQLFieldsContainer = (GraphQLFieldsContainer) environment.getParentType();
    String parentTypeName = graphQLFieldsContainer.getName();
    FieldCoordinates fieldCoordinates = FieldCoordinates.coordinates(parentTypeName, environment.getField().getName());

    Object data = entityValuesByTypeName.get(fieldCoordinates);

    final Field field = environment.getField();
    List<GraphQLError> errors = getErrors(result, field);
    return DataFetcherResult.newResult()
        .data(data)
        .errors(errors)
        .build();
  }
}
