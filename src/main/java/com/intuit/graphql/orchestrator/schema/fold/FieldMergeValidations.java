package com.intuit.graphql.orchestrator.schema.fold;

import static com.intuit.graphql.orchestrator.utils.XtextUtils.isObjectType;
import static com.intuit.graphql.utils.XtextTypeUtils.typeName;

import com.intuit.graphql.graphQL.ArgumentsDefinition;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InputValueDefinition;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import graphql.VisibleForTesting;
import java.util.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.common.util.EList;

public class FieldMergeValidations {

  private static final String FIELD_NOT_ELIGIBLE = "Nested fields (parentType:%s, field:%s) are not eligible to merge. Reason: %s";
  private static final String NOT_OBJECT_TYPE = "Type %s is not an ObjectType";
  private static final String TYPE_MISMATCH = "Type %s is not same as Type %s";
  private static final String UNEQUAL_DIRECTIVES = "Unequal directives: %s is not same as %s";
  private static final String MISSING_DIRECTIVE = "Missing directive: %s";
  private static final String MISSING_DIRECTIVE_LOCATION = "Missing directive location: %s";
  private static final String UNEQUAL_DIRECTIVE_LOCATIONS = "Unequal directive locations: %s is not same as %s";
  private static final String UNEQUAL_ARGUMENTS = "Unequal arguments: %s is not same as %s";
  private static final String MISSING_ARGUMENT = "Missing argument: %s:%s";


  private FieldMergeValidations() {
  }

  public static void fieldHasSameArguments(String parent, FieldDefinition current, FieldDefinition newcomer) {

    Objects.requireNonNull(current);
    Objects.requireNonNull(newcomer);

    ArgumentsDefinition curArgsDefinition = current.getArgumentsDefinition();
    ArgumentsDefinition newArgsDefinition = newcomer.getArgumentsDefinition();

    try {
      compareArguments(curArgsDefinition, newArgsDefinition);
    } catch (FieldMergeException fme) {
      throw new FieldMergeException(String.format(FIELD_NOT_ELIGIBLE, parent, current.getName(), fme.getMessage()));
    }
  }

  public static void compareArguments(ArgumentsDefinition argDef1, ArgumentsDefinition argDef2) {
    //todo: compare default value for args?
    int argsSize1 = argsize(argDef1);
    int argsSize2 = argsize(argDef2);

    if (argsSize1 > 0 || argsSize2 > 0) {

      //compare size
      if (argsSize1 != argsSize2) {
        throw new FieldMergeException(String.format(UNEQUAL_ARGUMENTS, argsSize1, argsSize2));
      }

      //compare names and types
      argDef1.getInputValueDefinition().forEach(leftArg -> {

        //compare names
        InputValueDefinition rightArg = argDef2.getInputValueDefinition().stream()
            .filter(t -> leftArg.getName().equals(t.getName())).findFirst()
            .orElseThrow(() -> new FieldMergeException(String
                .format(MISSING_ARGUMENT, leftArg.getName(), XtextUtils.toDescriptiveString(leftArg.getNamedType()))));

        //compare types
        if (!StringUtils.equals(typeName(leftArg.getNamedType()), typeName(rightArg.getNamedType()))) {
          throw new FieldMergeException(
              String.format(UNEQUAL_ARGUMENTS, XtextUtils.toDescriptiveString(leftArg.getNamedType()),
                  XtextUtils.toDescriptiveString(rightArg.getNamedType())));
        }

        //compare directives
        compareDirectives(leftArg.getDirectives(), rightArg.getDirectives());
      });
    }
  }

  public static int argsize(ArgumentsDefinition argumentsDefinition) {
    if (Objects.nonNull(argumentsDefinition)) {
      return CollectionUtils.size(argumentsDefinition.getInputValueDefinition());
    }
    return 0;
  }

  public static void fieldHasSameDirectives(String parent, FieldDefinition current, FieldDefinition newcomer) {

    Objects.requireNonNull(current);
    Objects.requireNonNull(newcomer);

    try {
      compareDirectives(current.getDirectives(), newcomer.getDirectives());
    } catch (FieldMergeException fme) {
      throw new FieldMergeException(String.format(FIELD_NOT_ELIGIBLE, parent, current.getName(), fme.getMessage()));
    }
  }

  public static void compareDirectives(EList<Directive> directives1, EList<Directive> directives2) {

    int size1 = CollectionUtils.size(directives1);
    int size2 = CollectionUtils.size(directives2);

    if (size1 > 0 || size2 > 0) {
      //compare size
      if (size1 != size2) {
        throw new FieldMergeException(String.format(UNEQUAL_DIRECTIVES, size1, size2));
      }

      //compare names and types
      directives1.forEach(leftDirective -> {
        //compare names
        Directive rightDirective = directives2.stream()
            .filter(t -> leftDirective.getDefinition().getName().equals(t.getDefinition().getName())).findFirst()
            .orElseThrow(() -> new FieldMergeException(
                String.format(MISSING_DIRECTIVE, leftDirective.getDefinition().getName())));

        //compare arguments
        compareArguments(leftDirective.getDefinition().getArgumentsDefinition(),
            rightDirective.getDefinition().getArgumentsDefinition());

        int llocSize = CollectionUtils.size(leftDirective.getDefinition().getDirectiveLocations());
        int rlocSize = CollectionUtils.size(rightDirective.getDefinition().getDirectiveLocations());

        if (llocSize != rlocSize) {
          throw new FieldMergeException(
              String.format(UNEQUAL_DIRECTIVE_LOCATIONS, llocSize, rlocSize));
        }

        if (llocSize > 0 || rlocSize > 0) {
          leftDirective.getDefinition().getDirectiveLocations().stream()
              .forEach(lloc -> rightDirective.getDefinition().getDirectiveLocations().stream()
                  .filter(rloc -> lloc.getNamedDirective().equals(rloc.getNamedDirective())).findFirst()
                  .orElseThrow(() -> new FieldMergeException(
                      String.format(MISSING_DIRECTIVE_LOCATION, lloc.getNamedDirective()))));
        }
      });
    }
  }

  public static void checkMergeEligibility(String parentType, FieldDefinition current, FieldDefinition newcomer) {

    NamedType currentNamedType = current.getNamedType();
    NamedType newcomerNamedType = newcomer.getNamedType();

    if (!StringUtils.equals(typeName(newcomerNamedType), typeName(currentNamedType))) {
      throw new FieldMergeException(String.format(FIELD_NOT_ELIGIBLE, parentType, current.getName(),
          String.format(TYPE_MISMATCH, XtextUtils.toDescriptiveString(currentNamedType),
              XtextUtils.toDescriptiveString(newcomerNamedType))));
    }
    checkObjectType(parentType, current);
    checkObjectType(parentType, newcomer);
    fieldHasSameArguments(parentType, current, newcomer);
    fieldHasSameDirectives(parentType, current, newcomer);
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
