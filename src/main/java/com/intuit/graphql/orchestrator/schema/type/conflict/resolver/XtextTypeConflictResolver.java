package com.intuit.graphql.orchestrator.schema.type.conflict.resolver;

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_EXTENDS_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.checkFieldsCompatibility;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isEntity;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isScalarType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.toDescriptiveString;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.definitionContainsDirective;

import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.UnionTypeDefinition;
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

  public void resolve(final TypeDefinition conflictingType, final TypeDefinition existingType, boolean isFederated)
          throws TypeConflictException {
    if(isFederated || isGoldenType(conflictingType)) {
      checkSharedType(conflictingType, existingType, isFederated);
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

  private void checkSharedType(final TypeDefinition conflictingType, final TypeDefinition existingType, boolean federatedComparison) {
    if(isSameType(conflictingType, existingType)) {
      boolean conflictingTypeisEntity = isEntity(conflictingType);
      boolean existingTypeIsEntity = isEntity(existingType);
      boolean entityComparison =  conflictingTypeisEntity && existingTypeIsEntity;
      boolean baseExtensionComparison = definitionContainsDirective(existingType, FEDERATION_EXTENDS_DIRECTIVE) || definitionContainsDirective(conflictingType, FEDERATION_EXTENDS_DIRECTIVE);

      if(isEntity(conflictingType) != isEntity(existingType)) {
        throw new TypeConflictException("Type %s is conflicting with existing type %s. Only one of the types are an entity.");
      }

      //In this specific case one is an entity and another is not an entity
      if(entityComparison && !baseExtensionComparison) {
        throw new TypeConflictException(
          String.format("Type %s is conflicting with existing type %s. Two schemas cannot own an entity.", toDescriptiveString(conflictingType),
                  toDescriptiveString(existingType)));
      }

      if(!(conflictingType instanceof UnionTypeDefinition || isScalarType(conflictingType) || conflictingType instanceof EnumTypeDefinition)) {
        checkFieldsCompatibility(existingType, conflictingType, existingTypeIsEntity, conflictingTypeisEntity,federatedComparison);
      }
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