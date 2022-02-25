package com.intuit.graphql.orchestrator.federation;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.schema.FieldCoordinates;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EntityExtensionContext  {

  public static final String DELIMITER = ":";

  private ServiceMetadata serviceMetadata;

  private FieldDefinition fieldDefinition;
  private TypeDefinition parentTypeDefinition;
  private boolean requiresTypeNameInjection;
  //private EntityDefinition baseType;
  private EntityExtensionMetadata entityExtensionMetadata;
  private String dataLoaderKey;

  public FieldCoordinates getFieldCoordinate() {
    return FieldCoordinates.coordinates(getParentTypename(), fieldDefinition.getName());
  }

  public String getParentTypename() {
    return parentTypeDefinition.getName();
  }
}
