package com.intuit.graphql.orchestrator.testhelpers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.mockito.Mockito;

/** A mock service provider that can be used with Mockito verify */
public class SimpleMockServiceProvider {

  public static Builder builder() {
    return new SimpleMockServiceProvider.Builder();
  }

  public static final class Builder {

    private static final String DEFAULT_TEST_NAMESPACE = "testNamespace";

    private final ServiceProvider mockServiceProvider = Mockito.mock(ServiceProvider.class);
    private final Map<String, Object> mockResponse = new HashMap<>();

    private Builder() {
      when(mockServiceProvider.getNameSpace()).thenReturn(DEFAULT_TEST_NAMESPACE);
      when(mockServiceProvider.domainTypes()).thenReturn(Collections.emptySet());
      when(mockServiceProvider.getSeviceType()).thenReturn(ServiceType.GRAPHQL);
      doReturn(CompletableFuture.completedFuture(mockResponse))
          .when(mockServiceProvider)
          .query(any(ExecutionInput.class), any(GraphQLContext.class));
    }

    public Builder namespace(String namespace) {
      when(mockServiceProvider.getNameSpace()).thenReturn(namespace);
      return this;
    }

    public Builder sdlFiles(Map<String, String> sdlFiles) {
      when(mockServiceProvider.sdlFiles()).thenReturn(sdlFiles);
      return this;
    }

    public Builder domainTypes(Set<String> domainTypes) {
      when(mockServiceProvider.domainTypes()).thenReturn(domainTypes);
      return this;
    }

    public Builder serviceType(ServiceType serviceType) {
      when(mockServiceProvider.getSeviceType()).thenReturn(serviceType);
      return this;
    }

    public Builder mockResponse(Map<String, Object> mockResponse) {
      this.mockResponse.putAll(mockResponse);
      return this;
    }

    public ServiceProvider build() throws IOException {
      return this.mockServiceProvider;
    }
  }
}
