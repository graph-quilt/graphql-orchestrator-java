package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.xtext.XtextScalars.STANDARD_SCALARS;

import com.intuit.graphql.graphQL.ScalarTypeDefinition;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;

/**
 * This class adds any default types based on the GraphQL Specification standard types, GraphQL-Java standard types, and
 * any custom defined standard types.
 *
 * n.b. Standard GraphQL scalars must be removed before the executable schema is made with GraphQL-Java library
 * classes.
 */
public class DefaultTypesAdderTransformer implements Transformer<XtextGraph, XtextGraph> {

  @Override
  public XtextGraph transform(final XtextGraph source) {
    for (final ScalarTypeDefinition standardScalar : STANDARD_SCALARS) {
      source.addType(standardScalar);
    }

    return source;
  }
}
