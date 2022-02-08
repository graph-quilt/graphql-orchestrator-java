package com.intuit.graphql.orchestrator.apollofederation;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@Data
public class EntityExtensionContext  {

  public static final String DELIMITER = ":";

  private FieldDefinition fieldDefinition;
  private TypeDefinition parentTypeDefinition;
  private boolean requiresTypeNameInjection;
  private ServiceMetadata serviceMetadata;
  private EntityDefinition baseType;
  private EntityExtensionDefinition thisEntityExtensionDefinition;

  public String createDataLoaderKey() {
    String serviceNamespace = serviceMetadata.getServiceProvider().getNameSpace();
    String parentTypename = getParentTypeDefinition().getName();
    return createDataLoaderKey(serviceNamespace, "ENTITY", parentTypename);
  }

  private static String createDataLoaderKey(String... tokens) {
    return StringUtils.join(tokens, DELIMITER);
  }

}
