package com.intuit.graphql.orchestrator.xtext;

import com.intuit.graphql.graphQL.*;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
import com.intuit.graphql.orchestrator.metadata.RenamedMetadata;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.TypeMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.utils.XtextTypeUtils;
import lombok.Getter;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Runtime graph represents the runtime elements required to build the runtime graphql schema. It also contains
 * batchloaders for optimization.
 */
@Getter
public class UnifiedXtextGraph {

  private Map<Operation, ObjectTypeDefinition> operationMap;
  private final Map<FieldContext, DataFetcherContext> codeRegistry;
  private final Map<FieldContext, ArgumentsDefinition> resolverArgumentFields;
  private final Set<DirectiveDefinition> directives;
  private final Map<String, TypeDefinition> types;
  private final Map<String, TypeMetadata> typeMetadatas;
  private final List<FieldResolverContext> fieldResolverContexts;

  private final boolean hasInterfaceOrUnion;
  private final boolean hasFieldResolverDefinition;

  //to be computed and cached
  private boolean cacheComputed = false;

  /**
   * Container that holds all Object Type Definitions for a Graph. Input Object Type Definitions are expected to be
   * cached and computed in a separate map.
   */
  private Map<String, ObjectTypeDefinition> objectTypeDefinitions;
  private Map<String, TypeDefinition> valueTypesByName;
  private final Map<String, TypeDefinition> entitiesByTypeName;
  private final Map<String, Map<String, TypeSystemDefinition>> entityExtensionsByNamespace;
  private final List<EntityExtensionMetadata> entityExtensionMetadatas;
  private final Map<String, FederationMetadata> federationMetadataByNamespace;
  private final Map<String, RenamedMetadata> renamedMetadataByNamespace;

  UnifiedXtextGraph(Builder builder) {
    //TODO: Research on all Providers having an XtextResource instead of a ResourceSet
    operationMap = builder.operationMap;
    codeRegistry = builder.codeRegistry;
    directives = builder.directives;
    types = builder.types;
    typeMetadatas = builder.typeMetadatas;
    hasInterfaceOrUnion = builder.hasInterfaceOrUnion;
    hasFieldResolverDefinition = builder.hasFieldResolverDefinition;
    resolverArgumentFields = builder.resolverArgumentFields;
    fieldResolverContexts = builder.fieldResolverContexts;
    valueTypesByName = builder.valueTypesByName;
    entitiesByTypeName = builder.entities;
    entityExtensionsByNamespace = builder.entityExtensionsByNamespace;
    entityExtensionMetadatas = builder.entityExtensionMetadatas;
    federationMetadataByNamespace = builder.federationMetadataByNamespace;
    renamedMetadataByNamespace = builder.renamedMetadataByNamespace;
  }

    /**
   * Creates a new Builder
   *
   * @return the Builder
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * Creates a builder from a copy.
   *
   * @param copy the copy
   * @return the builder
   */
  public static Builder newBuilder(UnifiedXtextGraph copy) {
    Builder builder = new Builder();
    builder.operationMap = copy.getOperationMap();
    builder.codeRegistry = copy.getCodeRegistry();
    builder.directives = copy.getDirectives();
    builder.types = copy.getTypes();
    builder.typeMetadatas = copy.getTypeMetadatas();
    builder.hasInterfaceOrUnion = copy.hasInterfaceOrUnion;
    builder.hasFieldResolverDefinition = copy.hasFieldResolverDefinition;
    builder.resolverArgumentFields = copy.resolverArgumentFields;
    builder.fieldResolverContexts = copy.fieldResolverContexts;
    builder.valueTypesByName = copy.valueTypesByName;
    builder.entities = copy.entitiesByTypeName;
    builder.entityExtensionsByNamespace = copy.entityExtensionsByNamespace;
    builder.entityExtensionMetadatas = copy.entityExtensionMetadatas;
    builder.federationMetadataByNamespace = copy.federationMetadataByNamespace;
    builder.renamedMetadataByNamespace = copy.renamedMetadataByNamespace;
    return builder;
  }

  /**
   * Empty Unified Xtext Graph.
   *
   * @return the Unified Xtext graph
   */
  public static UnifiedXtextGraph emptyGraph() {
    Map<Operation, ObjectTypeDefinition> newMap = new EnumMap<>(Operation.class);
    for (Operation op : Operation.values()) {
      newMap.put(op, op.asObjectTypeDefinition());
    }

    return new Builder()
        .operationMap(newMap)
        .build();
  }

