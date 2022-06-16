package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeExtensionDefinition;
import com.intuit.graphql.graphQL.ObjectTypeExtensionDefinition;
import com.intuit.graphql.graphQL.OperationType;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeExtensionDefinition;
import com.intuit.graphql.orchestrator.exceptions.InvalidRenameException;
import com.intuit.graphql.orchestrator.metadata.RenamedMetadata;
import com.intuit.graphql.orchestrator.utils.FederationUtils;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_EXTENDS_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.RenameDirectiveUtil.getRenameKey;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.RENAME_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getDirectiveWithNameFromDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getTypeSystemDefinition;

public class RenameTransformer implements Transformer<XtextGraph, XtextGraph> {

  private final String RENAME_ARGUMENT_NAME = "to";

  @Override
  public XtextGraph transform(XtextGraph xtextGraph) {
    Map<String, String> renamedTypeMap = new HashMap<>();
    Map<String, String> renamedFieldMap = new HashMap<>();
    RenamedMetadata metadata = new RenamedMetadata(xtextGraph.getServiceProvider());

    //Processing base types
    XtextUtils.getAllTypes(xtextGraph.getXtextResourceSet())
    .forEach(typeDefinition -> processTypeDefinition(typeDefinition, renamedTypeMap, renamedFieldMap));

    //Process extensions
    getTypeSystemDefinition(xtextGraph.getXtextResourceSet())
    .filter(FederationUtils::isTypeSystemForExtensionType)
    .filter(typeSystemDefinition ->
            typeSystemDefinition.getTypeExtension() instanceof ObjectTypeExtensionDefinition
              || typeSystemDefinition.getTypeExtension() instanceof InterfaceTypeExtensionDefinition
    )
    .forEach(typeSystemDefinition ->
        processTypeDefinition(typeSystemDefinition.getTypeExtension(), renamedFieldMap)
    );

    if(!renamedTypeMap.isEmpty()) {
      metadata.getOriginalTypeNamesByRenamedName().putAll(renamedTypeMap);
    }

    if(!renamedFieldMap.isEmpty()) {
      metadata.getOriginalFieldNamesByRenamedName().putAll(renamedFieldMap);
    }

    xtextGraph.addRenamedMetadata(metadata);
    return xtextGraph;
  }

  private void processTypeDefinition(TypeDefinition typeDefinition, Map<String, String> renamedTypeMap, Map<String, String> renamedFieldMap) {
    getDirectiveWithNameFromDefinition(typeDefinition, RENAME_DIRECTIVE)
    .ifPresent(typeRenameDirective -> {
      if(getDirectiveWithNameFromDefinition(typeDefinition, FEDERATION_EXTENDS_DIRECTIVE).isPresent()) {
        throw new InvalidRenameException(
                String.format("Invalid rename directive for %s: Type Extensions cannot be renamed %s", typeDefinition.getName(), typeDefinition.getName())
        );
      }

      addTypeToMap(typeDefinition, typeRenameDirective, renamedTypeMap);
    });

    getFieldDefinitions(typeDefinition, true).forEach(fieldDefinition ->
            getDirectiveWithNameFromDefinition(fieldDefinition, RENAME_DIRECTIVE)
                    .ifPresent(directive -> addFieldToMap(typeDefinition.getName(), fieldDefinition, directive, renamedFieldMap))
    );
  }

  private void processTypeDefinition(TypeExtensionDefinition typeDefinition, Map<String, String> renamedFieldMap) {
    if(getDirectiveWithNameFromDefinition(typeDefinition, RENAME_DIRECTIVE).isPresent()) {
      throw new InvalidRenameException(
        String.format("Invalid rename directive for %s: Type Extensions cannot be renamed %s", typeDefinition.getName(), typeDefinition.getName())
      );
    }

    getFieldDefinitions(typeDefinition, true).forEach(fieldDefinition ->
            getDirectiveWithNameFromDefinition(fieldDefinition, RENAME_DIRECTIVE)
                    .ifPresent(directive -> addFieldToMap(typeDefinition.getName(), fieldDefinition, directive, renamedFieldMap))
    );
  }

  private void addTypeToMap(TypeDefinition typeDefinition, Directive renameDirective, Map<String, String> renamedTypeMap){
    Pair<String, String> renamedPair = getRenamedPair(typeDefinition, renameDirective);
    addRenamedPairToMap(typeDefinition.getName(), renamedPair, renamedTypeMap);
    typeDefinition.setName(renamedPair.getKey());
  }

  private void addFieldToMap(String parentName, FieldDefinition fieldDefinition,
                             Directive renameDirective, Map<String, String> renamedFieldMap) {
    Pair<String, String> originalRenamedPair = getRenamedPair(fieldDefinition, renameDirective);
    String originalAlias = originalRenamedPair.getKey();

    String metadataKey = getRenameKey(parentName, originalRenamedPair.getKey(), (OperationType.get(parentName.toLowerCase()) != null));
    Pair<String, String> renamedPair = Pair.of(metadataKey, originalRenamedPair.getValue());

    addRenamedPairToMap(fieldDefinition.getName(), renamedPair, renamedFieldMap);
    fieldDefinition.setName(originalAlias);
  }

  private Pair<String, String> getRenamedPair(FieldDefinition fieldDefinition, Directive renameDirective){
    String originalName = fieldDefinition.getName();
    String alias = renameDirective.getArguments().stream()
            .filter(argument -> argument.getName().equals(RENAME_ARGUMENT_NAME))
            .findFirst()
            .map(toArgument -> toArgument.getValueWithVariable().getStringValue())
            .orElse(null);

    validateToValue(fieldDefinition.getName(), alias);

    return Pair.of(alias, originalName);
  }

  private Pair<String, String> getRenamedPair(TypeDefinition typeDefinition, Directive renameDirective){
    String originalName = typeDefinition.getName();
    String alias = renameDirective.getArguments().stream()
            .filter(argument -> argument.getName().equals(RENAME_ARGUMENT_NAME))
            .findFirst()
            .map(toArgument -> toArgument.getValueWithVariable().getStringValue())
            .orElse(null);

    validateToValue(typeDefinition.getName(), alias);

    return Pair.of(alias, originalName);
  }

  private void validateToValue(String parentName, String toValue) {
    if(StringUtils.isBlank(toValue)) {
      throw new InvalidRenameException(
              String.format("Invalid rename directive for %s: to argument is empty", parentName)
      );
    }

    if(StringUtils.containsWhitespace(toValue)) {
      throw new InvalidRenameException(
              String.format("Invalid rename directive for %s: to argument (%s) cannot contain whitespace", parentName, toValue)
      );
    }

    if(!StringUtils.isAlphanumeric(toValue)) {
      throw new InvalidRenameException(
              String.format("Invalid rename directive for %s: to argument (%s) must be alphanumerical", parentName, toValue)
      );
    }
  }

  private void addRenamedPairToMap(String parentName, Pair<String, String> renamedPair, Map<String, String> renameMap) {
    if(renameMap.containsValue(renamedPair.getKey())) {
      throw new InvalidRenameException(
              String.format("Invalid rename directive for %s: Multiple definitions are renamed with the same name", parentName)
      );
    }

    renameMap.put(renamedPair.getRight(), renamedPair.getLeft());
  }

}
