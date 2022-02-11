package com.intuit.graphql.orchestrator.apollofederation;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.orchestrator.keydirective.KeyDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Builder
@Getter
public class EntityExtensionDefinition {

  public static final String DELIMITER = ":";

  private String typeName;
  private List<FieldDefinition> additionFieldDefinitions; // TODO rename to fieldDefinitions
  private List<KeyDirectiveDefinition> keyDirectiveDefinitions;
  private ServiceMetadata serviceMetadata;

  public String createDataLoaderKey() {
    String serviceNamespace = serviceMetadata.getServiceProvider().getNameSpace();
    return createDataLoaderKey("ENTITY_FETCH", serviceNamespace, typeName);
  }

  private static String createDataLoaderKey(String... tokens) {
    return StringUtils.join(tokens, DELIMITER);
  }
}