  /**
   * Check if the given provider's schema (contains unions/interfaces and) requires typename to be injected in its
   * queries
   *
   * @return true or false
   */
  public boolean requiresTypenameInjection() {
    return isHasInterfaceOrUnion();
  }

  public TypeDefinition getType(final NamedType namedType) {
    return types.get(XtextTypeUtils.typeName(namedType));
  }

  public TypeDefinition getType(final String typeName) {
    return types.get(typeName);
  }

  public void addType(TypeDefinition type) {
    this.types.put(type.getName(), type);
  }

  public TypeDefinition removeType(TypeDefinition type) {
    return this.types.remove(type.getName());
  }

  public Map<String, ObjectTypeDefinition> objectTypeDefinitionsByName() {
    computeAndCacheCollections();

    return objectTypeDefinitions;
  }

  public Map<String, TypeDefinition> getEntitiesByTypeName() {
    return entitiesByTypeName;
  }

  private void computeAndCacheCollections() {
    objectTypeDefinitions = new HashMap<>();
    for (final Entry<String, TypeDefinition> entry : types.entrySet()) {
      String name = entry.getKey();
      TypeDefinition typeDef = entry.getValue();

      if (typeDef instanceof ObjectTypeDefinition) {
        objectTypeDefinitions.put(name, (ObjectTypeDefinition) typeDef);
      }
    }

    ObjectTypeDefinition queryType = operationMap.get(Operation.QUERY);
    objectTypeDefinitions.put(queryType.getName(), queryType);

    cacheComputed = true;
  }

  /**
   * Gets operation.
   *
   * @param operation the operation
   * @return the operation
   */
  public ObjectTypeDefinition getOperationType(Operation operation) {
    return operationMap.get(operation);
  }

  public Operation getOperation(String typename) {
    return operationMap.entrySet()
        .stream()
        .filter(entry -> entry.getValue().getName().equals(typename))
        .map(Entry::getKey)
        .findAny().orElse(null);
  }

  public boolean isOperationType(TypeDefinition typeDefinition) {
    return operationMap.containsValue(typeDefinition);
  }

  /**
   * Transform runtime graph.
   *
   * @param builderConsumer the builder consumer
   * @return the runtime graph
   */
  public UnifiedXtextGraph transform(Consumer<Builder> builderConsumer) {
    Builder builder = newBuilder(this);
    builderConsumer.accept(builder);
    return builder.build();
  }

  public Map<String, Map<String, TypeSystemDefinition>> getEntityExtensionsByNamespace() {
    return this.entityExtensionsByNamespace;
  }

  public void addToEntityExtensionMetadatas(EntityExtensionMetadata entityExtensionMetadatas) {
    this.entityExtensionMetadatas.add(entityExtensionMetadatas);
  }


  /**
   * The type Builder.
   */
  public static class Builder {

    private Map<Operation, ObjectTypeDefinition> operationMap = new HashMap<>();
    private Map<FieldContext, DataFetcherContext> codeRegistry = new HashMap<>();
    private Map<FieldContext, ArgumentsDefinition> resolverArgumentFields = new HashMap<>();
    private Set<DirectiveDefinition> directives = new HashSet<>();
    private Map<String, TypeDefinition> types = new HashMap<>();
    private Map<String, TypeMetadata> typeMetadatas = new HashMap<>();
    private Map<String, TypeDefinition> valueTypesByName = new HashMap<>();
    private Map<String, TypeDefinition> entities = new HashMap<>();
    private Map<String, Map<String, TypeSystemDefinition>> entityExtensionsByNamespace = new HashMap<>();
    private List<EntityExtensionMetadata> entityExtensionMetadatas = new ArrayList<>();
    private List<FieldResolverContext> fieldResolverContexts = new ArrayList<>();
    private Map<String, FederationMetadata> federationMetadataByNamespace = new HashMap<>();
    private Map<String, RenamedMetadata> renamedMetadataByNamespace = new HashMap<>();

    private boolean hasInterfaceOrUnion = false;
    private boolean hasFieldResolverDefinition = false;

    Builder() {
    }

    /**
     * Operation map builder.
     *
     * @param operationMap the operation map
     * @return the builder
     */
    public Builder operationMap(Map<Operation, ObjectTypeDefinition> operationMap) {
      this.operationMap.putAll(operationMap);
      return this;
    }

