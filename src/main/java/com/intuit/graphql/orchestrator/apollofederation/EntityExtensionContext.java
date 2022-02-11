package com.intuit.graphql.orchestrator.apollofederation;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.schema.FieldCoordinates;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Builder
@Getter
public class EntityExtensionContext  {

  public static final String DELIMITER = ":";

  private FieldDefinition fieldDefinition;
  private TypeDefinition parentTypeDefinition;
  private boolean requiresTypeNameInjection;
  private ServiceMetadata serviceMetadata;
  //private EntityDefinition baseType;
  private EntityExtensionDefinition thisEntityExtensionDefinition;

  public String createDataLoaderKey() {
    String serviceNamespace = serviceMetadata.getServiceProvider().getNameSpace();
    String parentTypename = getParentTypeDefinition().getName();

    return createDataLoaderKey("ENTITY_FETCH", serviceNamespace, parentTypename);
  }

  private static String createDataLoaderKey(String... tokens) {
    return StringUtils.join(tokens, DELIMITER);
  }

  public FieldCoordinates getFieldCoordinate() {
    return FieldCoordinates.coordinates(getParentTypeDefinition().getName(), fieldDefinition.getName());
  }
}
