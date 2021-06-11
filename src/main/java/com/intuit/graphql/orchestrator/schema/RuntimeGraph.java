package com.intuit.graphql.orchestrator.schema;

import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import org.dataloader.BatchLoader;

/**
 * Runtime graph represents the runtime elements required to build the runtime graphql schema. It also contains
 * batchloaders for optimization.
 */
@Getter
public class RuntimeGraph {

  private final Map<Operation, GraphQLObjectType> operationMap;
  private final GraphQLCodeRegistry.Builder codeRegistry;
  private final Map<String, BatchLoader> batchLoaderMap;
  private final Map<String, GraphQLType> additionalTypes;
  private final Set<GraphQLDirective> addtionalDirectives;
  private final Map<String, GraphQLDirective> schemaDirectives;
  private final Map<String, GraphQLType> graphQLtypes;
  private GraphQLSchema executableSchema;


  private RuntimeGraph(Builder builder) {
    operationMap = builder.operationMap;
    codeRegistry = builder.codeRegistry;
    batchLoaderMap = builder.batchLoaderMap;
    additionalTypes = builder.additionalTypes;
    addtionalDirectives = builder.additionalDirectives;
    schemaDirectives = builder.schemaDirectives;
    graphQLtypes = builder.graphQLtypes;
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
   * Empty runtime graph.
   *
   * @return the runtime graph
   */
  public static RuntimeGraph emptyGraph() {

    Map<Operation, GraphQLObjectType> newMap = new EnumMap<>(Operation.class);
    for (Operation op : Operation.values()) {
      newMap.put(op, op.asGraphQLObjectType());
    }
    return new Builder()
        .operationMap(newMap)
        .build();
  }

  public GraphQLType getType(String name) {
    return graphQLtypes.getOrDefault(name, additionalTypes.get(name));
  }

  /**
   * Gets operation.
   *
   * @param operation the operation
   * @return the operation
   */
  public GraphQLObjectType getOperation(Operation operation) {
    return operationMap.get(operation);
  }


  public GraphQLSchema getExecutableSchema() {
    if (Objects.isNull(executableSchema)) {
      executableSchema = makeExecutableSchema();
    }
    return executableSchema;
  }

  /**
   * Make executable schema graph ql schema.
   *
   * @return the graphql schema
   */
  private GraphQLSchema makeExecutableSchema() {
    return GraphQLSchema.newSchema()
        .query(operationMap.get(Operation.QUERY))
        .mutation(operationMap.get(Operation.MUTATION))
        .codeRegistry(codeRegistry.build())
        .additionalTypes(new HashSet<>(additionalTypes.values()))
        .additionalDirectives(addtionalDirectives)
        .withSchemaDirectives(schemaDirectives.values().toArray(new GraphQLDirective[0]))
        .build();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {

    private Map<Operation, GraphQLObjectType> operationMap = new EnumMap<>(Operation.class);
    private Map<String, GraphQLType> additionalTypes = new HashMap<>();
    private Set<GraphQLDirective> additionalDirectives = new HashSet<>();
    private Map<String, GraphQLDirective> schemaDirectives = new HashMap<>();
    private GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();
    private Map<String, BatchLoader> batchLoaderMap = new HashMap<>();
    private Map<String, GraphQLType> graphQLtypes = new HashMap<>();

    private Builder() {
    }

    /**
     * Operation map builder.
     *
     * @param operationMap the operation map
     * @return the builder
     */
    public Builder operationMap(Map<Operation, GraphQLObjectType> operationMap) {
      this.operationMap = operationMap;
      return this;
    }

    /**
     * Additional Types
     *
     * @param additionalTypes the operation map
     * @return the builder
     */
    public Builder additionalTypes(Map<String, GraphQLType> additionalTypes) {
      this.additionalTypes.putAll(additionalTypes);
      return this;
    }

    /**
     * Object Types
     *
     * @param graphQLtypes the types map
     * @return the builder
     */
    public Builder objectTypes(Map<String, GraphQLType> graphQLtypes) {
      this.graphQLtypes.putAll(graphQLtypes);
      return this;
    }

    /**
     * Addition Directives
     *
     * @param additionalDirectives the additional directives set
     * @return the builder
     */
    public Builder additionalDirectives(Set<GraphQLDirective> additionalDirectives) {
      this.additionalDirectives.addAll(additionalDirectives);
      return this;
    }

    /**
     * Code registry builder.
     *
     * @param codeRegistry the code registry
     * @return the builder
     */
    public Builder codeRegistry(GraphQLCodeRegistry.Builder codeRegistry) {
      this.codeRegistry = codeRegistry;
      return this;
    }

    /**
     * Batch loader builder.
     *
     * @param batchLoaderMap map of batchloaders
     * @return the builder
     */
    public Builder batchLoaders(Map<String, BatchLoader> batchLoaderMap) {
      this.batchLoaderMap.putAll(batchLoaderMap);
      return this;
    }

    /**
     * Build runtime graph.
     *
     * @return the runtime graph
     */
    public RuntimeGraph build() {
      return new RuntimeGraph(this);
    }
  }
}