    /**
     * Object Types
     *
     * @param codeRegistry the codeRegistry map
     * @return the builder
     */
    public Builder codeRegistry(Map<FieldContext, DataFetcherContext> codeRegistry) {
      this.codeRegistry.putAll(codeRegistry);
      return this;
    }

    public Builder dataFetcherContext(FieldContext fieldCoordinate, DataFetcherContext dataFetcherContext) {
      this.codeRegistry.put(requireNonNull(fieldCoordinate), requireNonNull(dataFetcherContext));
      return this;
    }

    /**
     * Query builder.
     *
     * @param query the query
     * @return the builder
     */
    public Builder query(ObjectTypeDefinition query) {
      this.operationMap.put(Operation.QUERY, requireNonNull(query));
      return this;
    }

    /**
     * Mutation builder.
     *
     * @param mutation the mutation
     * @return the builder
     */
    public Builder mutation(ObjectTypeDefinition mutation) {
      this.operationMap.put(Operation.MUTATION, requireNonNull(mutation));
      return this;
    }

    /**
     * Directives builder.
     *
     * @param directiveDefinitions the set of directives
     * @return the builder
     */
    public Builder directives(Set<DirectiveDefinition> directiveDefinitions) {
      requireNonNull(directiveDefinitions);
      this.directives.addAll(directiveDefinitions);
      return this;
    }

    /**
     * Types builder.
     *
     * @param types the map of xtext graphql types
     * @return the builder
     */
    public Builder types(Map<String, TypeDefinition> types) {
      requireNonNull(types);
      this.types.putAll(types);
      return this;
    }

    /**
     * Types builder.
     *
     * @param typeMetadatas the map of TypeMetadata
     * @return the builder
     */
    public Builder typeMetadatas(Map<String, TypeMetadata> typeMetadatas) {
      requireNonNull(typeMetadatas);
      this.typeMetadatas.putAll(typeMetadatas);
      return this;
    }

    /**
     * isHasInterfaceOrUnion builder.
     *
     * @param hasInterfaceOrUnion boolean value
     * @return the builder
     */
    public Builder hasInterfaceOrUnion(boolean hasInterfaceOrUnion) {
      this.hasInterfaceOrUnion = hasInterfaceOrUnion;
      return this;
    }

    /**
     * hasFieldResolverDefinition builder.
     *
     * @param hasFieldResolverDefinition boolean value
     * @return the builder
     */
    public Builder hasFieldResolverDefinition(boolean hasFieldResolverDefinition) {
      this.hasFieldResolverDefinition = hasFieldResolverDefinition;
      return this;
    }

    public Builder fieldResolverContexts(List<FieldResolverContext> fieldResolverContexts) {
      this.fieldResolverContexts.addAll(fieldResolverContexts);
      return this;
    }

    public Builder clearFieldResolverContexts() {
      this.fieldResolverContexts.clear();
      return this;
    }

    public Builder valueTypesByName(Map<String, TypeDefinition> valueTypes) {
      requireNonNull(valueTypes);
      this.valueTypesByName.putAll(valueTypes);
      return this;
    }

    public Builder entitiesByTypeName(Map<String, TypeDefinition> entities) {
      requireNonNull(entities);
      this.entities.putAll(entities);
      return this;
    }

    public Builder entityExtensionsByNamespace(
        Map<String, Map<String, TypeSystemDefinition>> entityExtensionsByNamespace) {
      requireNonNull(entityExtensionsByNamespace);
      this.entityExtensionsByNamespace.putAll(entityExtensionsByNamespace);
      return this;
    }

    public Builder federationMetadataByNamespace(Map<String, FederationMetadata> federationMetadataByNamespace) {
      requireNonNull(federationMetadataByNamespace);
      this.federationMetadataByNamespace.putAll(federationMetadataByNamespace);
      return this;
    }

    public Builder entityExtensionMetadatas(List<EntityExtensionMetadata> entityExtensionMetadatas) {
      requireNonNull(entityExtensionMetadatas);
      this.entityExtensionMetadatas.addAll(entityExtensionMetadatas);
      return this;
    }

    public Builder renamedMetadataByNamespace(Map<String, RenamedMetadata> renamedMetadataByNamespace) {
      requireNonNull(renamedMetadataByNamespace);
      this.renamedMetadataByNamespace.putAll(renamedMetadataByNamespace);
      return this;
    }

    /**
     * Build runtime graph.
     *
     * @return the runtime graph
     */
    public UnifiedXtextGraph build() {
      return new UnifiedXtextGraph(this);
    }
  }
}
