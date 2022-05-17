package com.intuit.graphql.orchestrator.testhelpers;

import static com.intuit.graphql.orchestrator.testhelpers.JsonTestUtils.jsonToMap;

import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.TestHelper;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class MockServiceProvider implements ServiceProvider {

  private final String namespace;
  private final Map<String, String> sdlFiles;
  private final Set<String> domainTypes;
  private final ServiceType serviceType;
  private final Map<String, Map<String, Object>> responseMap;

  public MockServiceProvider(Builder builder) {
    this.namespace = builder.namespace;
    this.sdlFiles = builder.sdlFiles;
    this.domainTypes = builder.domainTypes;
    this.serviceType = builder.serviceType;
    this.responseMap = builder.responseMap;
  }

  @Override
  public String getNameSpace() {
    return this.namespace;
  }

  @Override
  public Set<String> domainTypes() {
    return this.domainTypes;
  }

  @Override
  public Map<String, String> sdlFiles() {
    return this.sdlFiles;
  }

  @Override
  public ServiceType getSeviceType() {
    return this.serviceType;
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {
    return CompletableFuture.completedFuture(this.responseMap.get(executionInput.getQuery()));
  }

  public static Builder builder() {
    return new MockServiceProvider.Builder();
  }

  public static final class Builder {

    private String namespace;
    private Map<String, String> sdlFiles = new HashMap<>();
    private Set<String> domainTypes = Collections.emptySet();
    private ServiceType serviceType = ServiceType.GRAPHQL;
    private Map<String, Map<String, Object>> responseMap = new HashMap<>();

    private Builder() {
    }

    public Builder namespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    public Builder sdlFiles(Map<String, String> sdlFiles) {
      this.sdlFiles = sdlFiles;
      return this;
    }

    public Builder domainTypes(Set<String> domainTypes) {
      this.domainTypes = domainTypes;
      return this;
    }

    public Builder serviceType(ServiceType serviceType) {
      this.serviceType = serviceType;
      return this;
    }

    public Builder responseMap(Map<String, Map<String, Object>> responseMap) {
      this.responseMap = responseMap;
      return this;
    }

    public Builder mockResponse(ServiceProviderMockResponse serviceProviderMockResponse) {
      String jsonString = TestHelper.getResourceAsString(serviceProviderMockResponse.getExpectResponse());
      this.responseMap.put(serviceProviderMockResponse.getForExecutionInput().getQuery(),
          jsonToMap(jsonString));
      return this;
    }

    public MockServiceProvider build() throws IOException {
      return new MockServiceProvider(this);
    }

  }
}
