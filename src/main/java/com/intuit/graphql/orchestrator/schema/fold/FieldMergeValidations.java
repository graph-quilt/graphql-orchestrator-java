package com.intuit.graphql.orchestrator.schema.fold;

import static com.intuit.graphql.orchestrator.utils.XtextUtils.isObjectType;
import static com.intuit.graphql.utils.XtextTypeUtils.typeName;

import com.intuit.graphql.graphQL.ArgumentsDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import graphql.VisibleForTesting;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class FieldMergeValidations {

  private static final String FIELD_NOT_ELIGIBLE = "Nested fields (parentType:%s, field:%s) are not eligible to merge. Reason: %s";
  private static final String NOT_OBJECT_TYPE = "Type %s is not an ObjectType";
  private static final String TYPE_MISMATCH = "Type %s is not same as Type %s";
  private static final String SHOULD_NOT_CONTAIN_DIRECTIVE = "Field should not contain directives. Found directive/s: %s";
  private static final String SHOULD_NOT_CONTAIN_ARGUMENT = "Field should not contain arguments. Found argument/s: %s";

  private FieldMergeValidations() {
  }

  public static void fieldHasNoArguments(String parent, FieldDefinition... fieldDefinitionList) {
    for (FieldDefinition fieldDefinition : fieldDefinitionList) {
      Objects.requireNonNull(fieldDefinition);
      ArgumentsDefinition argumentsDefinition = fieldDefinition.getArgumentsDefinition();

      if (Objects.nonNull(argumentsDefinition) && CollectionUtils
          .isNotEmpty(argumentsDefinition.getInputValueDefinition())) {

        List<String> argNames = argumentsDefinition.getInputValueDefinition().stream()
            .map(arg -> arg.getName()).collect(Collectors.toList());

        throw new FieldMergeException(String.format(FIELD_NOT_ELIGIBLE, parent, fieldDefinition.getName(),
            String.format(SHOULD_NOT_CONTAIN_ARGUMENT, argNames.toString() )));

      }
    }
  }

  public static void fieldHasNoDirective(String parent, FieldDefinition... fieldDefinitionList) {
    for (FieldDefinition fieldDefinition : fieldDefinitionList) {
      Objects.requireNonNull(fieldDefinition);

      if (CollectionUtils.isNotEmpty(fieldDefinition.getDirectives())) {
        List<String> directiveNames = fieldDefinition.getDirectives().stream()
            .map(directive -> directive.getDefinition())
            .map(directiveDefinition -> directiveDefinition.getName())
            .collect(Collectors.toList());

        throw new FieldMergeException(String.format(FIELD_NOT_ELIGIBLE, parent, fieldDefinition.getName(),
            String.format(SHOULD_NOT_CONTAIN_DIRECTIVE, directiveNames.toString())));
      }
    }
  }


  public static void checkMergeEligiblity(String parentType, FieldDefinition current, FieldDefinition newcomer) {

    NamedType currentNamedType = current.getNamedType();
    NamedType newcomerNamedType = newcomer.getNamedType();

    if (!StringUtils.equals(typeName(newcomerNamedType), typeName(currentNamedType))) {
      throw new FieldMergeException(String.format(FIELD_NOT_ELIGIBLE, parentType, current.getName(),
          String.format(TYPE_MISMATCH, XtextUtils.toDescriptiveString(currentNamedType),
              XtextUtils.toDescriptiveString(newcomerNamedType))));
    }
    checkObjectType(parentType, current);
    checkObjectType(parentType, newcomer);
    fieldHasNoArguments(parentType, current, newcomer);
    fieldHasNoDirective(parentType, current, newcomer);
  }

  @VisibleForTesting
  protected static void checkObjectType(String parent, FieldDefinition fieldDefinition) {
    NamedType namedType = fieldDefinition.getNamedType();
    if (!isObjectType(namedType)) {
      throw new FieldMergeException(String.format(FIELD_NOT_ELIGIBLE, parent, fieldDefinition.getName(),
          String.format(NOT_OBJECT_TYPE, XtextUtils.toDescriptiveString(namedType))));
    }
  }

}
