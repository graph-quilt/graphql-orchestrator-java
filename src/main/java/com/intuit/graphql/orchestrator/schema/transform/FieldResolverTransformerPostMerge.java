package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.*;
import com.intuit.graphql.orchestrator.fieldresolver.FieldResolverException;
import com.intuit.graphql.orchestrator.fieldresolver.ResolverArgumentDefinitionValidator;
import com.intuit.graphql.orchestrator.resolverdirective.ExternalTypeNotfoundException;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.utils.XtextTypeUtils;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.XtextGraphUtils.addToCodeRegistry;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.*;
import static com.intuit.graphql.utils.XtextTypeUtils.unwrapAll;
import static java.util.stream.Collectors.toMap;

public class FieldResolverTransformerPostMerge implements Transformer<XtextGraph, XtextGraph> {

  @Override
  public XtextGraph transform(XtextGraph sourceXtextGraph) {
    if (CollectionUtils.isNotEmpty(sourceXtextGraph.getFieldResolverContexts())) {
      List<FieldResolverContext> fieldResolverContexts = sourceXtextGraph.getFieldResolverContexts()
              .stream()
              .peek(fieldResolverContext -> replacePlaceholderTypeWithActual(fieldResolverContext, sourceXtextGraph))
              .map(fieldResolverContext -> updateWithTargetFieldData(fieldResolverContext, sourceXtextGraph))
              .collect(Collectors.toList());
      XtextGraph newXtextGraph = sourceXtextGraph.transform(builder -> builder
              .clearFieldResolverContexts()
              .fieldResolverContexts(fieldResolverContexts)
      );
      fieldResolverContexts
              .forEach(fieldResolverContext -> {
                FieldContext fieldContext = new FieldContext(fieldResolverContext.getParentTypename(), fieldResolverContext.getFieldName());
                DataFetcherContext dataFetcherContext = createDataFetcherContext(fieldResolverContext);
                addToCodeRegistry(fieldContext, dataFetcherContext, newXtextGraph);
              });

      return newXtextGraph;
    }

    return sourceXtextGraph;
  }

  private FieldResolverContext updateWithTargetFieldData(final FieldResolverContext fieldResolverContext, final XtextGraph sourceXtextGraph) {

    final ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();

    final FieldDefinition targetFieldDefinition = getFieldDefinitionByFQN(resolverDirectiveDefinition.getField(), sourceXtextGraph);

    List<InputValueDefinition> targetFieldInputValueDefinitions = Collections.emptyList();
    if (Objects.nonNull(targetFieldDefinition.getArgumentsDefinition())) {
      targetFieldInputValueDefinitions = targetFieldDefinition.getArgumentsDefinition().getInputValueDefinition();
    }

    List<ResolverArgumentDefinition> updatedResolverArgumentDefinitions = updateWithInputType(
            resolverDirectiveDefinition.getArguments(), targetFieldInputValueDefinitions, fieldResolverContext);

    String targetFieldFQN = resolverDirectiveDefinition.getField();
    return fieldResolverContext.transform(builder -> builder
            .targetFieldDefinition(targetFieldDefinition)
            .targetFieldContext(new FieldContext(getParentTypeName(targetFieldDefinition), targetFieldDefinition.getName()))
            .resolverDirectiveDefinition(new ResolverDirectiveDefinition(targetFieldFQN, updatedResolverArgumentDefinitions))
    );

  }

  private DataFetcherContext createDataFetcherContext(final FieldResolverContext fieldResolverContext) {
    return DataFetcherContext
      .newBuilder()
      .dataFetcherType(DataFetcherContext.DataFetcherType.RESOLVER_ON_FIELD_DEFINITION)
      .fieldResolverContext(fieldResolverContext)
      .build();
  }

