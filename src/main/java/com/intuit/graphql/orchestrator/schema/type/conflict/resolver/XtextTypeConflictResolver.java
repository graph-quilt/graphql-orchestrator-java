package com.intuit.graphql.orchestrator.schema.type.conflict.resolver;

import static com.intuit.graphql.orchestrator.utils.XtextUtils.toDescriptiveString;

import com.intuit.graphql.graphQL.TypeDefinition;
import org.apache.commons.lang3.StringUtils;

public class XtextTypeConflictResolver {

  public static final XtextTypeConflictResolver INSTANCE = new XtextTypeConflictResolver();

  private XtextTypeConflictResolver() {
  }

  public void resolve(final TypeDefinition conflictingType, final TypeDefinition existingType)
      throws TypeConflictException {
    /* Note: Do not change the order of checks */
    checkSameType(conflictingType, existingType);
  }

  private void checkSameType(final TypeDefinition conflictingType, final TypeDefinition existingType) {
    if (!isSameType(conflictingType, existingType)) {
      throw new TypeConflictException(
          String.format("Type %s is conflicting with existing type %s", toDescriptiveString(conflictingType),
              toDescriptiveString(existingType)));
    }
  }

  private boolean isSameType(final TypeDefinition t1, final TypeDefinition t2) {
    return StringUtils.equals(t1.getClass().getSimpleName(), t2.getClass().getSimpleName());
  }

}
