package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.ScalarTypeDefinition;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextScalars;

/**
 * Removes any default types that were used for informational purposes when modifying the XtextGraph. Default types
 * should be removed as they conflict with pre-defined default types provided by GraphQL-Java. Trying to re-define
 * default types will result in validation errors at runtime.
 */
public class DefaultTypesRemoverTransformer implements Transformer<XtextGraph, XtextGraph> {

  @Override
  public XtextGraph transform(final XtextGraph source) {
    for (final ScalarTypeDefinition standardScalar : XtextScalars.STANDARD_SCALARS) {
      source.removeType(standardScalar);
    }

    return source;
  }
}
