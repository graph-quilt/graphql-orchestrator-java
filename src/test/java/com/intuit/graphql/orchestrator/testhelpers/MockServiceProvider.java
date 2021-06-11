package com.intuit.graphql.orchestrator.testhelpers;

import static com.intuit.graphql.orchestrator.testhelpers.TestFileLoader.loadJsonAsMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.mockito.Mockito;

public class MockServiceProvider {

  public static Builder builder() {
    return new MockServiceProvider.Builder();
  }

  public static final class Builder {

    private ServiceProvider mockServiceProvider = Mockito.mock(ServiceProvider.class);
    private List<ServiceProviderMockResponse> mockResponses = new ArrayList<>();

    private Builder() {
      when(mockServiceProvider.domainTypes()).thenReturn(Collections.emptySet());
      when(mockServiceProvider.getSeviceType()).thenReturn(ServiceType.GRAPHQL);
      doReturn(CompletableFuture.completedFuture(ImmutableMap.of("data", Collections.emptyMap())))
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

    public Builder mockResponse(ServiceProviderMockResponse serviceProviderMockResponse) {
      mockResponses.add(serviceProviderMockResponse);
      return this;
    }

    public ServiceProvider build() throws IOException {
      for (ServiceProviderMockResponse serviceProviderMockResponse : this.mockResponses) {

        Map<String, Object> data = loadJsonAsMap(serviceProviderMockResponse.getExpectResponse());

        ExecutionInput executionInput = serviceProviderMockResponse.getForExecutionInput();

        doReturn(CompletableFuture.completedFuture(data))
            .when(mockServiceProvider)
            .query(argThat(new ExecutionInputMatcher(executionInput)), any(GraphQLContext.class));
      }

      return this.mockServiceProvider;
    }
  }
}
