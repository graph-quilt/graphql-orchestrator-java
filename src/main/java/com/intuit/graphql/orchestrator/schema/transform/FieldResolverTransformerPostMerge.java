package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.utils.XtextGraphUtils.addToCodeRegistry;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.createNamedType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getParentTypeName;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isObjectType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isPrimitiveType;
import static com.intuit.graphql.utils.XtextTypeUtils.unwrapAll;
import static java.util.stream.Collectors.toMap;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InputValueDefinition;
import com.intuit.graphql.graphQL.ListType;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.fieldresolver.FieldResolverException;
import com.intuit.graphql.orchestrator.fieldresolver.ResolverArgumentDefinitionValidator;
import com.intuit.graphql.orchestrator.resolverdirective.ExternalTypeNotfoundException;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentNotAFieldOfParentException;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.TypeMetadata;
import com.intuit.graphql.orchestrator.utils.XtextTypeUtils;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class FieldResolverTransformerPostMerge implements Transformer<XtextGraph, XtextGraph> {

  @Override
  public XtextGraph transform(XtextGraph sourceXtextGraph) {
    if (CollectionUtils.isNotEmpty(sourceXtextGraph.getFieldResolverContexts())) {
      List<FieldResolverContext> fieldResolverContexts = sourceXtextGraph.getFieldResolverContexts()
              .stream()
              .peek(this::validateRequiredFields)
              .peek(fieldResolverContext -> validateTargetTypeExists(fieldResolverContext,sourceXtextGraph))
              .peek(fieldResolverContext -> validateFieldResolverType(fieldResolverContext,sourceXtextGraph))
              .peek(fieldResolverContext -> replacePlaceholderTypeWithActual(fieldResolverContext, sourceXtextGraph))
              .map(fieldResolverContext -> updateWithTargetFieldData(fieldResolverContext, sourceXtextGraph))
              .peek(fieldResolverContext -> addToParentTypeMetadata(fieldResolverContext, sourceXtextGraph))
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

  private void validateRequiredFields(FieldResolverContext fieldResolverContext) {
    fieldResolverContext.getRequiredFields().forEach(reqdFieldName -> {
      if (!fieldResolverContext.getParentTypeFields().containsKey(reqdFieldName)) {
        String serviceName = fieldResolverContext.getServiceNamespace();
        String parentTypeName = fieldResolverContext.getParentTypename();
        String fieldResolverName = fieldResolverContext.getFieldName();
        throw new ResolverArgumentNotAFieldOfParentException(reqdFieldName, serviceName,
            parentTypeName, fieldResolverName);
      }
    });
  }

  private void addToParentTypeMetadata(FieldResolverContext fieldResolverContext, XtextGraph xtextGraph) {
    Map<String, TypeMetadata> typeMetadatas = xtextGraph.getTypeMetadatas();
    TypeMetadata parentTypeMetadata = typeMetadatas.get(fieldResolverContext.getParentTypename());
    parentTypeMetadata.addFieldResolverContext(fieldResolverContext);
  }

  private void validateTargetTypeExists(FieldResolverContext fieldResolverContext, XtextGraph sourceXtextGraph) {
    FieldDefinition fieldDefinition = fieldResolverContext.getFieldDefinition();
    NamedType fieldType = fieldDefinition.getNamedType();
    if (!XtextTypeUtils.isPrimitiveType(fieldType)) {
      TypeDefinition actualTypeDefinition = sourceXtextGraph.getType(fieldType);
      if (Objects.isNull(actualTypeDefinition)) {
        String serviceName = fieldResolverContext.getServiceNamespace();
        String parentTypeName = XtextTypeUtils.getParentTypeName(fieldDefinition);
        String fieldName = fieldDefinition.getName();
        String placeHolderTypeDescription = XtextTypeUtils.toDescriptiveString(fieldType);

        throw new ExternalTypeNotfoundException(serviceName, parentTypeName, fieldName, placeHolderTypeDescription);
      }
    }
  }

  private void validateFieldResolverType(FieldResolverContext fieldResolverContext, XtextGraph sourceXtextGraph) {
    final ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();
    final FieldDefinition targetFieldDefinition = getFieldDefinitionByFQN(resolverDirectiveDefinition.getField(), sourceXtextGraph);
    NamedType fieldResolverType = fieldResolverContext.getFieldDefinition().getNamedType();
    NamedType targetFieldType = targetFieldDefinition.getNamedType();

    if (!XtextTypeUtils.isCompatible(fieldResolverType, targetFieldType)) {
      String errorMessage = "The type of field with @resolver is not compatible with target field type.";
      throw new FieldResolverException(errorMessage, fieldResolverContext);
    }
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
      if (Objects.isNull(resolverArgumentDefinition)) {
        continue;
      }

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
