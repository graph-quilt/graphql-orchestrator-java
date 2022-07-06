package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.ArgumentsDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InputValueDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDirectiveValidator;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;
import graphql.VisibleForTesting;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.RESOLVER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createArgumentsDefinition;

/**
 * This class is responsible for checking the merged graph for any resolver argument directives. If there are resolver
 * argument directives on any field, this class will validate the inputs to the resolver argument directive, then
 * transform the graph (if the validation checks pass).
 */
public class ResolverArgumentTransformer implements Transformer<UnifiedXtextGraph, UnifiedXtextGraph> {

  @VisibleForTesting
  ResolverArgumentDirectiveValidator validator = new ResolverArgumentDirectiveValidator();

  private boolean hasResolverArguments(FieldDefinition fieldDefinition) {
    final List<InputValueDefinition> arguments = Optional
        .ofNullable(fieldDefinition.getArgumentsDefinition())
        .map(ArgumentsDefinition::getInputValueDefinition)
        // Stream type conversion is wonky here, it doesn't like EList -> List, so we're casting
        .map(list -> (List<InputValueDefinition>) list)
        .orElse(Collections.emptyList());

    return arguments.stream()
        .anyMatch(inputValueDefinition -> inputValueDefinition.getDirectives().stream()
            .anyMatch(directive -> directive.getDefinition().getName().equals(RESOLVER_DIRECTIVE_NAME)));
  }

  @Override
  public UnifiedXtextGraph transform(final UnifiedXtextGraph source) {

    for (final ObjectTypeDefinition objectTypeDefinition : source.objectTypeDefinitionsByName().values()) {
      for (final FieldDefinition fieldDefinition : objectTypeDefinition.getFieldDefinition()) {
        if (hasResolverArguments(fieldDefinition)) {
          FieldContext fieldContext = new FieldContext(objectTypeDefinition.getName(), fieldDefinition.getName());

          validator.validateField(fieldDefinition, source, fieldContext);
          transformGraph(source, fieldContext, fieldDefinition);
        }
      }
    }

    return source;
  }

  private void transformGraph(UnifiedXtextGraph source, FieldContext fieldContext, FieldDefinition fieldDefinition) {

    ArgumentsDefinition argumentsDefinition = createArgumentsDefinition();

    argumentsDefinition.getInputValueDefinition()
        .addAll(fieldDefinition.getArgumentsDefinition().getInputValueDefinition());

    source.getResolverArgumentFields().put(fieldContext, argumentsDefinition);

    fieldDefinition.getArgumentsDefinition().getInputValueDefinition().clear();

    DataFetcherContext dataFetcherContext = source.getCodeRegistry().get(fieldContext);

    DataFetcherContext newContext = DataFetcherContext.newBuilder(dataFetcherContext)
        .dataFetcherType(DataFetcherType.RESOLVER_ARGUMENT).build();

    source.getCodeRegistry().put(fieldContext, newContext);
  }
}
