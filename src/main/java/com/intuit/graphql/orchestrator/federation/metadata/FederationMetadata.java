package com.intuit.graphql.orchestrator.federation.metadata;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;

/**
 * This class holds the metadata for the usage of federation specs in a given {@link
 * com.intuit.graphql.orchestrator.schema.ServiceMetadata}
 */
@Getter
public class FederationMetadata {

  private final ServiceProvider serviceProvider;

  /**
   * entities owned by the service
   */
  private final Map<String, EntityMetadata> entitiesByTypename = new HashMap<>();

  /**
   * entities extended by the service
   */
  private final Map<String, EntityExtensionMetadata> extensionsByTypename = new HashMap<>();

  private final Map<FieldCoordinates, Set<Field>> requiresFieldSetByCoordinate = new HashMap<>();

  public FederationMetadata(ServiceProvider serviceProvider) {
    Objects.requireNonNull(serviceProvider);
    this.serviceProvider = serviceProvider;
  }

  public boolean isFieldExternal(FieldCoordinates fieldCoordinates) {
    EntityMetadata entityMetadata = entitiesByTypename.get(fieldCoordinates.getTypeName());
    return Objects.nonNull(entityMetadata) && !entityMetadata.getFields().contains(fieldCoordinates.getFieldName());
  }

  public void addEntity(EntityMetadata entityMetadata) {
    this.entitiesByTypename.put(entityMetadata.getTypeName(), entityMetadata);
  }

  public void addEntityExtension(EntityExtensionMetadata entityExtensionMetadata) {
    this.extensionsByTypename.put(entityExtensionMetadata.getTypeName(), entityExtensionMetadata);
  }

  public boolean isEntity(String typename) {
    return this.entitiesByTypename.containsKey(typename);
  }

  public EntityMetadata getEntityMetadataByName(String typename) {
    return this.entitiesByTypename.get(typename);
  }

  public boolean hasRequiresFieldSet(FieldCoordinates fieldCoordinates) {
    return this.requiresFieldSetByCoordinate.containsKey(fieldCoordinates);
  }

  public Set<Field> getRequireFields(FieldCoordinates fieldCoordinates) {
    return this.requiresFieldSetByCoordinate.get(fieldCoordinates);
  }

  @Builder
  @Getter
  public static class EntityMetadata {

    @NonNull
    private final String typeName;
    @NonNull
    private final List<KeyDirectiveMetadata> keyDirectives;
    @NonNull
    private final Set<String> fields;
    @NonNull
    private final FederationMetadata federationMetadata;

    public static Set<String> getFieldsFrom(TypeDefinition entityDefinition) {
      Set<String> output = new HashSet<>(); // make sure HashSet is used
      getFieldDefinitions(entityDefinition).stream()
          .map(FieldDefinition::getName)
          .forEach(output::add);
      return output;
    }
  }

  @Builder
  @Getter
  public static class EntityExtensionMetadata {

    @NonNull
    private final String typeName;
    @NonNull
    private final List<KeyDirectiveMetadata> keyDirectives;
    @NonNull
    private final Map<String, Set<Field>> requiredFieldsByFieldName;
    @NonNull
    private final FederationMetadata federationMetadata;
    // TODO @provides

    private EntityMetadata baseEntityMetadata;

    public void setBaseEntityMetadata(EntityMetadata baseEntityMetadata) {
      Objects.requireNonNull(baseEntityMetadata);
      FederationMetadata baseFederationMetadata = baseEntityMetadata.getFederationMetadata();
      Objects.requireNonNull(baseFederationMetadata);
      requiredFieldsByFieldName.forEach(
          (fieldName, requireFieldSet) -> {
            FieldCoordinates fieldCoordinates = FieldCoordinates.coordinates(typeName, fieldName);
            baseFederationMetadata.requiresFieldSetByCoordinate.put(
                fieldCoordinates, requireFieldSet);
          });

      this.baseEntityMetadata = baseEntityMetadata;
    }

    public ServiceProvider getServiceProvider() {
      return this.federationMetadata.getServiceProvider();
    }

    public ServiceProvider getBaseServiceProvider() {
      return this.baseEntityMetadata
          .getFederationMetadata()
          .getServiceProvider();
    }

    public Set<Field> getRequiredFields(String fieldName) {
      return this.requiredFieldsByFieldName.get(fieldName);
    }
  }
}
