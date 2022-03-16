package com.intuit.graphql.orchestrator.federation.validators;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger;
import com.intuit.graphql.orchestrator.federation.exceptions.DirectiveMissingRequiredArgumentException;
import com.intuit.graphql.orchestrator.federation.exceptions.IncorrectDirectiveArgumentSizeException;
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException;
import com.intuit.graphql.orchestrator.utils.FederationUtils;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.ecore.EObject;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_FIELDS_ARGUMENT;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getTypeDefinitionName;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getDirectivesWithNameFromDefinition;
import static java.lang.String.format;

public class KeyDirectiveValidator {
  private final FieldSetValidator fieldSetValidator = new FieldSetValidator();

  public void validate(XtextGraph sourceGraph, EObject typeDefinition, List<Argument> argumentList) {
    String containerName = getTypeDefinitionName(typeDefinition);

    validateKeyArgumentSize(argumentList, containerName);

    Optional<Argument> argument = argumentList.stream().findFirst();
    if(argument.isPresent()) {
      validateKeyArgumentName(argument.get(), containerName);
      fieldSetValidator.validate(sourceGraph, typeDefinition, argument.get().getValueWithVariable().getStringValue(), FEDERATION_KEY_DIRECTIVE);
    }
  }

  public void validatePostMerge(EntityTypeMerger.EntityMergingContext entityMergingContext) {
    checkExtensionKeysAreSubset(entityMergingContext);
  }


  private void checkExtensionKeysAreSubset(EntityTypeMerger.EntityMergingContext entityMergingContext) {
    List<String> baseEntityKeys = getDirectivesWithNameFromDefinition(entityMergingContext.getBaseType(), FEDERATION_KEY_DIRECTIVE).stream()
            .map(this::getDirectiveFieldSet)
            .map(FederationUtils::getUniqueIdFromFieldSet)
            .collect(Collectors.toList());

    List<String> subsetKeys = getDirectivesWithNameFromDefinition(entityMergingContext.getTypeExtension(), FEDERATION_KEY_DIRECTIVE).stream()
            .map(this::getDirectiveFieldSet)
            .map(FederationUtils::getUniqueIdFromFieldSet)
            .collect(Collectors.toList());

    if(!baseEntityKeys.containsAll(subsetKeys)) {
      String incompatibleKeyMergeErrorMsg = "Failed to merge entity extension to base type. Defined keys do not exist in base entity. typename%s, serviceNamespace=%s";
      throw new TypeConflictException(format(incompatibleKeyMergeErrorMsg, entityMergingContext.getTypename(), entityMergingContext.getServiceNamespace()));
    }
  }

  private String getDirectiveFieldSet(Directive directive) {
    return directive.getArguments().get(0).getValueWithVariable().getStringValue();
  }


  private void validateKeyArgumentSize(List<Argument> argumentList, String containerName) throws IncorrectDirectiveArgumentSizeException {
    if(argumentList.size() > 1) {
      throw new IncorrectDirectiveArgumentSizeException(FEDERATION_KEY_DIRECTIVE, containerName, 1);
    }
  }

  private void validateKeyArgumentName(Argument argument, String containerName) throws DirectiveMissingRequiredArgumentException {
    if(!StringUtils.equals(FEDERATION_FIELDS_ARGUMENT, argument.getName())) {
      throw new DirectiveMissingRequiredArgumentException(FEDERATION_KEY_DIRECTIVE, containerName);
    }
  }
}
