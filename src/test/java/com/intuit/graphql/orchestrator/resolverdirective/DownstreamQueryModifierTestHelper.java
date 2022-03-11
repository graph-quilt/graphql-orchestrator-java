package com.intuit.graphql.orchestrator.resolverdirective;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLSchema;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

@Getter
public class DownstreamQueryModifierTestHelper {

  public static final String aSchema = "type Query { a1 : [AObjectType] a2 : String } "
      + "type AObjectType { af1 : String af2 : String } "
      + "extend type AObjectType { "
      + "    id : String"
      + "    reqdField : String"
      + "    b1 : BObjectType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
      + "    b2 : BInterfaceType @resolver(field: \"b2\" arguments: [{name : \"id\", value: \"$af1\"}]) "
      + "    b3 : BUnionType @resolver(field: \"b3\" arguments: [{name : \"id\", value: \"$af1\"}]) "
      + "    b4 : String @resolver(field: \"b4\" arguments: [{name : \"id\", value: \"$af1\"}]) "
      + "    b5 : BEnumType @resolver(field: \"b5\" arguments: [{name : \"id\", value: \"$af1\"}]) "
      + "    b6 : String "
      + "} "
      + "type BObjectType "
      + "interface BInterfaceType "
      + "union BUnionType "
      + "enum BEnumType "
      + "directive @resolver(field: String, arguments: [ResolverArgument!]) on FIELD_DEFINITION "
      + "input ResolverArgument { name : String! value : String! }\n";

  public static final String bSchema = "type Query { "
      + "    b1 (id : String) : BObjectType "
      + "    b2 (id : String) : BInterfaceType "
      + "    b3 (id : String) : BUnionType "
      + "    b4 (id : String) : String "
      + "    b5 (id : String) : BEnumType "
      + "} "
      + "type BObjectType { bf1 : String } "
      + "interface BInterfaceType { commonField : String } "
      + "type B1 implements  BInterfaceType { commonField : String b1Field : String } "
      + "type B2 implements  BInterfaceType { commonField : String b2Field : String } "
      + "union BUnionType = B1 | B2 "
      + "enum BEnumType { ONE, TWO }";

  private GraphQLSchema graphQLSchema;
  private RuntimeGraph runtimeGraph;
  private DataLoaderRegistry dataLoaderRegistry;

  private ServiceProvider testServiceA;
  private ServiceProvider testServiceB;

  public DownstreamQueryModifierTestHelper(ServiceProvider testServiceA, ServiceProvider testServiceB) {
    Objects.requireNonNull(testServiceA);
    Objects.requireNonNull(testServiceB);
    this.testServiceA = testServiceA;
    this.testServiceB = testServiceB;
    this.runtimeGraph = SchemaStitcher.newBuilder().service(testServiceA).service(testServiceB).build()
        .stitchGraph();
    this.graphQLSchema = runtimeGraph.getExecutableSchema();
    this.dataLoaderRegistry = createDataLoaderRegistry();
  }

  public GraphQLFieldDefinition getFieldDefinition(String parentTypeName,
      String fieldName) {
    GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) graphQLSchema.getType(parentTypeName);
    return parentType.getFieldDefinition(fieldName);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private  DataLoaderRegistry createDataLoaderRegistry() {
    Objects.requireNonNull(runtimeGraph);
    final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();

    final Map<BatchLoader, DataLoader> temporaryMap = this.runtimeGraph.getBatchLoaderMap().values().stream().distinct()
        .collect(Collectors.toMap(Function.identity(), DataLoader::new));

    this.runtimeGraph.getBatchLoaderMap()
        .forEach((key, batchLoader) ->
            dataLoaderRegistry.register(key, temporaryMap.getOrDefault(batchLoader, new DataLoader(batchLoader))));
    return dataLoaderRegistry;
  }

  public static class TestService implements ServiceProvider {

    private final String schema;
    private final String namespace;
    private final Set<String> domainTypes;
    private final Map<String, Object> response;

    public TestService(String namespace, String schema, Map<String, Object> response) {
      this.namespace = namespace;
      this.schema = schema;
      this.domainTypes = null;
      this.response = response;
    }

    @Override
    public String getNameSpace() {
      return namespace;
    }

    @Override
    public Map<String, String> sdlFiles() {
      return ImmutableMap.of("schema.graphqls", this.schema);
    }

    @Override
    public Set<String> domainTypes() {
      return this.domainTypes;
    }

    @Override
    public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput, GraphQLContext context) {
      Map<String, Object> data = new HashMap<>();
      Document document = (Document) executionInput.getRoot();
      OperationDefinition opDep = (OperationDefinition) document.getDefinitions().get(0);
      opDep.getSelectionSet().getSelections().forEach(selection -> {
        Field field = (Field) selection;
        data.put(field.getName(), response.get(field.getName()));
      });
      return CompletableFuture.completedFuture(ImmutableMap.of("data", data));
    }
  }

}
