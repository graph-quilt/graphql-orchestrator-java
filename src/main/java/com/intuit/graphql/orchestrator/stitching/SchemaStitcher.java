package com.intuit.graphql.orchestrator.stitching;

import static java.util.Objects.requireNonNull;

import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.batch.BatchLoaderExecutionHooks;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import graphql.VisibleForTesting;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class SchemaStitcher {

  private final List<ServiceProvider> serviceProviders;
  private final BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> batchLoaderHooks;

  @VisibleForTesting
  Stitcher stitcher;

  private RuntimeGraph runtimeGraph;

  /**
   * Stitch the xtext using the provided {@code serviceProviders} and return the stitched graph. This action is cached;
   * subsequent calls will return the same {@link RuntimeGraph} instance.
   *
   * @return The stitched graph which is an instance of {@link RuntimeGraph}
   */
  public RuntimeGraph stitchGraph() {
    if (runtimeGraph == null) {
      runtimeGraph = stitcher.stitch(this.serviceProviders);
    }

    return runtimeGraph;
  }

  private SchemaStitcher(Builder builder) {
    this.serviceProviders = requireNonNull(builder.serviceProviders);
    this.batchLoaderHooks = builder.batchLoaderHooks;
    this.stitcher = XtextStitcher.newBuilder()
        .batchLoaderHooks(batchLoaderHooks)
        .build();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {

    private BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> batchLoaderHooks = BatchLoaderExecutionHooks.DEFAULT_HOOKS;
    private List<ServiceProvider> serviceProviders = new ArrayList<>();

    private Builder() {
    }

    public Builder service(final ServiceProvider service) {
      this.serviceProviders.add(requireNonNull(service));
      return this;
    }

    public Builder services(final List<ServiceProvider> services) {
      this.serviceProviders.addAll(requireNonNull(services));
      return this;
    }

    public Builder batchLoaderHooks(
        final BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> batchLoaderHooks) {
      this.batchLoaderHooks = requireNonNull(batchLoaderHooks);
      return this;
    }

    public SchemaStitcher build() {
      return new SchemaStitcher(this);
    }
  }
}
