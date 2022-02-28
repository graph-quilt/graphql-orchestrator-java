//package com.intuit.graphql.orchestrator.federation;
//
//import static java.lang.String.join;
//
//import com.intuit.graphql.orchestrator.batch.BatchLoaderExecutionHooks;
//import com.intuit.graphql.orchestrator.batch.BatchResultTransformer;
//import com.intuit.graphql.orchestrator.batch.QueryExecutor;
//import com.intuit.graphql.orchestrator.batch.QueryResponseModifier;
//import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
//import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
//import graphql.GraphQLContext;
//import graphql.execution.DataFetcherResult;
//import graphql.introspection.Introspection;
//import graphql.language.Field;
//import graphql.language.InlineFragment;
//import graphql.language.SelectionSet;
//import graphql.language.TypeName;
//import graphql.schema.DataFetchingEnvironment;
//import graphql.schema.GraphQLFieldsContainer;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CompletionStage;
//import java.util.stream.Collectors;
//import lombok.Builder;
//import org.apache.commons.collections4.CollectionUtils;
//import org.dataloader.BatchLoader;
//
///**
// * This class is used to make an entity fetch for a federated subgraph.
// *
// * <p>An entity fetch sends a graphql query for _entities field. i.e.
// *
// * <pre>{@code
// * _entities(representations: [_Any!]!): [_Entity]!
// * }</pre>
// *
// * Using {@link EntityExtensionMetadata#getServiceProvider()}, this class may make a call to the
// * service that made the entity extension(s).
// *
// * <p>Using {@link EntityExtensionMetadata#getBaseServiceProvider()}, this class may make a calls
// * to the base service who owns the entity type. This occurs when the field's selection set contains
// * a field that is owned by base type and cannot be provided by the extended service.
// */
//@Builder
//public class EntityBatchLoader
//    implements BatchLoader<EntityBatchLoadingEnvironment, DataFetcherResult<Object>> {
//
//  private static final String KEY_DELIMITER = ":";
//
//  private final EntityExtensionMetadata entityExtensionMetadata;
//
//  // TODO field level authorization
//  private final BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> hooks;
//  private final BatchResultTransformer batchResultTransformer;
//  private final QueryResponseModifier queryResponseModifier;
//
//  public static String createDataLoaderKey(String serviceNamespace, String parentTypename) {
//    return join("ENTITY_FETCH", serviceNamespace, parentTypename, KEY_DELIMITER);
//  }
//
//  @Override
//  public CompletionStage<List<DataFetcherResult<Object>>> load(
//      List<EntityBatchLoadingEnvironment> environments) {
//
//    List<InlineFragment> inlineFragments = new ArrayList<>();
//    List<Map<String, Object>> keyRepresentationVariables = new ArrayList<>();
//    Set<String> requiredFields = new HashSet<>();
//
//    for (EntityBatchLoadingEnvironment batchLoadingEnvironment : environments) {
//      DataFetchingEnvironment dataFetchingEnvironment = batchLoadingEnvironment.getDataFetchingEnvironment();
//      Map<String, Object> dfeSource = dataFetchingEnvironment.getSource();
//      GraphQLFieldsContainer entityType = (GraphQLFieldsContainer) dataFetchingEnvironment.getParentType();
//      String entityTypeName = entityType.getName();
//
//      EntityExtensionContext entityExtensionContext = batchLoadingEnvironment.getEntityExtensionContext();
//      EntityExtensionMetadata entityExtensionMetadata = entityExtensionContext.getEntityExtensionMetadata();
//
//      // get required fields
//      entityExtensionMetadata.getRequiredFields().stream()
//          .filter(requiredFieldName -> !dfeSource.containsKey(requiredFieldName) &&
//              !requiredFields.contains(requiredFieldName))
//          .forEach(requiredFieldName -> requiredFields.add(requiredFieldName));
//
//      // create representation variables from key directives
//      keyRepresentationVariables.add(createKeyRepresentationVariables(batchLoadingEnvironment));
//
//      //
//      inlineFragments.add(createEntityRequestInlineFragment(batchLoadingEnvironment));
//    }
//
//    List<DataFetchingEnvironment> dataFetchingEnvironments =
//        environments.stream()
//            .map(EntityBatchLoadingEnvironment::getDataFetchingEnvironment)
//            .collect(Collectors.toList());
//
//    GraphQLContext graphQLContext = dataFetchingEnvironments.get(0).getContext();
//
//    // representation values may be taken from dfe.source() or from a remote service
//    CompletableFuture<List<Map<String, Object>>> futureRepresentations =
//        createFutureRepresentation(graphQLContext, keyRepresentationVariables, requiredFields);
//    return futureRepresentations.thenCompose(representationMap -> {
//      EntityQuery entityQuery =
//          EntityQuery.builder()
//              .graphQLContext(graphQLContext)
//              .inlineFragments(inlineFragments)
//              .variables(representationMap)
//              .build();
//
//      QueryExecutor queryExecutor = entityExtensionMetadata.getServiceProvider();
//      return queryExecutor
//          .query(entityQuery.createExecutionInput(), graphQLContext)
//          .thenApply(queryResponseModifier::modify)
//          .thenApply(
//              result -> batchResultTransformer.toBatchResult(result, dataFetchingEnvironments));
//    });
//  }
//
//  private CompletableFuture<Map<String, Object>> createFutureRepresentation(GraphQLContext graphQLContext,
//      Map<String, Object> keyRepresentationVariables, Set<String> requiredFields) {
//
//    if (CollectionUtils.isEmpty(requiredFields)) {
//      return CompletableFuture.completedFuture(keyRepresentationVariables);
//    } else {
//      EntityQuery entityQuery =
//          EntityQuery.builder()
//              .graphQLContext(graphQLContext)
//              .inlineFragments(createInlineFragments(requiredFields))
//              .variables(keyRepresentationVariables)
//              .build();
//
//      QueryExecutor queryExecutor = entityExtensionMetadata.getServiceProvider();
//      return queryExecutor
//          .query(entityQuery.createExecutionInput(), graphQLContext)
//          .thenApply(queryResponseModifier::modify)
//          .thenApply(
//              result -> {
//                keyRepresentationVariables.putAll(result.getData());
//                return keyRepresentationVariables;
//              });
//    }
//  }
//
//  private InlineFragment createEntityRequestInlineFragment(
//      EntityBatchLoadingEnvironment batchLoadingEnvironment) {
//    String entityTypeName = entityExtensionMetadata.getTypeName();
//    DataFetchingEnvironment dfe = batchLoadingEnvironment.getDataFetchingEnvironment();
//    Field originalField = dfe.getField();
//    Field __typenameField =
//        Field.newField().name(Introspection.TypeNameMetaFieldDef.getName()).build();
//
//    SelectionSet fieldSelectionSet = dfe
//            .getField()
//            .getSelectionSet()
//            .transform(builder -> builder.selection(__typenameField));
//
//    InlineFragment.Builder inlineFragmentBuilder = InlineFragment.newInlineFragment();
//    inlineFragmentBuilder.typeCondition(TypeName.newTypeName().name(entityTypeName).build());
//    inlineFragmentBuilder.selectionSet(
//        SelectionSet.newSelectionSet()
//            .selection(
//                Field.newField()
//                    .selectionSet(fieldSelectionSet)
//                    .name(originalField.getName())
//                    .build())
//            .build());
//    return inlineFragmentBuilder.build();
//  }
//
//  private Map<String, Object> createKeyRepresentationVariables(
//      EntityBatchLoadingEnvironment batchLoadingEnvironment) {
//    String entityTypeName = entityExtensionMetadata.getTypeName();
//    Map<String, Object> entityRepresentation = new HashMap<>();
//    entityRepresentation.put(Introspection.TypeNameMetaFieldDef.getName(), entityTypeName);
//
//    Map<String, Object> dataSource =
//        batchLoadingEnvironment.getDataFetchingEnvironment().getSource();
//    EntityExtensionMetadata entityExtensionMetadata =
//        batchLoadingEnvironment.getEntityExtensionContext().getEntityExtensionMetadata();
//    entityExtensionMetadata.getKeyDirectives().stream()
//        .flatMap(keyDirectiveMetadata -> keyDirectiveMetadata.getKeyFieldNames().stream())
//        .forEach(
//            keyFieldName -> entityRepresentation.put(keyFieldName, dataSource.get(keyFieldName)));
//
//    return entityRepresentation;
//  }
//
//}
