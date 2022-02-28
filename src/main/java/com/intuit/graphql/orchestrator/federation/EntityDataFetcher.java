package com.intuit.graphql.orchestrator.federation;

import com.intuit.graphql.orchestrator.batch.QueryExecutor;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
import graphql.GraphQLContext;
import graphql.introspection.Introspection;
import graphql.language.Field;
import graphql.language.InlineFragment;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

@RequiredArgsConstructor
public class EntityDataFetcher implements DataFetcher<CompletableFuture<Object>> {

  private final EntityExtensionMetadata entityExtensionMetadata; // Field added in entity

  @Override
  public CompletableFuture<Object> get(final DataFetchingEnvironment dataFetchingEnvironment) {
    List<InlineFragment> inlineFragments = new ArrayList<>();
    List<Map<String, Object>> keyRepresentationVariables = new ArrayList<>();
    Set<String> requiredFields = new HashSet<>();

    // TODO validate that base entity key's value are present

    Map<String, Object> dfeSource = dataFetchingEnvironment.getSource();

    // get required fields
    entityExtensionMetadata.getRequiredFields().stream()
        .filter(requiredFieldName -> !dfeSource.containsKey(requiredFieldName))
        .forEach(requiredFieldName -> requiredFields.add(requiredFieldName));

    // create representation variables from key directives
    keyRepresentationVariables.add(createKeyRepresentationVariables(dataFetchingEnvironment));

     inlineFragments.add(createEntityRequestInlineFragment(dataFetchingEnvironment));

    GraphQLContext graphQLContext = dataFetchingEnvironment.getContext();

    // representation values may be taken from dfe.source() or from a remote service
    CompletableFuture<List<Map<String, Object>>> futureRepresentations =
        createFutureRepresentation(graphQLContext, keyRepresentationVariables, requiredFields);
    return futureRepresentations.thenCompose(representationMap -> {
      EntityQuery entityQuery =
          EntityQuery.builder()
              .graphQLContext(graphQLContext)
              .inlineFragments(inlineFragments)
              .variables(representationMap)
              .build();

      QueryExecutor queryExecutor = entityExtensionMetadata.getServiceProvider();
      return queryExecutor
          .query(entityQuery.createExecutionInput(), graphQLContext)
          .thenApply(result -> {
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            List<Map<String, Object>> _entities = (List<Map<String, Object>>) data.get("_entities");

            String fieldName = dataFetchingEnvironment.getField().getName();
            return _entities.get(0).get(fieldName);
          });
    });
  }

  private CompletableFuture<List<Map<String, Object>>> createFutureRepresentation(GraphQLContext graphQLContext,
      List<Map<String, Object>> keyRepresentationVariables, Set<String> requiredFields) {

    if (CollectionUtils.isEmpty(requiredFields)) {
      return CompletableFuture.completedFuture(keyRepresentationVariables);
    } else {
      EntityQuery entityQuery =
          EntityQuery.builder()
              .graphQLContext(graphQLContext)
              .inlineFragments(Collections.singletonList(createInlineFragments(requiredFields)))
              .variables(keyRepresentationVariables)
              .build();

      QueryExecutor queryExecutor = entityExtensionMetadata.getBaseServiceProvider();
      return queryExecutor
          .query(entityQuery.createExecutionInput(), graphQLContext)
          .thenApply(
              result -> {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                List<Map<String, Object>> _entities = (List<Map<String, Object>>) data.get("_entities");
                for (String requiredField : requiredFields) {
                  keyRepresentationVariables.get(0).put(requiredField, _entities.get(0).get(requiredField));
                }
                return keyRepresentationVariables;
              });
    }
  }

  private InlineFragment createInlineFragments(Set<String> requiredFields) {
    String entityTypeName = entityExtensionMetadata.getTypeName();
    Field __typenameField =
        Field.newField().name(Introspection.TypeNameMetaFieldDef.getName()).build();

    SelectionSet.Builder selectionSetBuilder = SelectionSet.newSelectionSet();
    for (String requiredField : requiredFields) {
      selectionSetBuilder.selection(new Field(requiredField)); // TODO a requireField may be complex
    }
    selectionSetBuilder.selection(__typenameField);
    SelectionSet fieldSelectionSet = selectionSetBuilder.build();

    InlineFragment.Builder inlineFragmentBuilder = InlineFragment.newInlineFragment();
    inlineFragmentBuilder.typeCondition(TypeName.newTypeName().name(entityTypeName).build());
    inlineFragmentBuilder.selectionSet(fieldSelectionSet)
            .build();
    return inlineFragmentBuilder.build();
  }

  private InlineFragment createEntityRequestInlineFragment(DataFetchingEnvironment dfe) {
    String entityTypeName = entityExtensionMetadata.getTypeName();
    Field originalField = dfe.getField();
    Field __typenameField =
        Field.newField().name(Introspection.TypeNameMetaFieldDef.getName()).build();

    SelectionSet fieldSelectionSet = dfe.getField().getSelectionSet();
    if (fieldSelectionSet != null) {
      fieldSelectionSet = fieldSelectionSet.transform(builder -> builder.selection(__typenameField));
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

  private Map<String, Object> createKeyRepresentationVariables(DataFetchingEnvironment dfe) {
    String entityTypeName = entityExtensionMetadata.getTypeName();
    Map<String, Object> entityRepresentation = new HashMap<>();
    entityRepresentation.put(Introspection.TypeNameMetaFieldDef.getName(), entityTypeName);

    Map<String, Object> dataSource = dfe.getSource();
    entityExtensionMetadata.getKeyDirectives().stream()
        .flatMap(keyDirectiveMetadata -> keyDirectiveMetadata.getKeyFieldNames().stream())
        .forEach(
            keyFieldName -> entityRepresentation.put(keyFieldName, dataSource.get(keyFieldName)));

    return entityRepresentation;
  }

//  @Override
//  public Object get(final DataFetchingEnvironment environment) {
//    return environment
//        .getDataLoader(entityExtensionContext.getDataLoaderKey())
//        .load(EntityBatchLoadingEnvironment.builder()
//            .entityExtensionContext(entityExtensionContext)
//            .dataFetchingEnvironment(environment)
//            .build()
//        );
//  }

}
