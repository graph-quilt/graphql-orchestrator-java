package com.intuit.graphql.orchestrator.federation.metadata;

import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.schema.FieldCoordinates;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/**
 * This class holds the metadata for the usage of federation specs in a given {@link
 * com.intuit.graphql.orchestrator.schema.ServiceMetadata}
 */
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

  @Builder
  @Getter
  public static class EntityMetadata {
    private String typeName;
    private Map<String, DirectiveMetadata> typeDirectives = new HashMap<>();
    private Set<String> fields = new HashSet<>();
    private ServiceMetadata serviceMetadata;
  }

  @Builder
  public static class EntityExtensionMetadata {
    private String typeName;
    private Map<String, DirectiveMetadata> typeDirectives = new HashMap<>();
    private Map<String, DirectiveMetadata> externalDirectives = new HashMap<>();
    private Map<String, DirectiveMetadata> requiresDirectives = new HashMap<>();
    private Set<String> fields = new HashSet<>();
    private ServiceMetadata serviceMetadata;
    // TODO @provides

  }
}
