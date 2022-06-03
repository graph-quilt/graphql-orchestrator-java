package com.intuit.graphql.orchestrator.schema.fold;

import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.EnumValueDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InputObjectTypeDefinition;
import com.intuit.graphql.graphQL.InputValueDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException;
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.XtextTypeConflictResolver;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext.DataFetcherType;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.utils.XtextTypeUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.schema.fold.FieldMergeValidations.checkMergeEligibility;
import static com.intuit.graphql.orchestrator.utils.DescriptionUtils.mergeDescriptions;
import static com.intuit.graphql.orchestrator.xtext.DataFetcherContext.STATIC_DATAFETCHER_CONTEXT;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createObjectType;
import static com.intuit.graphql.utils.XtextTypeUtils.unwrapAll;

public class XtextGraphFolder implements Foldable<XtextGraph> {

  private Map<FieldContext, DataFetcherContext> accCodeRegistry;
  private Map<String, TypeDefinition> nestedTypes;

  @Override
  public XtextGraph fold(XtextGraph initVal, Collection<XtextGraph> list) {
    XtextGraph accumulator = initVal;
    this.accCodeRegistry = initVal.getCodeRegistry();

    for (XtextGraph current : list) {
      accumulator = merge(accumulator, current);
    }
    return accumulator;
  }

  /**
   * Merge runtime graph.
   *
   * @param accumulator the current graph
   * @param current the new comer graph
   * @return the runtime graph
   */
  private XtextGraph merge(XtextGraph accumulator, XtextGraph current) {

    this.nestedTypes = new HashMap<>(); //To be re-initialized for every merge
    Map<Operation, ObjectTypeDefinition> newOperationMap = new EnumMap<>(Operation.class);
    for (Operation op : Operation.values()) {
      newOperationMap.put(op,
          merge(accumulator.getOperationType(op), current.getOperationType(op), current.getServiceProvider()));
    }

    if (current.getServiceProvider().isFederationProvider()) {
      current.getValueTypesByName().values()
          .forEach(incomingSharedType -> {
            TypeDefinition preexistingTypeDefinition = accumulator.getType(incomingSharedType.getName());
            if (incomingSharedType instanceof EnumTypeDefinition) {
              mergeSharedValueType((EnumTypeDefinition) preexistingTypeDefinition,
                  (EnumTypeDefinition) incomingSharedType, current.getServiceProvider());
            } else if (incomingSharedType instanceof ObjectTypeDefinition) {
              mergeSharedValueType((ObjectTypeDefinition) preexistingTypeDefinition,
                  (ObjectTypeDefinition) incomingSharedType, current.getServiceProvider());
            } else if (incomingSharedType instanceof InterfaceTypeDefinition) {
              mergeSharedValueType((InterfaceTypeDefinition) preexistingTypeDefinition,
                  (InterfaceTypeDefinition) incomingSharedType, current.getServiceProvider());
            }
          });
    }

    resolveTypeConflicts(accumulator.getTypes(), current.getTypes(), current);

    // transform the current graph with the new operation and code registry builder
    return accumulator.transform(builder -> {
      builder.operationMap(newOperationMap);
      builder.codeRegistry(this.accCodeRegistry);
      builder.types(current.getTypes());
      builder.typeMetadatas(current.getTypeMetadatas());
      builder.types(nestedTypes);
      builder.directives(current.getDirectives());
      builder.fieldResolverContexts(current.getFieldResolverContexts());
      builder.valueTypesByName(current.getValueTypesByName());
      builder.entityExtensionMetadatas(current.getEntityExtensionMetadatas());
      builder.entitiesByTypeName(current.getEntitiesByTypeName());
      builder.entityExtensionsByNamespace(current.getEntityExtensionsByNamespace());
      builder.federationMetadataByNamespace(current.getFederationMetadataByNamespace());
    });
  }

  private void resolveTypeConflicts(final Map<String, TypeDefinition> existing,
      final Map<String, TypeDefinition> current, final XtextGraph currentGraph) throws TypeConflictException {

    final Set<String> operationTypeNames = currentGraph.getOperationMap().values().stream()
        .map(TypeDefinition::getName)
        .collect(Collectors.toSet());

    for (String typeName : current.keySet()) {
      //ignore type resolution for Query, Mutation, or Subscription types.
      if (operationTypeNames.contains(typeName)) {
        continue;
      }

      TypeDefinition existingType = existing.get(typeName);
      if (Objects.nonNull(existingType) && !nestedTypes.containsKey(existingType.getName())) {
        TypeDefinition conflictingType = current.get(typeName);
        XtextTypeConflictResolver.INSTANCE
            .resolve(conflictingType, existingType, currentGraph.getServiceProvider().isFederationProvider());
      }
    }
  }

