package com.intuit.graphql.orchestrator.stitching;

import static com.intuit.graphql.orchestrator.batch.DataLoaderKeyUtil.createDataLoaderKey;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.RESOLVER_ARGUMENT_INPUT_NAME;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.RESOLVER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getAllTypes;
import static com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType.ENTITY_FETCHER;
import static com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType.RESOLVER_ARGUMENT;
import static com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType.RESOLVER_ON_FIELD_DEFINITION;
import static com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType.SERVICE;
import static com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType.STATIC;
import static graphql.schema.FieldCoordinates.coordinates;
import static java.util.Objects.requireNonNull;

import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.graphql.orchestrator.batch.BatchLoaderExecutionHooks;
import com.intuit.graphql.orchestrator.batch.DataLoaderKeyUtil;
import com.intuit.graphql.orchestrator.batch.EntityFetcherBatchLoader;
import com.intuit.graphql.orchestrator.batch.FieldResolverBatchLoader;
import com.intuit.graphql.orchestrator.batch.GraphQLServiceBatchLoader;
import com.intuit.graphql.orchestrator.datafetcher.FieldResolverDirectiveDataFetcher;
import com.intuit.graphql.orchestrator.datafetcher.ResolverArgumentDataFetcher;
import com.intuit.graphql.orchestrator.datafetcher.RestDataFetcher;
import com.intuit.graphql.orchestrator.datafetcher.ServiceDataFetcher;
import com.intuit.graphql.orchestrator.federation.EntityDataFetcher;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDirective;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentQueryBuilder;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.schema.ServiceMetadataImpl;
import com.intuit.graphql.orchestrator.schema.fold.UnifiedXtextGraphFolder;
import com.intuit.graphql.orchestrator.schema.transform.AllTypesTransformer;
import com.intuit.graphql.orchestrator.schema.transform.DirectivesTransformer;
import com.intuit.graphql.orchestrator.schema.transform.DomainTypesTransformer;
import com.intuit.graphql.orchestrator.schema.transform.FederationTransformerPostMerge;
import com.intuit.graphql.orchestrator.schema.transform.FederationTransformerPreMerge;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMerge;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPreMerge;
import com.intuit.graphql.orchestrator.schema.transform.GraphQLAdapterTransformer;
import com.intuit.graphql.orchestrator.schema.transform.RenameTransformer;
import com.intuit.graphql.orchestrator.schema.transform.ResolverArgumentTransformer;
import com.intuit.graphql.orchestrator.schema.transform.Transformer;
import com.intuit.graphql.orchestrator.schema.transform.TypeExtensionTransformer;
import com.intuit.graphql.orchestrator.schema.transform.UnionAndInterfaceTransformer;
import com.intuit.graphql.orchestrator.utils.XtextToGraphQLJavaVisitor;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder;
import graphql.execution.DataFetcherResult;
import graphql.language.OperationDefinition;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.StaticDataFetcher;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.dataloader.BatchLoader;

/**
 * The type Xtext stitcher.
 */
public class XtextStitcher implements Stitcher {

  private static final ResolverArgumentQueryBuilder queryBuilder = new ResolverArgumentQueryBuilder();

  private final List<Transformer<XtextGraph, XtextGraph>> preMergeTransformers;
  private final List<Transformer<UnifiedXtextGraph, UnifiedXtextGraph>> postMergeTransformers;
  private final BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> batchLoaderHooks;

