package com.intuit.graphql.orchestrator.xtext;

import static java.util.Objects.requireNonNull;

import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.graphql.orchestrator.apollofederation.EntityExtensionContext;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import lombok.Getter;

@Getter
public class DataFetcherContext {

  public static final DataFetcherContext STATIC_DATAFETCHER_CONTEXT = DataFetcherContext
      .newBuilder().dataFetcherType(DataFetcherType.STATIC).build();

  private final String namespace;
  private final DataFetcherType dataFetcherType;
  private final FieldResolverContext fieldResolverContext;
  private final EntityExtensionContext entityExtensionContext;
  private final ServiceType serviceType;

  private DataFetcherContext(final Builder builder) {
    namespace = builder.namespace;
    dataFetcherType = builder.dataFetcherType;
    this.serviceType = builder.serviceType;
    this.fieldResolverContext = builder.fieldResolverContext;
    this.entityExtensionContext = builder.entityExtensionContext;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(DataFetcherContext copy) {
    Builder builder = new Builder();
    builder.namespace = copy.getNamespace();
    builder.dataFetcherType = copy.getDataFetcherType();
    return builder;
  }

  public enum DataFetcherType {
    STATIC, SERVICE, PROPERTY, RESOLVER_ARGUMENT, RESOLVER_ON_FIELD_DEFINITION, ENTITY_FETCHER
  }

  public static final class Builder {

    private String namespace;
    private DataFetcherType dataFetcherType = DataFetcherType.PROPERTY;
    private FieldResolverContext fieldResolverContext;
    private EntityExtensionContext entityExtensionContext;
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

    public Builder fieldResolverContext(FieldResolverContext fieldResolverContext) {
      this.fieldResolverContext = fieldResolverContext;
      return this;
    }

    public Builder serviceType(ServiceType serviceType){
      this.serviceType = serviceType;
      return this;
    }

    public Builder entityExtensionContext(EntityExtensionContext entityExtensionContext) {
      this.entityExtensionContext = entityExtensionContext;
      return this;
    }

    public DataFetcherContext build() {
      return new DataFetcherContext(this);
    }

  }
}
