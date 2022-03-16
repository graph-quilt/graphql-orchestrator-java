package com.intuit.graphql.orchestrator.schema;

import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TypeMetadata {
  @NonNull private final TypeDefinition typeDefinition;
  private final Map<String, FieldResolverContext> fieldResolverContextsByFieldName = new HashMap<>();

  /**
   * checks if the given field has resolver directive
   * @param fieldName field name
   * @return true or false
   */
  public boolean hasResolverDirective(String fieldName) {
    return fieldResolverContextsByFieldName.containsKey(fieldName);
  }

  public void addFieldResolverContext(FieldResolverContext fieldResolverContext) {
    String fieldName = fieldResolverContext.getFieldName();
    fieldResolverContextsByFieldName.put(fieldName, fieldResolverContext);
  }

  /**
   * get the FieldResolverContext for the given field name
   * @param fieldName field name
   * @return null if not found.  Otherwise, the actual FieldResolverContext object
   */
  public FieldResolverContext getFieldResolverContext(String fieldName) {
    return fieldResolverContextsByFieldName.get(fieldName);
  }
}
