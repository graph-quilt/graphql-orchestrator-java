package com.intuit.graphql.orchestrator.resolverdirective;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isAnyTypeLeaf;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.parseString;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InputObjectTypeDefinition;
import com.intuit.graphql.graphQL.InputValueDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;
import graphql.VisibleForTesting;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class helps break up the {@link com.intuit.graphql.orchestrator.schema.transform.ResolverArgumentTransformer} by
 * validating if a FieldDefinition that has a resolver argument directive is valid.
 */
public class ResolverArgumentDirectiveValidator {

  @VisibleForTesting
  ResolverDirectiveTypeResolver typeResolver = new ResolverDirectiveTypeResolver();

  /**
   * Validates a FieldDefinition with resolver argument directives and ensures the argument conforms to the criteria of
   * a valid resolver directive argument:
   *
   * The field in the resolver directive must exist under the query root (e.g. "a.b.c")
   *
   * The InputType of the argument in {@code fieldDefinition} only contains fields that exist in the schema.
   *
   * Leaf types (Enums and Scalars) have the same names as those found in the schema.
   *
   * @param fieldDefinition        FieldDefinition that has arguments with resolver directives
   * @param source                 the graph where the fieldDefinition exists
   * @param definitionFieldContext FieldContext of the TypeDefinition the fieldDefinition exists (for Exception
   *                               Building)
   */
  public void validateField(FieldDefinition fieldDefinition, UnifiedXtextGraph source,
      FieldContext definitionFieldContext)
      throws ResolverArgumentLeafTypeNotSame, ResolverArgumentTypeMismatch, ResolverArgumentFieldNotInSchema {
    for (final InputValueDefinition inputValueDefinition : fieldDefinition.getArgumentsDefinition()
        .getInputValueDefinition()) {
      final TypeDefinition inputTypeDefinition = source.getType(inputValueDefinition.getNamedType());
      final Directive resolverDirective = getResolverDirective(inputValueDefinition);

      if(resolverDirective == null) {
        continue;
      }

      final Map<String, Argument> argumentsByName = getArgumentsByName(resolverDirective.getArguments());
      final String argumentName = inputValueDefinition.getName();
      final String field = parseString(argumentsByName.get("field").getValueWithVariable());

      final TypeDefinition typeInSchema = typeResolver
          .resolveField(field, source, argumentName, definitionFieldContext);

      validateArgumentInputType(source, argumentName, inputTypeDefinition, typeInSchema, definitionFieldContext,
          null);
    }
  }

  private void validateLeafNode(String argumentName, TypeDefinition inputType,
      TypeDefinition typeInSchema, FieldContext rootContext, FieldContext parentContext) {
    if (inputType.getClass().equals(typeInSchema.getClass())) {
      if (!inputType.getName().equals(typeInSchema.getName())) {
        throw ResolverArgumentLeafTypeNotSame.create(argumentName, rootContext, parentContext, inputType.getName(),
            typeInSchema.getName());
      }
      return;
    }

    //one of the types are not leaf node
    throw ResolverArgumentTypeMismatch
        .create(argumentName, rootContext, parentContext, inputType.getName(), typeInSchema.getName());
  }

  private void validateArgumentInputType(final UnifiedXtextGraph source, final String argumentName,
      final TypeDefinition argumentInputType, final TypeDefinition typeInSchema, FieldContext rootContext,
      FieldContext parentContext) {

    if (isAnyTypeLeaf(typeInSchema, argumentInputType)) {
      validateLeafNode(argumentName, argumentInputType, typeInSchema, rootContext, parentContext);
      return;
    }

    final Map<String, FieldDefinition> schemaFieldDefinitionsByName = ((ObjectTypeDefinition) typeInSchema)
        .getFieldDefinition()
        .stream()
        .collect(Collectors.toMap(FieldDefinition::getName, Function.identity()));

    for (final InputValueDefinition inputValueDefinition : ((InputObjectTypeDefinition) argumentInputType)
        .getInputValueDefinition()) {

      FieldDefinition fieldDefinitionInSchema = schemaFieldDefinitionsByName.get(inputValueDefinition.getName());

      if (fieldDefinitionInSchema == null) {
        throw new ResolverArgumentFieldNotInSchema(argumentName, rootContext,
            new FieldContext(argumentInputType.getName(), inputValueDefinition.getName()));
      }

      TypeDefinition childInputType = source.getType(inputValueDefinition.getNamedType());
      TypeDefinition childTypeInSchema = source.getType(fieldDefinitionInSchema.getNamedType());

      validateArgumentInputType(source, argumentName, childInputType, childTypeInSchema, rootContext,
          new FieldContext(argumentInputType.getName(), inputValueDefinition.getName()));
    }
  }

  private Map<String, Argument> getArgumentsByName(List<Argument> arguments) {
    return arguments.stream()
        .collect(Collectors.toMap(Argument::getName, Function.identity()));
  }

  private Directive getResolverDirective(InputValueDefinition argument) {
    return argument.getDirectives().stream().filter(directive -> directive.getDefinition().getName().equals("resolver"))
        .findFirst().orElse(null);
  }
}
