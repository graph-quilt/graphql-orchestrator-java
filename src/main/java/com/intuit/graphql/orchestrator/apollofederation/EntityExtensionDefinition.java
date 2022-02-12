package com.intuit.graphql.orchestrator.apollofederation;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
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
  private List<FieldDefinition> additionFieldDefinitions;
  private List<KeyDirectiveDefinition> keyDirectiveDefinitions;
  private ServiceMetadata serviceMetadata;

  // this is the service that owns the typeName, i.e. base type
  private ServiceMetadata baseServiceMetadata;

  public String createDataLoaderKey() {
    String serviceNamespace = serviceMetadata.getServiceProvider().getNameSpace();
    // TODO batch by service not by type
    return createDataLoaderKey("ENTITY_FETCH", serviceNamespace, typeName);
  }

  private static String createDataLoaderKey(String... tokens) {
    return StringUtils.join(tokens, DELIMITER);
  }

  public ServiceProvider getServiceProvider() {
    return this.serviceMetadata.getServiceProvider();
  }

  public ServiceProvider getBaseServiceProvider() {
    return this.baseServiceMetadata.getServiceProvider();
  }
}
