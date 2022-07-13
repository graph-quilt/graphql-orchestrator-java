package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InputValueDefinition;
import com.intuit.graphql.graphQL.ListType;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.fieldresolver.FieldResolverException;
import com.intuit.graphql.orchestrator.fieldresolver.FieldResolverGraphQLError;
import com.intuit.graphql.orchestrator.fieldresolver.FieldResolverValidator;
import com.intuit.graphql.orchestrator.fieldresolver.ResolverArgumentDefinitionValidator;
import com.intuit.graphql.orchestrator.resolverdirective.ExternalTypeNotfoundException;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.TypeMetadata;
import com.intuit.graphql.orchestrator.utils.XtextTypeUtils;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.FQN_FIELD_SEPARATOR;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.FQN_KEYWORD_QUERY;
import static com.intuit.graphql.orchestrator.utils.XtextGraphUtils.addToCodeRegistry;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.createNamedType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitionFromObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getParentTypeName;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isObjectType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isPrimitiveType;
import static com.intuit.graphql.utils.XtextTypeUtils.unwrapAll;
import static java.util.stream.Collectors.toMap;
public class FieldResolverTransformerPostMerge implements Transformer<UnifiedXtextGraph, UnifiedXtextGraph> {

  @Override
  public UnifiedXtextGraph transform(UnifiedXtextGraph sourceUnifiedXtextGraph) {
    if (CollectionUtils.isNotEmpty(sourceUnifiedXtextGraph.getFieldResolverContexts())) {
      List<FieldResolverContext> fieldResolverContexts = sourceUnifiedXtextGraph.getFieldResolverContexts()
              .stream()
              .peek(FieldResolverValidator::validateRequiredFields)
              .peek(fieldResolverContext -> validateTargetTypeExists(fieldResolverContext,sourceUnifiedXtextGraph))
              .peek(fieldResolverContext -> validateFieldResolverType(fieldResolverContext,sourceUnifiedXtextGraph))
              .peek(fieldResolverContext -> replacePlaceholderTypeWithActual(fieldResolverContext, sourceUnifiedXtextGraph))
              .map(fieldResolverContext -> updateWithTargetFieldData(fieldResolverContext, sourceUnifiedXtextGraph))
              .peek(fieldResolverContext -> addToParentTypeMetadata(fieldResolverContext, sourceUnifiedXtextGraph))
              .collect(Collectors.toList());
      UnifiedXtextGraph newUnifiedXtextGraph = sourceUnifiedXtextGraph.transform(builder -> builder
              .clearFieldResolverContexts()
              .fieldResolverContexts(fieldResolverContexts)
      );
      fieldResolverContexts
              .forEach(fieldResolverContext -> {
                FieldContext fieldContext = new FieldContext(fieldResolverContext.getParentTypename(), fieldResolverContext.getFieldName());
                DataFetcherContext dataFetcherContext = createDataFetcherContext(fieldResolverContext);
                addToCodeRegistry(fieldContext, dataFetcherContext, newUnifiedXtextGraph);
              });

      return newUnifiedXtextGraph;
    }
    return sourceUnifiedXtextGraph;
  }

  private void addToParentTypeMetadata(FieldResolverContext fieldResolverContext, UnifiedXtextGraph unifiedXtextGraph) {
    Map<String, TypeMetadata> typeMetadatas = unifiedXtextGraph.getTypeMetadatas();
    TypeMetadata parentTypeMetadata = typeMetadatas.get(fieldResolverContext.getParentTypename());
    parentTypeMetadata.addFieldResolverContext(fieldResolverContext);
  }

  private void validateTargetTypeExists(FieldResolverContext fieldResolverContext, UnifiedXtextGraph unifiedXtextGraph) {
    FieldDefinition fieldDefinition = fieldResolverContext.getFieldDefinition();
    NamedType fieldType = fieldDefinition.getNamedType();
    if (!XtextTypeUtils.isPrimitiveType(fieldType)) {
      TypeDefinition actualTypeDefinition = unifiedXtextGraph.getType(fieldType);
      if (Objects.isNull(actualTypeDefinition)) {
        String serviceName = fieldResolverContext.getServiceNamespace();
        String parentTypeName = XtextTypeUtils.getParentTypeName(fieldDefinition);
        String fieldName = fieldDefinition.getName();
        String placeHolderTypeDescription = XtextTypeUtils.toDescriptiveString(fieldType);

        throw new ExternalTypeNotfoundException(serviceName, parentTypeName, fieldName, placeHolderTypeDescription);
      }
    }
  }

  private void validateFieldResolverType(FieldResolverContext fieldResolverContext, UnifiedXtextGraph sourceXtextGraph) {
    final ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();
    final FieldDefinition targetFieldDefinition = getFieldDefinitionByFQN(resolverDirectiveDefinition.getField(), sourceXtextGraph);
    NamedType fieldResolverType = fieldResolverContext.getFieldDefinition().getNamedType();
    NamedType targetFieldType = targetFieldDefinition.getNamedType();

    if (!XtextTypeUtils.isCompatible(fieldResolverType, targetFieldType)) {
      String errorMessage = "The type of field with @resolver is not compatible with target field type.";
      throw new FieldResolverException(errorMessage, fieldResolverContext);
    }
  }

