package com.intuit.graphql.orchestrator.federation;

import static com.intuit.graphql.orchestrator.utils.IntrospectionUtil.__typenameField;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

/**
 * This class is used for resolving fields added to an Entity by making an entity fetch request. To
 * build the entity fetch query, it uses {@link EntityQuery}.
 */
@RequiredArgsConstructor
public class EntityDataFetcher implements DataFetcher<CompletableFuture<Object>> {

  private final EntityExtensionMetadata entityExtensionMetadata; // Field added in entity
  public static final String NO_ENTITY_FIELD = "Faulty entity response due to null _entities field";

  @Override
  public CompletableFuture<Object> get(final DataFetchingEnvironment dataFetchingEnvironment) {
    // TODO validate that base entity key's value are present
    GraphQLContext graphQLContext = dataFetchingEnvironment.getContext();
    String fieldName = dataFetchingEnvironment.getField().getName();
    Map<String, Object> dfeSource = dataFetchingEnvironment.getSource();

    Map<String, Object> keyRepresentationVariables = new HashMap<>();

    // create representation variables from key directives
    String entityTypename = entityExtensionMetadata.getTypeName();
    List<KeyDirectiveMetadata> keyDirectives = entityExtensionMetadata.getKeyDirectives();
    if (CollectionUtils.isNotEmpty(keyDirectives)) {
      Map<String, Object> keyVarMap =
          createRepresentationWithForKeys(entityTypename, keyDirectives, dfeSource);
      keyRepresentationVariables.putAll(keyVarMap);
    } else {
      throw EntityFetchingException.builder()
          .serviceNameSpace(entityExtensionMetadata.getServiceProvider().getNameSpace())
          .fieldName(fieldName)
          .parentTypeName(entityTypename)
          .build();
    }

    Set<Field> requiresFieldSet = entityExtensionMetadata.getRequiredFields(fieldName);
    if (CollectionUtils.isNotEmpty(requiresFieldSet)) {
      Map<String, Object> requiredVarMap =
          createRepresentationForRequires(entityTypename, requiresFieldSet, dfeSource);
      keyRepresentationVariables.putAll(requiredVarMap);
    }  // else ignore, use of @requires is optional

    List<InlineFragment> inlineFragments = new ArrayList<>();
    inlineFragments.add(createEntityRequestInlineFragment(dataFetchingEnvironment));

    EntityQuery entityQuery =
        EntityQuery.builder()
            .graphQLContext(graphQLContext)
            .inlineFragments(inlineFragments)
            .variables(Collections.singletonList(keyRepresentationVariables))
            .build();

    QueryExecutor queryExecutor = entityExtensionMetadata.getServiceProvider();
    return queryExecutor
        .query(entityQuery.createExecutionInput(), graphQLContext)
        .thenApply(
            result -> {
              // TODO transformer
              Map<String, Object> data = (Map<String, Object>) result.get("data");
              List<Map<String, Object>> _entities =
                  (List<Map<String, Object>>) data.get("_entities");

              if(_entities == null) {
                throw EntityFetchingException.builder()
                        .serviceNameSpace(entityExtensionMetadata.getServiceProvider().getNameSpace())
                        .fieldName(fieldName)
                        .parentTypeName(entityTypename)
                        .additionalInfo(NO_ENTITY_FIELD)
                        .build();
              }

              return _entities.get(0).get(fieldName);
            });
  }

  private InlineFragment createEntityRequestInlineFragment(DataFetchingEnvironment dfe) {
    String entityTypeName = entityExtensionMetadata.getTypeName();
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

  private Map<String, Object> createRepresentationWithForKeys(
      String entityTypeName,
      List<KeyDirectiveMetadata> keyDirectives,
      Map<String, Object> dataSource) {
    Map<String, Object> entityRepresentation = new HashMap<>();
    entityRepresentation.put(Introspection.TypeNameMetaFieldDef.getName(), entityTypeName);

    // this might be a subset of entity keys
    if (CollectionUtils.isNotEmpty(keyDirectives)) {
      keyDirectives.stream()
          .map(KeyDirectiveMetadata::getFieldSet)
          .flatMap(Collection::stream)
          .forEach(
              field -> entityRepresentation.put(field.getName(), dataSource.get(field.getName())));
    }

    return entityRepresentation;
  }

  private Map<String, Object> createRepresentationForRequires(
      String entityTypename, Set<Field> requiresFieldSet, Map<String, Object> dfeSource) {
    Map<String, Object> entityRepresentation = new HashMap<>();
    entityRepresentation.put(Introspection.TypeNameMetaFieldDef.getName(), entityTypename);

    requiresFieldSet.stream()
        .map(Field::getName)
        .forEach(fieldName -> entityRepresentation.put(fieldName, dfeSource.get(fieldName)));

    return entityRepresentation;
  }
}
