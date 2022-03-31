package com.intuit.graphql.orchestrator.fieldresolver;

import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentNotAFieldOfParentException;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import java.util.Objects;

public class FieldResolverValidator {

  public static void validateRequiredFields(FieldResolverContext fieldResolverContext) {
    if (Objects.nonNull(fieldResolverContext.getRequiredFields())) {
      fieldResolverContext.getRequiredFields().forEach(reqdFieldName -> {
        if (!fieldResolverContext.getParentTypeFields().containsKey(reqdFieldName)) {
          String serviceName = fieldResolverContext.getServiceNamespace();
          String parentTypeName = fieldResolverContext.getParentTypename();
          String fieldResolverName = fieldResolverContext.getFieldName();
          throw new ResolverArgumentNotAFieldOfParentException(reqdFieldName, serviceName,
              parentTypeName, fieldResolverName);
        }
      });
    }
  }
}
