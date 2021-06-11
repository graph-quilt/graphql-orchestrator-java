package com.intuit.graphql.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher;
import graphql.ExecutionInput;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.ExecutionIdProvider;
import graphql.execution.ExecutionStrategy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class TestCase {

  private final GraphQLOrchestrator orchestrator;
  private ExecutionInput executionInput;
  private Map<String, Object> executionResult;

  private TestCase(List<ServiceProvider> serviceProviders, String query,
      Map<String, Object> variables) {
    ServiceProvider[] services = serviceProviders.toArray(new ServiceProvider[0]);
    this.orchestrator = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
        new AsyncExecutionStrategy(), services);

    ExecutionInput.Builder eiBuilder = ExecutionInput.newExecutionInput();
    eiBuilder.query(query);
    if (Objects.nonNull(variables)) {
      eiBuilder.variables(variables);
    }
    this.executionInput = eiBuilder.build();

  }

  public static Builder newTestCase() {
    return new Builder();
  }

  public void run() throws ExecutionException, InterruptedException {
    this.executionResult = orchestrator.execute(executionInput).get().toSpecification();
  }

  public void assertHasData() {
    assertThat(executionResult.get("data")).isNotNull();
  }

  public void assertHashNoErrors() {
    assertThat(executionResult.get("errors")).isNull();
  }

  public Object getDataField(String fieldName) {
    Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data");
    assertThat(dataValue).containsKeys(fieldName);
    return dataValue.get(fieldName);
  }

  public static class Builder {

    private List<ServiceProvider> serviceProviderList = new ArrayList<>();
    private String query;
    private Map<String, Object> variables;

    Builder service(ServiceProvider serviceProvider) {
      this.serviceProviderList.add(serviceProvider);
      return this;
    }

    Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder variables(Map<String, Object> variables) {
      this.variables = variables;
      return this;
    }

    public TestCase build() {
      return new TestCase(this.serviceProviderList, this.query, this.variables);
    }
  }

  private GraphQLOrchestrator createGraphQLOrchestrator(ExecutionStrategy queryExecutionStrategy,
      ExecutionStrategy mutationExecutionStrategy, ServiceProvider... services) {

    RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder()
        .services(Arrays.asList(services)).build().stitchGraph();

    GraphQLOrchestrator.Builder builder = GraphQLOrchestrator.newOrchestrator();
    builder.runtimeGraph(runtimeGraph);
    builder.instrumentations(Collections.emptyList());
    builder.executionIdProvider(ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER);
    if (Objects.nonNull(queryExecutionStrategy)) {
      builder.queryExecutionStrategy(queryExecutionStrategy);
    }
    if (Objects.nonNull(mutationExecutionStrategy)) {
      builder.mutationExecutionStrategy(mutationExecutionStrategy);
    }
    return builder.build();
  }
}
