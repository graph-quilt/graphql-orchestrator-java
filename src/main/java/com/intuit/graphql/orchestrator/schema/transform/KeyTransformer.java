package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.keydirective.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.VisibleForTesting;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.FederationUtils.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.typeContainsDirective;

/**
 * This class is responsible for checking the merged graph for any key directives. For each field in key
 *  directive, this class will validate the fields to the key directive by ensuring they exist
 */
public class KeyTransformer implements Transformer<XtextGraph, XtextGraph> {

  @VisibleForTesting
  KeyDirectiveValidator keyDirectiveValidator = new KeyDirectiveValidator();

  @Override
  public XtextGraph transform(final XtextGraph source) {

    if(source.getServiceProvider().isFederationProvider()) {
      Map<String, TypeDefinition> entities = source.getTypes().values().stream()
              .filter(typeDefinition -> typeContainsDirective(typeDefinition, FEDERATION_KEY_DIRECTIVE))
              .collect(Collectors.toMap(TypeDefinition::getName, Function.identity()));

      for(final TypeDefinition entityDefinitions : entities.values()) {
        for (final Directive directive : entityDefinitions.getDirectives()) {
          if(directive.getDefinition().getName().equals(FEDERATION_KEY_DIRECTIVE)) {
            List<Argument> arguments = directive.getArguments();
            keyDirectiveValidator.validate(source, entityDefinitions, arguments);
          }
        }
      }
      return source.transform(builder -> builder.entitiesByTypeName(entities));
    } else {
      return source;
    }
  }
}
