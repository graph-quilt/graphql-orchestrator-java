package com.intuit.graphql.orchestrator;

import com.intuit.graphql.orchestrator.batch.QueryExecutor;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TestServiceProvider implements ServiceProvider {

  String namespace;
  ServiceType serviceType;
  Map<String, String> sdlFiles;
  Set<String> domainTypes;
  QueryExecutor queryExecutor;


  private TestServiceProvider(final Builder builder) {
    namespace = builder.namespace;
    sdlFiles = builder.sdlFiles;
    serviceType = builder.serviceType;
    this.domainTypes = builder.domainTypes;
    queryExecutor = builder.queryExecutor;
  }

  public TestServiceProvider(String url, String namespace, Map<String, String> sdlFiles) {
    this.namespace = namespace;
    this.sdlFiles = sdlFiles;
    this.serviceType = ServiceType.GRAPHQL;
  }

  public TestServiceProvider(String url, String namespace, String sdlFilename) {
    this(url, namespace, TestHelper.getFileMapFromList(sdlFilename));
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public String getNameSpace() {
    return namespace;
  }

  @Override
  public Map<String, String> sdlFiles() {
    return sdlFiles;
  }

  @Override
  public ServiceType getSeviceType() {
    return serviceType;
  }

  @Override
  public Set<String> domainTypes() {
    return domainTypes;
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(final ExecutionInput executionInput,
      final GraphQLContext context) {
    if (queryExecutor != null) {
      queryExecutor.query(executionInput, context);
    }
    return CompletableFuture.completedFuture(Collections.EMPTY_MAP);
  }

  public static final class Builder {

    private String namespace;
    private Map<String, String> sdlFiles;
    private ServiceType serviceType = ServiceType.GRAPHQL;
    private Set<String> domainTypes = new HashSet<>();
    private QueryExecutor queryExecutor;

    private Builder() {
    }

    public Builder namespace(final String val) {
      namespace = val;
      return this;
    }

    public Builder sdlFiles(final Map<String, String> val) {
      sdlFiles = val;
      return this;
    }

    public Builder serviceType(final ServiceType val) {
      serviceType = val;
      return this;
    }

    public Builder domainTypes(final Set val) {
      domainTypes.addAll(val);
      return this;
    }

    public Builder queryFunction(QueryExecutor queryExecutor) {
      queryExecutor = queryExecutor;
      return this;
    }

    public TestServiceProvider build() {
      return new TestServiceProvider(this);
    }
  }
}