  private XtextStitcher(final Builder builder) {
    preMergeTransformers = builder.preMergeTransformers;
    postMergeTransformers = builder.postMergeTransformers;
    batchLoaderHooks = builder.batchLoaderHooks;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * Uses Graph Transformers to transform and enrich provider schemas. These transformed graphs are stitched into a
   * single graph, which is then used to create an executable RuntimeGraph.
   *
   * @param serviceProviders the service contexts
   * @return runtime graph
   */
  @Override
  public RuntimeGraph stitch(List<ServiceProvider> serviceProviders) {

    //Transform Graphs
    Map<String, XtextGraph> xtextGraphMap = serviceProviders.stream()
        .map(XtextGraphBuilder::build)
        .map(xtextGraph -> transform(xtextGraph, preMergeTransformers))
        .peek(this::validateGraph)
        .collect(Collectors.toMap(graph -> graph.getServiceProvider().getNameSpace(), Function.identity(),
            (g1, g2) -> {
              throw new StitchingException(
                  String.format("Duplicate Namespace: %s", g1.getServiceProvider().getNameSpace()));
            }));

    //Stitch Graphs
    UnifiedXtextGraph stitchedGraph = new UnifiedXtextGraphFolder().fold(UnifiedXtextGraph.emptyGraph(), xtextGraphMap.values());

    //Service Metadata
    final Map<String, ServiceMetadata> serviceMetadataMap = xtextGraphMap.values().stream()
        .map(this::buildServiceMetadata)
        .collect(Collectors.toMap(metadata -> metadata.getServiceProvider().getNameSpace(), Function.identity()));

    //Transform after merge
    UnifiedXtextGraph stitchedTransformedGraph = transform(stitchedGraph, postMergeTransformers);

    //Executable RuntimeGraph with BatchLoaders and DataFetchers
    final Map<String, BatchLoader> batchLoaders = getBatchLoaders(serviceMetadataMap);

    stitchedTransformedGraph.getFieldResolverContexts().forEach(fieldResolverContext -> {
      FieldResolverBatchLoader fieldResolverDataLoader = FieldResolverBatchLoader
          .builder()
          .fieldResolverContext(fieldResolverContext)
          .serviceMetadata(serviceMetadataMap.get(fieldResolverContext.getTargetServiceNamespace()))
          .build();

      String batchLoaderKey = DataLoaderKeyUtil.createDataLoaderKeyFrom(fieldResolverContext);
      batchLoaders.put(batchLoaderKey, fieldResolverDataLoader);

    });

    stitchedGraph.getEntityExtensionMetadatas().forEach(metadata ->
      metadata.getRequiredFieldsByFieldName().forEach((fieldName, fields) -> {
        EntityFetcherBatchLoader entityFetcherBatchLoader = new EntityFetcherBatchLoader(
                metadata,
                serviceMetadataMap.get(metadata.getServiceProvider().getNameSpace()),
                fieldName
        );

        String batchLoaderKey = createDataLoaderKey(metadata.getTypeName(), fieldName);
        batchLoaders.put(batchLoaderKey, entityFetcherBatchLoader);
      })
    );

    final GraphQLCodeRegistry.Builder codeRegistryBuilder = getCodeRegistry(stitchedTransformedGraph,
        serviceMetadataMap);

    final RuntimeGraph.Builder runtimeGraphBuilder = createRuntimeGraph(stitchedTransformedGraph);

    RuntimeGraph runtimeGraph = runtimeGraphBuilder
        .batchLoaders(batchLoaders)
        .codeRegistry(codeRegistryBuilder)
        .build();

    runtimeGraph.getExecutableSchema();

    return runtimeGraph;
  }

  private void validateGraph(XtextGraph xtextGraph) {
    boolean emptyGraph = xtextGraph.getOperationMap().values()
            .stream()
            .map(ObjectTypeDefinition::getFieldDefinition)
            .allMatch(List::isEmpty);

    if(emptyGraph &&
        !(xtextGraph.getServiceProvider().isFederationProvider() &&
                getAllTypes(xtextGraph.getXtextResourceSet()).findAny().isPresent()
        )
    ) {
      throw new StitchingException(
              String.format("%s graph is invalid. Graph cannot be empty.", xtextGraph.getServiceProvider().getNameSpace())
      );
    }
  }

  /**
   * Creates a namespace vs batch loader map for corresponding data providers per graph
   *
   * @param serviceMetadataMap map of namespace vs xtext graph for all data providers
   * @return map of namespace vs batch loader
   */
  private Map<String, BatchLoader> getBatchLoaders(Map<String, ServiceMetadata> serviceMetadataMap) {

    //created an entity batch loader here
    HashMap<String, BatchLoader> batchLoaderMap = new HashMap<>();
    serviceMetadataMap.forEach((namespace, serviceMetadata) -> {
      if (serviceMetadata.getServiceProvider().getSeviceType() == ServiceType.GRAPHQL || serviceMetadata
          .getServiceProvider()
          .isFederationProvider()) {
        batchLoaderMap.put(namespace,
            GraphQLServiceBatchLoader
                .newQueryExecutorBatchLoader()
                .queryExecutor(serviceMetadata.getServiceProvider())
                .serviceMetadata(serviceMetadata)
                .batchLoaderExecutionHooks(batchLoaderHooks)
                .build());
      }
    });
    return batchLoaderMap;
  }

  private ServiceMetadata buildServiceMetadata(XtextGraph xtextGraph) {
    return ServiceMetadataImpl.newBuilder()
        .serviceProvider(xtextGraph.getServiceProvider())
        .typeMetadataMap(xtextGraph.getTypeMetadatas())
        .federationMetadata(xtextGraph.getFederationServiceMetadata())
        .hasFieldResolverDefinition(xtextGraph.isHasFieldResolverDefinition())
        .hasInterfaceOrUnion(xtextGraph.isHasInterfaceOrUnion())
        .renamedMetadata(xtextGraph.getRenamedMetadata())
        .build();
  }

  /**
   * Builds GraphQL Code Registry for a unified xtext graph using field context and data fetcher context
   *
   * @param mergedGraph the post-merged and post-transformed graph
   * @param serviceMetadataMap the individual graphs that were used to build the merged graph
   * @return GraphQL Code Registry Builder
   */
  private GraphQLCodeRegistry.Builder getCodeRegistry(UnifiedXtextGraph mergedGraph,
                                                      Map<String, ServiceMetadata> serviceMetadataMap) {

    GraphQLCodeRegistry.Builder builder = GraphQLCodeRegistry.newCodeRegistry();

    mergedGraph.getCodeRegistry().forEach((fieldContext, dataFetcherContext) -> {
      FieldCoordinates coordinates = coordinates(fieldContext.getParentType(), fieldContext.getFieldName());
      DataFetcherType type = dataFetcherContext.getDataFetcherType();

      if (type == STATIC) {
        builder.dataFetcher(coordinates, new StaticDataFetcher(Collections.emptyMap()));
      } else if (type == SERVICE) {
        final ServiceMetadata serviceMetadata = serviceMetadataMap.get(dataFetcherContext.getNamespace());

        builder.dataFetcher(coordinates,
            dataFetcherContext.getServiceType() == ServiceType.REST
                ? new RestDataFetcher(serviceMetadata)
                : new ServiceDataFetcher(serviceMetadata)
        );
      } else if (type == RESOLVER_ARGUMENT) {
        final XtextToGraphQLJavaVisitor visitor = XtextToGraphQLJavaVisitor.newBuilder().build();

        final List<GraphQLArgument> graphqlArguments = visitor
            .createGraphqlArguments(mergedGraph.getResolverArgumentFields().get(fieldContext));

        Map<ResolverArgumentDirective, OperationDefinition> map = graphqlArguments.stream()
            .map(ResolverArgumentDirective::fromGraphQLArgument)
            .collect(
                Collectors.toMap(Function.identity(), d -> queryBuilder.buildQuery(d.getField(), d.getInputType())));

        builder.dataFetcher(coordinates, ResolverArgumentDataFetcher.newBuilder()
            .namespace(dataFetcherContext.getNamespace())
            .queriesByResolverArgument(map)
            .serviceType(dataFetcherContext.getServiceType())
            .build()
        );
      } else if (type == RESOLVER_ON_FIELD_DEFINITION) {
        builder.dataFetcher(coordinates, FieldResolverDirectiveDataFetcher.from(dataFetcherContext)
        );
      } else if (type == ENTITY_FETCHER && mergedGraph.getType(fieldContext.getParentType()) != null) {
        EntityDataFetcher entityDataFetcher = new EntityDataFetcher(dataFetcherContext.getEntityExtensionMetadata().getTypeName(),
          dataFetcherContext.getNamespace(), dataFetcherContext.getServiceType());
        builder.dataFetcher(coordinates, entityDataFetcher);
      }
    });
    return builder;
  }

  /**
   * Creates a {@link RuntimeGraph} from an {@link XtextGraph} by converting xtext AST to GraphQL Java AST
   *
   * @param unifiedXtextGraph unified xtext graph
   * @return RuntimeGraph Builder
   */
  private RuntimeGraph.Builder createRuntimeGraph(UnifiedXtextGraph unifiedXtextGraph) {

    XtextToGraphQLJavaVisitor visitor = XtextToGraphQLJavaVisitor.newBuilder()
            .graphqlBlackList(unifiedXtextGraph.getBlacklistedTypes())
            .build();
    //fill operations
    final Map<Operation, GraphQLObjectType> operationMap = new EnumMap<>(Operation.class);

    unifiedXtextGraph.getOperationMap()
        .forEach((operation, objectTypeDefinition) ->
            operationMap.put(operation, (GraphQLObjectType) visitor.doSwitch(objectTypeDefinition)));

    return RuntimeGraph.newBuilder()
        .operationMap(operationMap)
        .objectTypes(visitor.getGraphQLObjectTypes())
        .additionalTypes(getAdditionalTypes(unifiedXtextGraph, visitor.getGraphQLObjectTypes()))
        .additionalDirectives(getAdditionalDirectives(unifiedXtextGraph, visitor.getDirectiveDefinitions()));
  }

  /**
   * Collects additional types (not referenced from schema top level) by filtering visited types from all types in
   * schema.
   *
   * @param unifiedXtextGraph the unified xtext graph
   * @param visited types referenced from schema top level
   * @return map of type names to additional types
   */
  private Map<String, GraphQLType> getAdditionalTypes(UnifiedXtextGraph unifiedXtextGraph, Map<String, GraphQLType> visited) {

    XtextToGraphQLJavaVisitor localVisitor = XtextToGraphQLJavaVisitor.newBuilder().graphqlObjectTypes(visited).build();

    return unifiedXtextGraph.getTypes().values().stream()
        .filter(type -> !type.getName().endsWith(RESOLVER_ARGUMENT_INPUT_NAME))
        .filter(type -> !unifiedXtextGraph.isOperationType(type))
        .filter(type -> !(visited.containsKey(type.getName())))
        .collect(Collectors.toMap(TypeDefinition::getName, type -> (GraphQLType) localVisitor.doSwitch(type)));
  }

  private Set<GraphQLDirective> getAdditionalDirectives(UnifiedXtextGraph unifiedXtextGraph, Map<String, GraphQLDirective> visited) {
    XtextToGraphQLJavaVisitor localVisitor = XtextToGraphQLJavaVisitor.newBuilder().directiveDefinitions(visited)
        .build();

    return unifiedXtextGraph.getDirectives().stream()
        .filter(directiveDefinition -> !directiveDefinition.getName().equals(RESOLVER_DIRECTIVE_NAME))
        .map(localVisitor::doSwitch)
        .map(GraphQLDirective.class::cast)
        .collect(Collectors.toSet());
  }

  private <T> T transform(T graph, List<Transformer<T, T>> transformers) {

    for (Transformer<T, T> transformer : transformers) {
      graph = transformer.transform(graph);
    }
    return graph;
  }

  public static final class Builder {

    private List<Transformer<XtextGraph, XtextGraph>> preMergeTransformers = defaultPreMergeTransformers();
    private List<Transformer<UnifiedXtextGraph, UnifiedXtextGraph>> postMergeTransformers = defaultPostMergeTransformers();
    private BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> batchLoaderHooks = BatchLoaderExecutionHooks.DEFAULT_HOOKS;

    /**
     * Provides the default pre-merge transformers that each operate on the individual provider graphs.
     */
    private List<Transformer<XtextGraph, XtextGraph>> defaultPreMergeTransformers() {
      return Arrays.asList(
          new RenameTransformer(),
          new TypeExtensionTransformer(),
          new DomainTypesTransformer(),
          new AllTypesTransformer(),
          new DirectivesTransformer(),
          new UnionAndInterfaceTransformer(),
          new FieldResolverTransformerPreMerge(),
          new FederationTransformerPreMerge()
      );
    }

    /**
     * Provides the default post-merge transformers that operate on the stitched and merged graph.
     */
    private List<Transformer<UnifiedXtextGraph, UnifiedXtextGraph>> defaultPostMergeTransformers() {
      return Arrays.asList(
          new FederationTransformerPostMerge(),
          new ResolverArgumentTransformer(),
          new FieldResolverTransformerPostMerge(),
          new GraphQLAdapterTransformer()
      );
    }

    private Builder() {
    }

    public Builder preMergeTransformers(final List<Transformer<XtextGraph, XtextGraph>> val) {
      preMergeTransformers = requireNonNull(val);
      return this;
    }

    public Builder postMergeTransformers(final List<Transformer<UnifiedXtextGraph, UnifiedXtextGraph>> val) {
      postMergeTransformers = requireNonNull(val);
      return this;
    }

    public Builder batchLoaderHooks(
        final BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> hooks) {
      batchLoaderHooks = requireNonNull(hooks);
      return this;
    }

    public XtextStitcher build() {
      return new XtextStitcher(this);
    }
  }
}
