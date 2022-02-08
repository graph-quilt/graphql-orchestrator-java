package com.intuit.graphql.orchestrator.keydirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.orchestrator.keydirective.exceptions.EmptyFieldsArgumentKeyDirective;
import com.intuit.graphql.orchestrator.keydirective.exceptions.MultipleArgumentsForKeyDirective;
import com.intuit.graphql.orchestrator.keydirective.exceptions.NoFieldsArgumentForKeyDirective;
import com.intuit.graphql.orchestrator.keydirective.exceptions.InvalidKeyDirectiveFieldReference;
import com.intuit.graphql.orchestrator.keydirective.exceptions.InvalidLocationForKeyDirective;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class helps break up the {@link com.intuit.graphql.orchestrator.schema.transform.ResolverArgumentTransformer} by
 * validating if the ObjectTypeDefinition key directive is valid.
 */
public class KeyDirectiveValidator {

  public void validateKeyArguments(ObjectTypeDefinition objectTypeDefinition,
      List<Argument> argumentList) throws InvalidLocationForKeyDirective, EmptyFieldsArgumentKeyDirective, InvalidKeyDirectiveFieldReference, NoFieldsArgumentForKeyDirective, MultipleArgumentsForKeyDirective {

    String containerName = objectTypeDefinition.getName();

    validateKeyDirectiveLocation(objectTypeDefinition, containerName);
    validateKeyArgumentSize(argumentList, containerName);

    Optional<Argument> argument = argumentList.stream().findFirst();
    if(argument.isPresent()) {
      validateKeyArgumentName(argument.get(), containerName);

      final List<String> keyFieldList = Arrays.asList(argument.get().getValueWithVariable().getStringValue().trim().split(" "));
      validateKeyFieldsArgumentSize(keyFieldList, containerName);

      final List<String> existingFieldDefinitions = ((List<FieldDefinition>) objectTypeDefinition.getFieldDefinition()).stream().map(FieldDefinition::getName).collect(Collectors.toList());
      validateKeyFieldReferences(existingFieldDefinitions, keyFieldList, containerName);
    }
  }

  private void validateKeyArgumentSize(List<Argument> argumentList, String containerName) throws MultipleArgumentsForKeyDirective {
    if(argumentList.size() > 1) {
      throw new MultipleArgumentsForKeyDirective(containerName);
    }
  }

  private void validateKeyDirectiveLocation(ObjectTypeDefinition objectTypeDefinition, String containerName) throws InvalidLocationForKeyDirective {
    if(!(objectTypeDefinition.eContainer() instanceof TypeSystemDefinition || objectTypeDefinition.eContainer() instanceof InterfaceTypeDefinition)) {
      throw new InvalidLocationForKeyDirective(containerName, objectTypeDefinition.eContainer().eClass().getInstanceClassName());
    }
  }

  private void validateKeyArgumentName(Argument argument, String containerName) throws NoFieldsArgumentForKeyDirective {
    if(!argument.getName().equals("fields")) {
      throw new NoFieldsArgumentForKeyDirective(containerName);
    }
  }

  private void validateKeyFieldsArgumentSize(List<String> keyFields, String containerName) throws EmptyFieldsArgumentKeyDirective {
    if(keyFields.isEmpty() || keyFields.stream().anyMatch(field -> field.equals(""))) {
      throw new EmptyFieldsArgumentKeyDirective(containerName);
    }
  }

  private  void validateKeyFieldReferences(List<String> existingFieldDefinitions, List<String> keyFieldRefereces, String containerName) throws InvalidKeyDirectiveFieldReference {
    for(String keyField : keyFieldRefereces) {
      if(existingFieldDefinitions.stream().noneMatch(definedField -> definedField.equals(keyField))) {
        throw new InvalidKeyDirectiveFieldReference(keyField, containerName);
      }
    }
  }
}
