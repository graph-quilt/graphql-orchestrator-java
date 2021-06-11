package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.RESOLVER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.isReferenceToFieldInParentType;
import static com.intuit.graphql.orchestrator.utils.XtextGraphUtils.addToCodeRegistry;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.createNamedType;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ListType;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ExternalTypeNotfoundException;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgument;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentNotAFieldOfParentException;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.utils.XtextTypeUtils;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

public class FieldResolverTransformerPostMerge implements Transformer<XtextGraph, XtextGraph> {

  @Override
  public XtextGraph transform(XtextGraph sourceXtextGraph) {
    sourceXtextGraph.getFieldWithResolverMetadatas()
        .forEach(fieldWithResolverMetadata -> process(fieldWithResolverMetadata, sourceXtextGraph));

    return sourceXtextGraph;
  }

  private void process(FieldWithResolverMetadata fieldWithResolverMetadata, XtextGraph sourceXtextGraph) {
    replacePlaceholderTypeWithActual(fieldWithResolverMetadata, sourceXtextGraph);

    fieldWithResolverMetadata.getFieldDefinition()
        .getDirectives()
        .forEach(directive -> {
          if (StringUtils.equals(RESOLVER_DIRECTIVE_NAME, directive.getDefinition().getName())) {
            ResolverDirectiveDefinition resolverDirectiveDefinition = ResolverDirectiveDefinition
                .from(directive);

              validateResolverArgumentsAreFieldsOfParent(resolverDirectiveDefinition.getArguments(),
                fieldWithResolverMetadata.getParentTypeDefinition());

            DataFetcherContext dataFetcherContext = DataFetcherContext
                .newBuilder()
                .dataFetcherType(DataFetcherContext.DataFetcherType.RESOLVER_ON_FIELD_DEFINITION)
                .fieldResolverDirectiveDefinition(resolverDirectiveDefinition)
                .fieldWithResolverMetadata(fieldWithResolverMetadata)
                .build();

            addToCodeRegistry(fieldWithResolverMetadata.getFieldContext(), dataFetcherContext,
                sourceXtextGraph);
          }
        });
  }

  private void validateResolverArgumentsAreFieldsOfParent(List<ResolverArgument> arguments,
      TypeDefinition parentTypeDefinition) {

    arguments.forEach(resolverArgument -> {
      String fieldName = resolverArgument.getValue();
      if (!isReferenceToFieldInParentType(resolverArgument.getValue(), parentTypeDefinition)) {
        throw new ResolverArgumentNotAFieldOfParentException(fieldName, parentTypeDefinition.getName());
      }
    });
  }

  private void replacePlaceholderTypeWithActual(FieldWithResolverMetadata fieldWithResolverMetadata,
      XtextGraph sourceXtextGraph) {

    FieldDefinition fieldDefinition = fieldWithResolverMetadata.getFieldDefinition();
    NamedType fieldType = fieldDefinition.getNamedType();

    if (!XtextTypeUtils.isPrimitiveType(fieldType)) {
      TypeDefinition actualTypeDefinition = sourceXtextGraph.getType(fieldType);
      if (Objects.isNull(actualTypeDefinition)) {
        String serviceName = fieldWithResolverMetadata.getServiceNamespace();
        String parentTypeName = XtextTypeUtils.getParentTypeName(fieldDefinition);
        String fieldName = fieldDefinition.getName();
        String placeHolderTypeDescription = XtextUtils.toDescriptiveString(fieldType);

        throw new ExternalTypeNotfoundException(serviceName, parentTypeName, fieldName, placeHolderTypeDescription);
      }

      if (XtextTypeUtils.isObjectType(fieldType)) {
        fieldDefinition.setNamedType(createNamedType(actualTypeDefinition));
      }

      if (XtextTypeUtils.isListType(fieldType)) {
        ListType listType = GraphQLFactoryDelegate.createListType();
        listType.setType(createNamedType(actualTypeDefinition));
        fieldDefinition.setNamedType(listType);
      }
    }

    // else primitive, no type replacement needed
  }

}
