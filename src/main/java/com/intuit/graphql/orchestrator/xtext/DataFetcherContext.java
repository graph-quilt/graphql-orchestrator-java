package com.intuit.graphql.orchestrator.xtext;

import static java.util.Objects.requireNonNull;

import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.transform.FieldWithResolverMetadata;
import lombok.Getter;

@Getter
public class DataFetcherContext {

  public static final DataFetcherContext STATIC_DATAFETCHER_CONTEXT = DataFetcherContext
      .newBuilder().dataFetcherType(DataFetcherType.STATIC).build();

  private final String namespace;
  private final DataFetcherType dataFetcherType;
  private final ResolverDirectiveDefinition fieldResolverDirectiveDefinition;
  private final FieldWithResolverMetadata fieldWithResolverMetadata;
  private final ServiceType serviceType;

  private DataFetcherContext(final Builder builder) {
    namespace = builder.namespace;
    dataFetcherType = builder.dataFetcherType;
    this.serviceType = builder.serviceType;
    this.fieldResolverDirectiveDefinition = builder.fieldResolverDirectiveDefinition;
    this.fieldWithResolverMetadata = builder.fieldWithResolverMetadata;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(DataFetcherContext copy) {
    Builder builder = new Builder();
    builder.namespace = copy.getNamespace();
    builder.dataFetcherType = copy.getDataFetcherType();
    builder.fieldResolverDirectiveDefinition = copy.getFieldResolverDirectiveDefinition();
    return builder;
  }

  public ResolverDirectiveDefinition getFieldResolverDirectiveDefinition() {
    return this.fieldResolverDirectiveDefinition;
  }


  public enum DataFetcherType {
    STATIC, SERVICE, PROPERTY, RESOLVER_ARGUMENT, RESOLVER_ON_FIELD_DEFINITION
  }

  public static final class Builder {

    private String namespace;
    private DataFetcherType dataFetcherType = DataFetcherType.PROPERTY;
    private ResolverDirectiveDefinition fieldResolverDirectiveDefinition;
    private FieldWithResolverMetadata fieldWithResolverMetadata;
    private ServiceType serviceType;

    private Builder() {
    }

    public Builder namespace(String val) {
      this.namespace = requireNonNull(val);
      this.dataFetcherType = DataFetcherType.SERVICE;
      return this;
    }

    public Builder dataFetcherType(DataFetcherType val) {
      this.dataFetcherType = val;
      return this;
    }

    public Builder fieldResolverDirectiveDefinition(ResolverDirectiveDefinition resolverDirectiveDefinition) {
      this.fieldResolverDirectiveDefinition = resolverDirectiveDefinition;
      return this;
    }

    public Builder fieldWithResolverMetadata(FieldWithResolverMetadata fieldWithResolverMetadata) {
      this.fieldWithResolverMetadata = fieldWithResolverMetadata;
      return this;
    }

    public Builder serviceType(ServiceType serviceType){
      this.serviceType = serviceType;
      return this;
    }

    public DataFetcherContext build() {
      return new DataFetcherContext(this);
    }

  }
}
