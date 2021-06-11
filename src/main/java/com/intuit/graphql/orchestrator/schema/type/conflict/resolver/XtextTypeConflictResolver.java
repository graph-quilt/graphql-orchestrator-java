package com.intuit.graphql.orchestrator.schema.type.conflict.resolver;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isScalarType;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.toDescriptiveString;

import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import java.util.Arrays;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;

public class XtextTypeConflictResolver {

  public static final XtextTypeConflictResolver INSTANCE = new XtextTypeConflictResolver();
  public static HashSet<String> goldenTypes = new HashSet<>(Arrays.asList("PageInfo", "ResolverArgument"));
  public static HashSet<String> interfaceGoldenTypes = new HashSet<>(Arrays.asList("Node", "Entity"));

  private XtextTypeConflictResolver() {
  }

  public void resolve(final TypeDefinition conflictingType, final TypeDefinition existingType)
      throws TypeConflictException {
    /* Note: Do not change the order of checks */
    checkSameType(conflictingType, existingType);
    checkIfGoldenType(conflictingType, existingType);
  }

  private void checkSameType(final TypeDefinition conflictingType, final TypeDefinition existingType) {
    if (!isSameType(conflictingType, existingType)) {
      throw new TypeConflictException(
          String.format("Type %s is conflicting with existing type %s", toDescriptiveString(conflictingType),
              toDescriptiveString(existingType)));
    }
  }

  private void checkIfGoldenType(final TypeDefinition conflictingType, final TypeDefinition existingType) {
    if (!(isScalarType(conflictingType) || isGoldenType(conflictingType))) {
      throw new TypeConflictException(
          String.format("Type %s is conflicting with existing type %s and is not a golden type",
              toDescriptiveString(conflictingType), toDescriptiveString(existingType)));
    }
  }

  private boolean isSameType(final TypeDefinition t1, final TypeDefinition t2) {
    return StringUtils.equals(t1.getClass().getSimpleName(), t2.getClass().getSimpleName());
  }

  private boolean isGoldenType(final TypeDefinition conflictingType) {
    return (conflictingType instanceof ObjectTypeDefinition && goldenTypes.contains(conflictingType.getName()))
        || (conflictingType instanceof InterfaceTypeDefinition && interfaceGoldenTypes
        .contains(conflictingType.getName()));
  }
}
