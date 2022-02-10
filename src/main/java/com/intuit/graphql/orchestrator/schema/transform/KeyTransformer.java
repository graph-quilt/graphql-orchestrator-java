package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.keydirective.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.VisibleForTesting;

import java.util.List;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.XtextUtils.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.typeContainsDirective;

/**
 * This class is responsible for checking the merged graph for any key directives. For each field in key
 *  directive, this class will validate the fields to the key directive by ensuring they exist
 */
public class KeyTransformer implements Transformer<XtextGraph, XtextGraph> {

  @VisibleForTesting
  KeyDirectiveValidator validator = new KeyDirectiveValidator();

  @Override
  public XtextGraph transform(final XtextGraph source) {
    List<TypeDefinition> entities = source.getTypes().values().stream()
            .filter(typeDefinition -> typeContainsDirective(typeDefinition, FEDERATION_KEY_DIRECTIVE))
            .collect(Collectors.toList());

    for (final TypeDefinition typeDefinition : entities) {
      for (final Directive directive : (List<Directive>)typeDefinition.getDirectives()) {
        if(directive.getDefinition().getName().equals(FEDERATION_KEY_DIRECTIVE)) {
          List<Argument> arguments = (List<Argument>)directive.getArguments();
          validator.validateKeyArguments(typeDefinition, arguments);
        }
      }
    }

    return source;
  }


}
