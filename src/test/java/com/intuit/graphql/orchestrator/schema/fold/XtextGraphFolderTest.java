package com.intuit.graphql.orchestrator.schema.fold;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException;
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLObjectType;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XtextGraphFolderTest {

  @Test
  public void testScalarTypeConflictsGettingIgnoredXtextStrategy() {
    ServiceProvider serviceProvider1 = new GenericTestService("SomeService1",
            "schema { query: Query } type Query { a: Date } scalar Date");

    ServiceProvider serviceProvider2 = new GenericTestService("SomeService2",
            "schema { query: Query } type Query { b: Date } scalar Date");

    RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder()
            .services(Arrays.asList(new ServiceProvider[]{serviceProvider1, serviceProvider2})).build().stitchGraph();

    GraphQLObjectType query = runtimeGraph.getOperationMap().get(Operation.QUERY);
    Assertions.assertThat(query).isNotNull();

    Assertions.assertThat(query.getFieldDefinition("b")).isNotNull();

    Assertions.assertThat(((GraphQLNamedType) query.getFieldDefinition("b").getType()).getName())
            .isEqualTo("Date");
  }

  @Test
  public void testGoldenTypeSignatureConflictThrowsException() {
    ServiceProvider serviceProvider1 = new GenericTestService("SomeService1",
            "schema { query: Query } type Query { a: PageInfo } type PageInfo { id: String }");

    ServiceProvider serviceProvider2 = new GenericTestService("SomeService2",
            "schema { query: Query } type Query { b: PageInfo } type PageInfo { id: String! }");

    SchemaStitcher schemaStitcher = SchemaStitcher.newBuilder()
            .services(Arrays.asList(new ServiceProvider[]{serviceProvider1, serviceProvider2})).build();

    assertThatThrownBy(() -> schemaStitcher.stitchGraph()).isInstanceOf(TypeConflictException.class);
  }

  @Test
  public void testGoldenInterfaceConflictThrowsException() {
    ServiceProvider serviceProvider1 = new GenericTestService("SomeService1",
            "schema { query: Query } type Query { a: Node } interface Node { id: String! }");

    ServiceProvider serviceProvider2 = new GenericTestService("SomeService2",
            "schema { query: Query } type Query { b: Node } interface Node { id: String }");

    SchemaStitcher schemaStitcher = SchemaStitcher.newBuilder()
            .services(Arrays.asList(new ServiceProvider[]{serviceProvider1, serviceProvider2})).build();

    assertThatThrownBy(() -> schemaStitcher.stitchGraph()).isInstanceOf(TypeConflictException.class);
  }

  static class GenericTestService implements ServiceProvider {

    private final String schema;
    private final String namespace;
    private final Set<String> domainTypes;

    GenericTestService(String namespace, String schema) {
      this.namespace = namespace;
      this.schema = schema;
      this.domainTypes = null;
    }

    GenericTestService(String namespace, Set<String> domainTypes, String schema) {
      this.namespace = namespace;
      this.schema = schema;
      this.domainTypes = domainTypes;
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
      throw new RuntimeException("This should not be called in this test case.");
    }
  }
}
