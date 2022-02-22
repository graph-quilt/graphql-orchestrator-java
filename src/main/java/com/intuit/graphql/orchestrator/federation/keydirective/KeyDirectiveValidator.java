package com.intuit.graphql.orchestrator.federation.keydirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.orchestrator.federation.keydirective.exceptions.MultipleArgumentsForKeyDirective;
import com.intuit.graphql.orchestrator.federation.keydirective.exceptions.NoFieldsArgumentForKeyDirective;
import com.intuit.graphql.orchestrator.federation.keydirective.exceptions.InvalidLocationForKeyDirective;
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

    validateKeyDirectiveLocation(typeDefinition, containerName);
    validateKeyArgumentSize(argumentList, containerName);

    Optional<Argument> argument = argumentList.stream().findFirst();
    if(argument.isPresent()) {
      validateKeyArgumentName(argument.get(), containerName);
      checkFieldSetValidity(sourceGraph, typeDefinition, argument.get().getValueWithVariable().getStringValue(), FEDERATION_KEY_DIRECTIVE);
    }
  }

  private void validateKeyArgumentSize(List<Argument> argumentList, String containerName) throws MultipleArgumentsForKeyDirective {
    if(argumentList.size() > 1) {
      throw new MultipleArgumentsForKeyDirective(containerName);
    }
  }

  private void validateKeyDirectiveLocation(TypeDefinition typeDefinition, String containerName) throws InvalidLocationForKeyDirective {
    if(!(typeDefinition.eContainer() instanceof TypeSystemDefinition || typeDefinition.eContainer() instanceof InterfaceTypeDefinition)) {
      throw new InvalidLocationForKeyDirective(containerName, typeDefinition.eContainer().eClass().getInstanceClassName());
    }
  }

  private void validateKeyArgumentName(Argument argument, String containerName) throws NoFieldsArgumentForKeyDirective {
    if(!StringUtils.equals("fields", argument.getName())) {
      throw new NoFieldsArgumentForKeyDirective(containerName);
    }
  }
}
