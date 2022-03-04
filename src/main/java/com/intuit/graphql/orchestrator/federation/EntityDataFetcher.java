package com.intuit.graphql.orchestrator.federation;

import com.intuit.graphql.orchestrator.batch.QueryExecutor;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

@RequiredArgsConstructor
public class EntityDataFetcher implements DataFetcher<CompletableFuture<Object>> {

  private final EntityExtensionMetadata entityExtensionMetadata; // Field added in entity

  @Override
  public CompletableFuture<Object> get(final DataFetchingEnvironment dataFetchingEnvironment) {
    // TODO validate that base entity key's value are present
    GraphQLContext graphQLContext = dataFetchingEnvironment.getContext();
    String fieldName = dataFetchingEnvironment.getField().getName();
    Map<String, Object> dfeSource = dataFetchingEnvironment.getSource();

    Set<String> requiredFields =
        entityExtensionMetadata.getRequiredFields(fieldName).stream()
            .filter(requiredFieldName -> !dfeSource.containsKey(requiredFieldName))
            .collect(Collectors.toSet());

    // create representation variables from key directives
    List<Map<String, Object>> keyRepresentationVariables = new ArrayList<>();
    String entityTypename = entityExtensionMetadata.getTypeName();
    List<KeyDirectiveMetadata> keyDirectives = entityExtensionMetadata.getBaseEntityMetadata().getKeyDirectives();
    keyRepresentationVariables.add(
        createRepresentationWithKeys(entityTypename, keyDirectives, dfeSource));

    // representation values may be taken from dfe.source() or from a remote service
    return createFutureRepresentation(graphQLContext, keyRepresentationVariables, requiredFields)
        .thenCompose(
            representationMap -> {
              List<InlineFragment> inlineFragments = new ArrayList<>();
              inlineFragments.add(createEntityRequestInlineFragment(dataFetchingEnvironment));

              EntityQuery entityQuery =
                  EntityQuery.builder()
                      .graphQLContext(graphQLContext)
                      .inlineFragments(inlineFragments)
                      .variables(representationMap)
                      .build();

              QueryExecutor queryExecutor = entityExtensionMetadata.getServiceProvider();
              return queryExecutor
                  .query(entityQuery.createExecutionInput(), graphQLContext)
                  .thenApply(
                      result -> {
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        List<Map<String, Object>> _entities =
                            (List<Map<String, Object>>) data.get("_entities");
                        return _entities.get(0).get(fieldName);
                      });
            });
  }

  private CompletableFuture<List<Map<String, Object>>> createFutureRepresentation(
      GraphQLContext graphQLContext,
      List<Map<String, Object>> keyRepresentationVariables,
      Set<String> requiredFields) {

    if (CollectionUtils.isEmpty(requiredFields)) {
      return CompletableFuture.completedFuture(keyRepresentationVariables);
    } else {
      EntityQuery entityQuery =
          EntityQuery.builder()
              .graphQLContext(graphQLContext)
              .inlineFragments(Collections.singletonList(createInlineFragment(requiredFields)))
              .variables(keyRepresentationVariables)
              .build();

      QueryExecutor queryExecutor = entityExtensionMetadata.getBaseServiceProvider();
      return queryExecutor
          .query(entityQuery.createExecutionInput(), graphQLContext)
          .thenApply(
              result -> {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                List<Map<String, Object>> _entities =
                    (List<Map<String, Object>>) data.get("_entities");
                for (String requiredField : requiredFields) {
                  keyRepresentationVariables
                      .get(0)
                      .put(requiredField, _entities.get(0).get(requiredField));
                }
                return keyRepresentationVariables;
              });
    }
  }

  private InlineFragment createInlineFragment(Set<String> requiredFields) {
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
    inlineFragmentBuilder.selectionSet(fieldSelectionSet).build();
    return inlineFragmentBuilder.build();
  }

  private InlineFragment createEntityRequestInlineFragment(DataFetchingEnvironment dfe) {
    String entityTypeName = entityExtensionMetadata.getTypeName();
    Field originalField = dfe.getField();
    Field __typenameField =
        Field.newField().name(Introspection.TypeNameMetaFieldDef.getName()).build();

    SelectionSet fieldSelectionSet = dfe.getField().getSelectionSet();
    if (fieldSelectionSet != null) {
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

  private Map<String, Object> createRepresentationWithKeys(
      String entityTypeName,
      List<KeyDirectiveMetadata> keyDirectives,
      Map<String, Object> dataSource) {
    Map<String, Object> entityRepresentation = new HashMap<>();
    entityRepresentation.put(Introspection.TypeNameMetaFieldDef.getName(), entityTypeName);

    // this might be a subset of entity keys
    if (CollectionUtils.isNotEmpty(keyDirectives)) {
      keyDirectives.stream()
          .flatMap(keyDirectiveMetadata -> keyDirectiveMetadata.getKeyFieldNames().stream())
          .forEach(
              keyFieldName -> entityRepresentation.put(keyFieldName, dataSource.get(keyFieldName)));
    }

    return entityRepresentation;
  }
}
