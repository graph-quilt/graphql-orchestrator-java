package com.intuit.graphql.orchestrator.federation.keydirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.exceptions.DirectiveMissingRequiredArgumentException;
import com.intuit.graphql.orchestrator.federation.exceptions.IncorrectDirectiveArgumentSizeException;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

import static com.intuit.graphql.orchestrator.utils.FederationUtils.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.checkFieldSetValidity;

/**
 * This class helps break up the {@link com.intuit.graphql.orchestrator.schema.transform.KeyTransformer} by
 * validating if the TypeDefinition key directive is valid.
 */
public class KeyDirectiveValidator {

  public void validate(XtextGraph sourceGraph, TypeDefinition typeDefinition, List<Argument> argumentList) {
    String containerName = typeDefinition.getName();

    validateKeyArgumentSize(argumentList, containerName);

    Optional<Argument> argument = argumentList.stream().findFirst();
    if(argument.isPresent()) {
      validateKeyArgumentName(argument.get(), containerName);
      checkFieldSetValidity(sourceGraph, typeDefinition, argument.get().getValueWithVariable().getStringValue(), FEDERATION_KEY_DIRECTIVE);
    }
  }

  private void validateKeyArgumentSize(List<Argument> argumentList, String containerName) throws IncorrectDirectiveArgumentSizeException {
    if(argumentList.size() > 1) {
      throw new IncorrectDirectiveArgumentSizeException(FEDERATION_KEY_DIRECTIVE, containerName, 1);
    }
  }

  private void validateKeyArgumentName(Argument argument, String containerName) throws DirectiveMissingRequiredArgumentException {
    if(!StringUtils.equals("fields", argument.getName())) {
      throw new DirectiveMissingRequiredArgumentException(FEDERATION_KEY_DIRECTIVE, containerName);
    }
  }
}
