package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.ArgumentsDefinition;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.keydirective.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.VisibleForTesting;

import java.util.List;

import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createArgumentsDefinition;

/**
 * This class is responsible for checking the merged graph for any key directives. For each field in key
 *  directive, this class will validate the fields to the key directive by ensuring they exist
 */
public class KeyTransformer implements Transformer<XtextGraph, XtextGraph> {

  @VisibleForTesting
  KeyDirectiveValidator validator = new KeyDirectiveValidator();

  @Override
  public XtextGraph transform(final XtextGraph source) {

    for (final ObjectTypeDefinition objectTypeDefinition : source.objectTypeDefinitionsByName().values()) {
      for (final Directive directive : (List<Directive>)objectTypeDefinition.getDirectives()) {
        if(directive.getDefinition().getName().equals("key")) {
          List<Argument> arguments = (List<Argument>)directive.getArguments();
          validator.validateKeyArguments(objectTypeDefinition, arguments);
        }
      }
    }

    return source;
  }


}
