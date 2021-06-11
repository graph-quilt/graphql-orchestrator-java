package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.schema.transform.FieldWithResolverMetadata;
import org.apache.commons.lang3.StringUtils;

public class FieldResolverDataLoaderUtil {

  public static final String DELIMITER = ":";

  private FieldResolverDataLoaderUtil() {}

  public static String createDataLoaderKey(String... tokens) {
    return StringUtils.join(tokens, DELIMITER);
  }

  public static String createDataLoaderKeyFrom(FieldWithResolverMetadata fieldWithResolverMetadata) {
    String serviceNamespace = fieldWithResolverMetadata.getServiceNamespace();
    String parentTypename = fieldWithResolverMetadata.getParentTypeDefinition().getName();
    String fieldname = fieldWithResolverMetadata.getFieldDefinition().getName();
    return createDataLoaderKey(serviceNamespace, parentTypename, fieldname);
  }

}
