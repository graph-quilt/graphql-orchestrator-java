package com.intuit.graphql.orchestrator.schema;

import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import graphql.schema.FieldCoordinates;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Getter
public class ServiceMetadataImpl implements ServiceMetadata {

  private final Map<String, TypeMetadata> typeMetadataMap;
  private final ServiceProvider serviceProvider;
  private final FederationMetadata federationMetadata;
  private final boolean hasInterfaceOrUnion;
  private final boolean hasFieldResolverDefinition;
  private final boolean containsRenamedFields;
  private final Map<String, String> originalTypeNamesByRenamedName;
  private final Map<String, String> originalFieldNamesByRenamedName;

  private ServiceMetadataImpl(Builder builder) {
    typeMetadataMap = builder.typeMetadataMap;
    serviceProvider = requireNonNull(builder.serviceProvider);
    federationMetadata = builder.federationMetadata;
    hasInterfaceOrUnion = builder.hasInterfaceOrUnion;
    hasFieldResolverDefinition = builder.hasFieldResolverDefinition;
    containsRenamedFields = builder.containsRenamedFields;
    originalTypeNamesByRenamedName = builder.originalTypeNamesByRenamedName;
    originalFieldNamesByRenamedName = builder.originalFieldNamesByRenamedName;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(ServiceMetadataImpl copy) {
    Builder builder = new Builder();
    builder.typeMetadataMap = copy.getTypeMetadataMap();
    builder.serviceProvider = copy.getServiceProvider();
    builder.federationMetadata = copy.getFederationMetadata();
    builder.hasInterfaceOrUnion = copy.isHasInterfaceOrUnion();
    builder.hasFieldResolverDefinition = copy.isHasFieldResolverDefinition();
    builder.containsRenamedFields = copy.containsRenamedFields();
    builder.originalTypeNamesByRenamedName = copy.getOriginalTypeNamesByRenamedName();
    builder.originalFieldNamesByRenamedName = copy.getOriginalFieldNamesByRenamedName();
    return builder;
  }

  @Override
  public boolean hasType(String typeName) {
    return this.typeMetadataMap.containsKey(typeName);
  }

  @Override
  public boolean requiresTypenameInjection() {
    return this.hasInterfaceOrUnion;
  }

  @Override
  public boolean hasFieldResolverDirective() {
    return this.hasFieldResolverDefinition;
  }

  @Override
  public boolean isFederationService() {
    return this.serviceProvider.isFederationProvider();
  }

  @Override
  public boolean shouldRemoveExternalFields() {
    return this.hasFieldResolverDirective() || this.isFederationService();
  }


  @Override
  public ServiceProvider getServiceProvider() {
    return this.serviceProvider;
  }

  @Override
  public FieldResolverContext getFieldResolverContext(FieldCoordinates fieldCoordinates) {
    return Optional.ofNullable(this.typeMetadataMap.get(fieldCoordinates.getTypeName()))
        .map(typeMetadata -> typeMetadata.getFieldResolverContext(fieldCoordinates.getFieldName()))
        .orElse(null);
  }

  @Override
  public boolean isOwnedByEntityExtension(FieldCoordinates fieldCoordinates) {
    return this.serviceProvider.isFederationProvider() && federationMetadata.isFieldExternal(fieldCoordinates);
  }


  @Override
  public boolean isEntity(String typename) {
    return this.isFederationService() && this.federationMetadata.isEntity(typename);
  }

  @Override
  public FederationMetadata getFederationServiceMetadata() {
    return federationMetadata;
  }

  @Override
  public boolean shouldUpdateOperationsOrFields() {
    return shouldRemoveExternalFields() || containsRenamedFields();
  }

  @Override
  public boolean containsRenamedFields(){
    return this.containsRenamedFields;
  }

  public Map<String, String> getOriginalTypeNamesByRenamedName() {
    return this.originalTypeNamesByRenamedName;
  }
  public Map<String, String> getOriginalFieldNamesByRenamedName() {
    return this.originalFieldNamesByRenamedName;
  }


  public static final class Builder {

    private Map<String, TypeMetadata> typeMetadataMap = new HashMap<>();
    private ServiceProvider serviceProvider;
    private FederationMetadata federationMetadata;
    private boolean hasInterfaceOrUnion;
    private boolean hasFieldResolverDefinition;
    private boolean containsRenamedFields;
    private Map<String, String> originalTypeNamesByRenamedName = new HashMap<>();
    private Map<String, String> originalFieldNamesByRenamedName = new HashMap<>();


    private Builder() {
    }

    public Builder typeMetadataMap(Map<String, TypeMetadata> val) {
      typeMetadataMap = val;
      return this;
    }

    public Builder serviceProvider(ServiceProvider val) {
      serviceProvider = val;
      return this;
    }

    public Builder federationMetadata(FederationMetadata val) {
      federationMetadata = val;
      return this;
    }

    public Builder hasInterfaceOrUnion(boolean val) {
      hasInterfaceOrUnion = val;
      return this;
    }

    public Builder hasFieldResolverDefinition(boolean val) {
      hasFieldResolverDefinition = val;
      return this;
    }

    public Builder containsRenamedFields(boolean val){
      this.containsRenamedFields = val;
      return this;
    }
    public Builder originalTypeNamesByRenamedName(Map<String, String> val){
      this.originalTypeNamesByRenamedName = val;
      return this;
    }
    public Builder originalFieldNamesByRenamedName(Map<String, String> val){
      this.originalFieldNamesByRenamedName = val;
      return this;
    }

    public ServiceMetadataImpl build() {
      return new ServiceMetadataImpl(this);
    }
  }
}