  /**
   * Merge the two graphql objects.
   *
   * @return merged object
   */
  private ObjectTypeDefinition merge(ObjectTypeDefinition current, ObjectTypeDefinition newComer,
      ServiceProvider newComerServiceProvider) {
    //nothing to merge
    if (newComer == null) {
      return current;
    }
    //transform the current to add the new types
    String parentType = current.getName();

    newComer.getFieldDefinition().forEach(newField -> {

      Optional<FieldDefinition> currentField = current.getFieldDefinition().stream()
          .filter(ec -> newField.getName().equals(ec.getName()))
          .findFirst();

      if (currentField.isPresent()) {
        checkMergeEligibility(parentType, currentField.get(), newField);

        // XtextNestedTypeConflictResolver.INSTANCE.resolve(newField.getNamedType(), currentField.get().getNamedType());

        // nested merge begins
        current.getFieldDefinition().remove(currentField.get());
        FieldDefinition nestedField = merge(parentType, currentField.get(), newField, newComerServiceProvider);
        current.getFieldDefinition().add(nestedField);

        collectNestedTypes(nestedField);
      } else {
        addNewFieldToObject(current, newField, newComerServiceProvider);
      }
    });

    current.setDesc(mergeDescriptions(current.getDesc(), newComer.getDesc()));

    return current;
  }

  /**
   * Merge the two graphql objects.
   *
   * @return merged object
   */
  private TypeDefinition mergeSharedValueType(EnumTypeDefinition current, EnumTypeDefinition newComer,
      ServiceProvider newComerServiceProvider) {
    //nothing to merge
    if (current == null || !newComerServiceProvider.isFederationProvider()) {
      return current;
    }
    //transform the current to add the new types
    newComer.getEnumValueDefinition().forEach(enumValue -> {
      Optional<EnumValueDefinition> currentEnum = current.getEnumValueDefinition().stream()
          .filter(ec -> enumValue.getEnumValue().equals(ec.getEnumValue()))
          .findFirst();

      if (!currentEnum.isPresent()) {
        addNewFieldToObject(current, enumValue, newComerServiceProvider);
      } else {
        //To be used for inaccessible check later on
      }
    });

    current.setDesc(mergeDescriptions(current.getDesc(), newComer.getDesc()));

    return current;
  }

  private TypeDefinition mergeSharedValueType(ObjectTypeDefinition current, ObjectTypeDefinition newComer,
      ServiceProvider newComerServiceProvider) {
    if (current == null || !newComerServiceProvider.isFederationProvider()) {
      return current;
    }

    newComer.getFieldDefinition().forEach(newField -> {
      Optional<FieldDefinition> currentField = current.getFieldDefinition().stream()
          .filter(fieldName -> newField.getName().equals(fieldName.getName()))
          .findFirst();

      if (!currentField.isPresent()) {
        addNewFieldToObject(current, newField, newComerServiceProvider);
      } else {
        //To be used for inaccessible check later on
      }
    });

    current.setDesc(mergeDescriptions(current.getDesc(), newComer.getDesc()));

    return current;
  }

  private TypeDefinition mergeSharedValueType(InterfaceTypeDefinition current, InterfaceTypeDefinition newComer,
      ServiceProvider newComerServiceProvider) {
    if (current == null || !newComerServiceProvider.isFederationProvider()) {
      return current;
    }

    newComer.getFieldDefinition().forEach(newField -> {
      Optional<FieldDefinition> currentField = current.getFieldDefinition().stream()
          .filter(fieldName -> newField.getName().equals(fieldName.getName()))
          .findFirst();

      if (!currentField.isPresent()) {
        addNewFieldToObject(current, newField, newComerServiceProvider);
      } else {
        //To be used for inaccessible check later on
      }
    });

    current.setDesc(mergeDescriptions(current.getDesc(), newComer.getDesc()));

    return current;
  }