  private FieldResolverContext updateWithTargetFieldData(final FieldResolverContext fieldResolverContext, final UnifiedXtextGraph sourceXtextGraph) {

    final ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();

    final FieldDefinition targetFieldDefinition = getFieldDefinitionByFQN(resolverDirectiveDefinition.getField(), sourceXtextGraph);

    List<InputValueDefinition> targetFieldInputValueDefinitions = Collections.emptyList();
    if (Objects.nonNull(targetFieldDefinition.getArgumentsDefinition())) {
      targetFieldInputValueDefinitions = targetFieldDefinition.getArgumentsDefinition().getInputValueDefinition();
    }

    List<ResolverArgumentDefinition> updatedResolverArgumentDefinitions = updateWithInputType(
            resolverDirectiveDefinition.getArguments(), targetFieldInputValueDefinitions, fieldResolverContext);

    FieldContext targetFieldContext = new FieldContext(getParentTypeName(targetFieldDefinition), targetFieldDefinition.getName());

    //if it contains '.' must find correct targetContext to get namespace since it is nested
    String namespace = (StringUtils.contains(resolverDirectiveDefinition.getField(), FQN_FIELD_SEPARATOR)) ?
            getTargetNamespace(fieldResolverContext, resolverDirectiveDefinition, sourceXtextGraph) :
            sourceXtextGraph.getCodeRegistry().get(targetFieldContext).getNamespace();

    String targetFieldFQN = resolverDirectiveDefinition.getField();
    return fieldResolverContext.transform(builder -> builder
            .targetFieldDefinition(targetFieldDefinition)
            .targetFieldContext(targetFieldContext)
            .targetServiceNamespace(namespace)
            .resolverDirectiveDefinition(new ResolverDirectiveDefinition(targetFieldFQN, updatedResolverArgumentDefinitions))
    );

  }

  private String getTargetNamespace(final FieldResolverContext fieldResolverContext,
                                    final ResolverDirectiveDefinition resolverDirectiveDefinition,
                                    final UnifiedXtextGraph sourceXtextGraph) {
    String namespace = fieldResolverContext.getServiceNamespace();
    String[] resolverPath = resolverDirectiveDefinition.getField().split("\\.");
    AtomicReference<String> parentTypeNameRef = new AtomicReference<>(Operation.QUERY.getName());
    int start = (FQN_KEYWORD_QUERY.equals(resolverPath[0]) || Operation.QUERY.getName().equals(resolverPath[0])) ? 1 : 0;

    //traverse through path to get the correct fieldContext
    for (int i = start; i < resolverPath.length; i++) {
      String fieldName = resolverPath[i];
      FieldContext fieldContext = new FieldContext(parentTypeNameRef.get(), fieldName);
      DataFetcherContext dfContext = sourceXtextGraph.getCodeRegistry().get(fieldContext);
      if(dfContext != null) {
        namespace = dfContext.getNamespace();
      }

      ObjectTypeDefinition currentParent = sourceXtextGraph.getObjectTypeDefinitions().get(parentTypeNameRef.get());
      if(currentParent != null) {
        FieldDefinition currentField = getFieldDefinitionFromObjectTypeDefinition(currentParent, fieldName);
        if(currentField != null) {
          parentTypeNameRef.set(com.intuit.graphql.utils.XtextTypeUtils.typeName(currentField.getNamedType()));
        } else {
          throw FieldResolverGraphQLError.builder()
                  .errorMessage("Target service field not found.")
                  .fieldName(fieldName)
                  .parentTypeName(parentTypeNameRef.get())
                  .resolverDirectiveDefinition(fieldResolverContext.getResolverDirectiveDefinition())
                  .serviceNameSpace(fieldResolverContext.getServiceNamespace())
                  .build();
        }
      }
    }

    return namespace;
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
                                                UnifiedXtextGraph sourceXtextGraph) {

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

  private FieldDefinition getFieldDefinitionByFQN(final String queryFieldFQN, UnifiedXtextGraph unifiedXtextGraph) {

    String queryFieldFQNNoQuery = StringUtils.removeStart(queryFieldFQN ,FQN_KEYWORD_QUERY + FQN_FIELD_SEPARATOR); // remove if exists

    String[] queryFieldFQNTokens = StringUtils.split(queryFieldFQNNoQuery, FQN_FIELD_SEPARATOR);
    if (ArrayUtils.isEmpty(queryFieldFQNTokens)) {
      String errorMessage = String.format("Failed to tokenize queryFieldFQN.  queryFieldFQN=%s", queryFieldFQN);
      throw new IllegalArgumentException(errorMessage);
    }

    FieldDefinition fieldDefinition = null;

    TypeDefinition currentParentType = unifiedXtextGraph.getOperationMap().get(Operation.QUERY);
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
