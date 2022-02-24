package com.intuit.graphql.orchestrator.schema.type.conflict.resolver;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.checkFieldsCompatibility;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isScalarType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.toDescriptiveString;

import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class XtextTypeConflictResolver {

  public static final XtextTypeConflictResolver INSTANCE = new XtextTypeConflictResolver();
  public static final Set<String> goldenTypes = new HashSet<>(Arrays.asList("PageInfo", "ResolverArgument"));
  public static final Set<String> interfaceGoldenTypes = new HashSet<>(Arrays.asList("Node", "Entity"));

  private XtextTypeConflictResolver() {
  }

  public void resolve(final TypeDefinition conflictingType, final TypeDefinition existingType)
          throws TypeConflictException {
    if(isGoldenType(conflictingType)) {
      checkSharedType(conflictingType, existingType);
    } else {
      checkSameType(conflictingType, existingType);
    }
  }

  private void checkSameType(final TypeDefinition conflictingType, final TypeDefinition existingType) {
    if (!(isSameType(conflictingType, existingType) && isScalarType(conflictingType))) {
      throw new TypeConflictException(
          String.format("Type %s is conflicting with existing type %s", toDescriptiveString(conflictingType),
              toDescriptiveString(existingType)));
    }
  }

  private void checkSharedType(final TypeDefinition conflictingType, final TypeDefinition existingType) {
    if(isSameType(conflictingType, existingType)) {
      checkFieldsCompatibility(existingType, conflictingType);
    }
  }

  private boolean isSameType(final TypeDefinition t1, final TypeDefinition t2) {
    return StringUtils.equals(t1.getClass().getSimpleName(), t2.getClass().getSimpleName());
  }

  private boolean isGoldenType(TypeDefinition conflictingTypeDefinition) {
    return (conflictingTypeDefinition instanceof ObjectTypeDefinition && goldenTypes.contains(conflictingTypeDefinition.getName()))
            || (conflictingTypeDefinition instanceof InterfaceTypeDefinition && interfaceGoldenTypes
            .contains(conflictingTypeDefinition.getName()));
  }
}