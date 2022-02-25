package com.intuit.graphql.orchestrator.federation.metadata;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.schema.FieldCoordinates;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/**
 * This class holds the metadata for the usage of federation specs in a given {@link
 * com.intuit.graphql.orchestrator.schema.ServiceMetadata}
 */
@Getter
public class FederationMetadata {

  /**
   * entities owned by the service
   */
  private final Map<String, EntityMetadata> entitiesByTypename = new HashMap<>();

  /**
   * entities extended by the service
   */
  private final Map<String, EntityExtensionMetadata> extensionsByTypename = new HashMap<>();

  public boolean isFieldExternal(FieldCoordinates fieldCoordinates) {
    EntityMetadata entityMetadata = entitiesByTypename.get(fieldCoordinates.getTypeName());
    return !entityMetadata.getFields().contains(fieldCoordinates.getFieldName());
  }

  public void addEntity(EntityMetadata entityMetadata) {
    this.entitiesByTypename.put(entityMetadata.getTypeName(), entityMetadata);
  }

  public void addEntityExtension(EntityExtensionMetadata entityExtensionMetadata) {
    this.extensionsByTypename.put(entityExtensionMetadata.getTypeName(), entityExtensionMetadata);
  }

  @Builder
  @Getter
  public static class EntityMetadata {
    private String typeName;
    private List<KeyDirectiveMetadata> keyDirectives;
    private Set<String> fields;
    private ServiceMetadata serviceMetadata;

    public static Set<String> getFieldsFrom(TypeDefinition entityDefinition) {
      Set<String> output = new HashSet<>(); //make sure HashSet is used
      getFieldDefinitions(entityDefinition).stream()
          .map(FieldDefinition::getName)
          .forEach(output::add);
      return output;
    }
  }

  @Builder
  @Getter
  public static class EntityExtensionMetadata {
    private String typeName;
    private List<KeyDirectiveMetadata> keyDirectives;
    private Set<String> externalFields;
    private Set<String> requiredFields;
    private ServiceMetadata serviceMetadata;
    private ServiceMetadata baseServiceMetadata;
    private String dataLoaderKey;
    // TODO @provides

    public ServiceProvider getServiceProvider() {
      return this.serviceMetadata.getServiceProvider();
    }

    public ServiceProvider getBaseServiceProvider() {
      return this.baseServiceMetadata.getServiceProvider();
    }
  }
}
