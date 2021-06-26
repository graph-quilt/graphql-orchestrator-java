package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.canContainFieldResolverDirective;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.createFieldResolverContexts;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ArgumentDefinitionNotAllowed;
import com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil;
import com.intuit.graphql.orchestrator.resolverdirective.NotAValidLocationForFieldResolverDirective;
import com.intuit.graphql.orchestrator.utils.XtextTypeUtils;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.emf.ecore.EObject;

public class FieldResolverTransformerPreMerge implements Transformer<XtextGraph, XtextGraph> {

  @Override
  public XtextGraph transform(XtextGraph sourceXtextGraph) {

    List<FieldResolverContext> fieldResolverContexts = new ArrayList<>();
    sourceXtextGraph
        .getTypes().values().stream()
        .filter(FieldResolverDirectiveUtil::isObjectOrInterfaceType)
        .map(typeDefinition -> createFieldResolverContexts(typeDefinition, sourceXtextGraph))
        .forEach(fieldResolverContexts::addAll);

    if (CollectionUtils.isNotEmpty(fieldResolverContexts)) {
      validateFieldWithResolver(fieldResolverContexts);
      return sourceXtextGraph.transform(builder -> {
          builder.hasFieldResolverDefinition(true);
          builder.fieldResolverContexts(fieldResolverContexts);
      });
    }

    return sourceXtextGraph;
  }

  private void validateFieldWithResolver(List<FieldResolverContext> fieldResolverContexts) {
    fieldResolverContexts
        .forEach(fieldResolverContext -> {
          FieldDefinition fieldDefinition = fieldResolverContext.getFieldDefinition();
          String fieldName = fieldDefinition.getName();
          String parentTypeName = XtextTypeUtils.getParentTypeName(fieldDefinition);

          EObject parentType = fieldDefinition.eContainer();
          if (!(canContainFieldResolverDirective(parentType))) {
            throw new NotAValidLocationForFieldResolverDirective(fieldName, parentTypeName);
          }

          if (hasArguments(fieldDefinition)) {
            throw new ArgumentDefinitionNotAllowed(fieldName, parentTypeName);
          }
        });
  }

  private boolean hasArguments(FieldDefinition fieldDefinition) {
    return Objects.nonNull(fieldDefinition.getArgumentsDefinition());
  }

}
