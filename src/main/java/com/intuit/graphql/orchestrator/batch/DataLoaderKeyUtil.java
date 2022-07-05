package com.intuit.graphql.orchestrator.batch;

import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import org.apache.commons.lang3.StringUtils;

public class DataLoaderKeyUtil {

  public static final String DELIMITER = ":";

  private DataLoaderKeyUtil() {}

  public static String createDataLoaderKey(String... tokens) {
    return StringUtils.join(tokens, DELIMITER);
  }

  public static String createDataLoaderKeyFrom(FieldResolverContext fieldResolverContext) {
    String serviceNamespace = fieldResolverContext.getServiceNamespace();
    String parentTypename = fieldResolverContext.getParentTypeDefinition().getName();
    String fieldName = fieldResolverContext.getFieldDefinition().getName();
    return createDataLoaderKey(serviceNamespace, parentTypename, fieldName);
  }

}