  /**
   * Store the type of a nested field, and all types of its input arguments.
   *
   * @param nestedField a nested field
   */
  private void collectNestedTypes(FieldDefinition nestedField) {
    TypeDefinition type = XtextTypeUtils.getObjectType(nestedField.getNamedType());
    nestedTypes.put(type.getName(), type);

    if (Objects.nonNull(nestedField.getArgumentsDefinition())) {
      //collect argument types
      collectNestedInputTypes(nestedField.getArgumentsDefinition().getInputValueDefinition());
    }
  }

  /**
   * Recursively traverse input arguments of a field (which is nested) and store all the input types as nested.
   *
   * @param inputs the input arguments of a nested field
   */
  private void collectNestedInputTypes(EList<InputValueDefinition> inputs) {
    inputs.stream().forEach(inputValueDefinition -> {
      NamedType namedType = unwrapAll(inputValueDefinition.getNamedType());
      if (com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isObjectType(namedType)) {
        InputObjectTypeDefinition inputObjectTypeDefinition = (InputObjectTypeDefinition) XtextTypeUtils
            .getObjectType(namedType);
        if (!nestedTypes.containsKey(inputObjectTypeDefinition.getName())) {
          nestedTypes.put(inputObjectTypeDefinition.getName(), inputObjectTypeDefinition);
          collectNestedInputTypes(inputObjectTypeDefinition.getInputValueDefinition());
        }
      }
    });
  }

  private void addNewFieldToObject(ObjectTypeDefinition objectTypeDefinition, FieldDefinition fieldDefinition,
      ServiceProvider serviceProvider) {
    addFieldContextToRegistry(objectTypeDefinition.getName(), fieldDefinition.getName(), serviceProvider);
    objectTypeDefinition.getFieldDefinition().add(EcoreUtil.copy(fieldDefinition));
  }


  private void addNewFieldToObject(EnumTypeDefinition enumTypeDefinition, EnumValueDefinition valueDefinition,
      ServiceProvider serviceProvider) {
    addFieldContextToRegistry(enumTypeDefinition.getName(), valueDefinition.getEnumValue(), serviceProvider);
    enumTypeDefinition.getEnumValueDefinition().add(EcoreUtil.copy(valueDefinition));
  }

  private void addNewFieldToObject(InterfaceTypeDefinition interfaceTypeDefinition, FieldDefinition fieldDefinition,
      ServiceProvider serviceProvider) {
    addFieldContextToRegistry(interfaceTypeDefinition.getName(), fieldDefinition.getName(), serviceProvider);
    interfaceTypeDefinition.getFieldDefinition().add(EcoreUtil.copy(fieldDefinition));
  }


  private void addFieldContextToRegistry(String parentName, String definitionName, ServiceProvider serviceProvider) {
    final FieldContext fieldContext = new FieldContext(parentName, definitionName);

    accCodeRegistry.put(fieldContext, DataFetcherContext.newBuilder().namespace(serviceProvider.getNameSpace())
        .serviceType(serviceProvider.getSeviceType()).build());
  }

  /**
   * Merge the two field definitions
   *
   * @return merged field
   */
  private FieldDefinition merge(String parentType, FieldDefinition current,
      FieldDefinition newComer, ServiceProvider newComerServiceProvider) {

    // Do only if both fields are objects. might support other types in the future.
    final ObjectTypeDefinition currentType = (ObjectTypeDefinition) ((ObjectType) current.getNamedType())
        .getType();
    final ObjectTypeDefinition newComerType = (ObjectTypeDefinition) ((ObjectType) newComer.getNamedType())
        .getType();

    // Get datafetcher of current field
    DataFetcherContext dfContext = accCodeRegistry.get(new FieldContext(parentType, current.getName()));
    if (dfContext.getDataFetcherType() != DataFetcherType.STATIC) {
      accCodeRegistry.put(new FieldContext(parentType, current.getName()), STATIC_DATAFETCHER_CONTEXT);
      copyParentDataFetcher(currentType, dfContext);
    }

    //recurse for next level merging
    FieldDefinition copyCurrent = EcoreUtil.copy(current);

    ObjectType objectType = createObjectType();
    objectType.setType(merge(currentType, newComerType, newComerServiceProvider));
    copyCurrent.setNamedType(objectType);
    return copyCurrent;
  }


  private void copyParentDataFetcher(ObjectTypeDefinition objectTypeDefinition, DataFetcherContext dataFetcherContext) {
    objectTypeDefinition.getFieldDefinition().forEach(fieldDefinition ->
        accCodeRegistry.put(new FieldContext(objectTypeDefinition.getName(), fieldDefinition.getName()),
            dataFetcherContext)
    );
  }

}
