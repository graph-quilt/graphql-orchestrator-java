package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.utils.XtextGraphUtils.addToCodeRegistry;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.orchestrator.apollofederation.EntityExtensionContext;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;

public class KeyTransformerPostMerge implements Transformer<XtextGraph, XtextGraph> {

  @Override
  public XtextGraph transform(XtextGraph source) {
    source.getEntityFieldExtensionsMap().keySet().stream()
        .filter(fieldCoordinates -> {
          EntityExtensionContext entityExtensionContext = source.getEntityFieldExtensionsMap().get(fieldCoordinates);
          FieldDefinition fieldDefinition = entityExtensionContext.getFieldDefinition();
          return !containsExternalDirective(fieldDefinition);
        })
        .forEach(fieldCoordinates -> {
          EntityExtensionContext entityExtensionContext = source.getEntityFieldExtensionsMap().get(fieldCoordinates);
          FieldDefinition fieldDefinition = entityExtensionContext.getFieldDefinition();
          FieldContext fieldContext = new FieldContext(fieldCoordinates.getTypeName(), fieldDefinition.getName());
          DataFetcherContext dataFetcherContext = DataFetcherContext
              .newBuilder()
              .dataFetcherType(DataFetcherContext.DataFetcherType.ENTITY_FETCHER)
              .entityExtensionContext(entityExtensionContext)
              .build();
          addToCodeRegistry(fieldContext, dataFetcherContext, source);
        });

    return source;
  }

  private boolean containsExternalDirective(FieldDefinition fieldDefinition) {
    return fieldDefinition.getDirectives().stream()
        .anyMatch(directive -> directive.getDefinition().getName().equals("external"));
  }
}
