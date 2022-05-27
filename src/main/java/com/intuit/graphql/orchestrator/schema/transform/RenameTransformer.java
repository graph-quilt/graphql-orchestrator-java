package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.OperationType;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static com.intuit.graphql.orchestrator.utils.RenameDirectiveUtil.getRenameKey;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.RENAME_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getDirectiveWithNameFromDefinition;

public class RenameTransformer implements Transformer<XtextGraph, XtextGraph> {

  private final String RENAME_ARGUMENT_NAME = "to";

  @Override
  public XtextGraph transform(XtextGraph xtextGraph) {
    Map<String, String> renamedTypeMap = new HashMap<>();
    Map<String, String> renamedFieldMap = new HashMap<>();

    XtextUtils.getAllTypes(xtextGraph.getXtextResourceSet())
        .forEach(typeDefinition -> {
            getDirectiveWithNameFromDefinition(typeDefinition, RENAME_DIRECTIVE)
              .ifPresent(typeRenameDirective -> addTypeToMap(typeDefinition, typeRenameDirective, renamedTypeMap)
          );

          getFieldDefinitions(typeDefinition, true).forEach(fieldDefinition ->
            getDirectiveWithNameFromDefinition(fieldDefinition, RENAME_DIRECTIVE)
              .ifPresent(directive -> addFieldToMap(typeDefinition.getName(), fieldDefinition, directive, renamedFieldMap))
          );
    });

    if(!renamedFieldMap.isEmpty() || !renamedTypeMap.isEmpty()) {
      return xtextGraph.transform(builder -> builder
              .originalTypeNamesByRenamedName(renamedTypeMap)
              .originalFieldNamesByRenamedName(renamedFieldMap)
              .containsRenamedFields(renamedFieldMap.isEmpty())
      );
    }

    return xtextGraph;
  }

  private void addTypeToMap(TypeDefinition typeDefinition, Directive renameDirective, Map<String, String> renamedTypeMap){
    String originalName = typeDefinition.getName();
    String alias = renameDirective.getArguments().stream()
            .filter(argument -> argument.getName().equals(RENAME_ARGUMENT_NAME))
            .findFirst()
            .map(toArgument -> toArgument.getValueWithVariable().getStringValue())
            .orElse(null);

    validateToValue(alias);

    addKeyToMap(alias, originalName, renamedTypeMap);
    typeDefinition.setName(alias);
  }

  private void addFieldToMap(String parentName, FieldDefinition fieldDefinition,
                             Directive renameDirective, Map<String, String> renamedFieldMap) {
    String originalName = fieldDefinition.getName();
    String alias = renameDirective.getArguments().stream()
            .filter(argument -> argument.getName().equals(RENAME_ARGUMENT_NAME))
            .findFirst()
            .map(toArgument -> toArgument.getValueWithVariable().getStringValue())
            .orElse(null);

    validateToValue(alias);

    String metadataKey = getRenameKey(parentName, alias, (OperationType.get(parentName.toLowerCase()) != null));

    addKeyToMap(metadataKey, originalName, renamedFieldMap);
    fieldDefinition.setName(alias);
  }

  private void validateToValue(String toValue) {
    if(StringUtils.isBlank(toValue)) {
      throw new RuntimeException("to argument is empty for %s");
    }

    if(StringUtils.containsWhitespace(toValue)) {
      throw new RuntimeException("to argument for cannot %s contain whitespace");
    }
  }

  private void addKeyToMap(String key, String value, Map<String, String> renameMap) {
    if(renameMap.containsKey(key)) {
      throw new RuntimeException("Multiple renames with the same name");
    }

    renameMap.put(key, value);
  }

}