  private List<ResolverArgumentDefinition> updateWithInputType(List<ResolverArgumentDefinition> resolverArgumentDefinitions,
                                                               List<InputValueDefinition> targetFieldInputValueDefinitions,
                                                               FieldResolverContext fieldResolverContext) {

    if (resolverArgumentDefinitions.size() > targetFieldInputValueDefinitions.size()) {
      String errorMessage = "Field resolver arguments size is greater than of target field definition.";
      throw new FieldResolverException(errorMessage, fieldResolverContext);
    }

    if (CollectionUtils.isEmpty(resolverArgumentDefinitions)) {
      return resolverArgumentDefinitions;
    }

    Map<String, ResolverArgumentDefinition> resolverArgumentDefinitionMap = resolverArgumentDefinitions.stream()
      .collect(toMap(ResolverArgumentDefinition::getName, thisObject -> thisObject));

    List<ResolverArgumentDefinition> updatedList = new ArrayList<>();

    for (InputValueDefinition inputValueDefinition : targetFieldInputValueDefinitions) {
      String argumentName = inputValueDefinition.getName();

      ResolverArgumentDefinition resolverArgumentDefinition = resolverArgumentDefinitionMap.get(argumentName);
      ResolverArgumentDefinitionValidator resolverArgumentDefinitionValidator = new ResolverArgumentDefinitionValidator(
              resolverArgumentDefinition, inputValueDefinition, fieldResolverContext
      );

      resolverArgumentDefinitionValidator.validate();

      ResolverArgumentDefinition newResolverArgumentDefinition = resolverArgumentDefinition.transform(builder -> builder.namedType(inputValueDefinition.getNamedType()));
      updatedList.add(newResolverArgumentDefinition);
    }

    return updatedList;

  }

  private void replacePlaceholderTypeWithActual(FieldResolverContext fieldResolverContext,
                                                XtextGraph sourceXtextGraph) {

    FieldDefinition fieldDefinition = fieldResolverContext.getFieldDefinition();
    NamedType fieldType = fieldDefinition.getNamedType();

    if (!XtextTypeUtils.isPrimitiveType(fieldType)) {
      TypeDefinition actualTypeDefinition = sourceXtextGraph.getType(fieldType);
      if (Objects.isNull(actualTypeDefinition)) {
        String serviceName = fieldResolverContext.getServiceNamespace();
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

  private FieldDefinition getFieldDefinitionByFQN(final String queryFieldFQN, XtextGraph xtextGraph) {

    String queryFieldFQNNoQuery = StringUtils.removeStart(queryFieldFQN ,"query."); // remove if exists

    String[] queryFieldFQNTokens = StringUtils.split(queryFieldFQNNoQuery, '.');
    if (ArrayUtils.isEmpty(queryFieldFQNTokens)) {
      String errorMessage = String.format("Failed to tokenize queryFieldFQN.  queryFieldFQN=%s", queryFieldFQN);
      throw new IllegalArgumentException(errorMessage);
    }

    FieldDefinition fieldDefinition = null;

    TypeDefinition currentParentType = xtextGraph.getOperationMap().get(Operation.QUERY);
    for (int i = 0; i < queryFieldFQNTokens.length; i++) {
      String fieldName = queryFieldFQNTokens[i];
      List<FieldDefinition> fieldDefinitionList = getFieldDefinitions(currentParentType);
      fieldDefinition = findFirstFieldDefinitionByName(fieldDefinitionList, fieldName)
              .orElseThrow(() -> {
                String errorMessage = String.format("Field definition for field %s not found " +
                        "in xtextGraph.  Ensure that queryFieldFQN is valid.  queryFieldFQN=%s", fieldName, queryFieldFQN);
                return new IllegalArgumentException(errorMessage);
              });

      NamedType unwrappedNamedType = unwrapAll(fieldDefinition.getNamedType());

      if (isPrimitiveType(unwrappedNamedType)) {
        if (i != (queryFieldFQNTokens.length - 1)) {
          String errorMessage = String.format("Invalid queryFieldFQN=%s. field=%s is of primitive type but has sub-fields" +
                  " defined in fqn.",  queryFieldFQN, fieldName);
          throw new IllegalArgumentException(errorMessage);
        }
      } else if (isObjectType(unwrappedNamedType)) {
        currentParentType =  ((ObjectType)unwrappedNamedType).getType();
      } else {
        String errorMessage = "Unsupported NamedType: " + unwrappedNamedType.getClass().getName();
        throw new IllegalArgumentException(errorMessage);
      }
    }

    return fieldDefinition;
  }

  private static Optional<FieldDefinition> findFirstFieldDefinitionByName(List<FieldDefinition> fieldDefinitionList, String fieldName) {
    return fieldDefinitionList
            .stream()
            .filter(fd -> StringUtils.equals(fd.getName(), fieldName))
            .findFirst();
  }

}
