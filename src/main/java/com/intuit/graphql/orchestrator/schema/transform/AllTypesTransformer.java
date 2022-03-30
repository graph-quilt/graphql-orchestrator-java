package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_EXTENDS_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.isTypeSystemForBaseType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.toDescriptiveString;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.definitionContainsDirective;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getAllTypes;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getTypeSystemDefinition;
import static graphql.schema.FieldCoordinates.coordinates;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.Streams;
import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeExtensionDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeExtensionDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.graphQL.UnionTypeDefinition;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.SchemaTransformationException;
import com.intuit.graphql.orchestrator.utils.DescriptionUtils;
import com.intuit.graphql.orchestrator.utils.FederationUtils;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.schema.FieldCoordinates;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.apache.commons.collections4.CollectionUtils;

public class AllTypesTransformer implements Transformer<XtextGraph, XtextGraph> {



  @Override
  public XtextGraph transform(XtextGraph source) {
    Map<String, TypeDefinition> types = getAllTypes(source.getXtextResourceSet())
        .filter(type -> !source.isOperationType(type))
        .filter(this::isNotEmpty)
        .collect(toMap(TypeDefinition::getName, Function.identity(),
            (t1, t2) -> {
              throw new SchemaTransformationException(
                  String.format("Duplicate TypeDefinition %s in namespace: %s", toDescriptiveString(t1),
                      source.getServiceProvider().getNameSpace()));
            }));
    updateDescWithNamespace(types, source.getServiceProvider().getNameSpace());

    List<FieldCoordinates> fieldCoordinates = collectFieldCoordinates(types);
    fieldCoordinates.addAll(collectOperationsFieldCoordinates(source.getOperationMap()));

    if(source.isFederationService()) {
      Map<String, TypeDefinition> baseEntities = getAllTypes(source.getXtextResourceSet())
              .filter(typeDefinition -> definitionContainsDirective(typeDefinition, FEDERATION_KEY_DIRECTIVE))
              .filter(FederationUtils::isBaseType)
              .collect(toMap(TypeDefinition::getName, Function.identity()));

      Map<String, TypeSystemDefinition>  extensionEntities =  Streams.concat(
          getTypeSystemDefinition(source.getXtextResourceSet())
                  .filter(FederationUtils::isTypeSystemForBaseType)
                  .filter(typeSystemDefinition -> definitionContainsDirective(typeSystemDefinition.getType(), FEDERATION_KEY_DIRECTIVE))
                  .filter(typeSystemDefinition -> definitionContainsDirective(typeSystemDefinition.getType(), FEDERATION_EXTENDS_DIRECTIVE)),
          getTypeSystemDefinition(source.getXtextResourceSet())
                  .filter(FederationUtils::isTypeSystemForExtensionType)
                  .filter(typeSystemDefinition -> typeSystemDefinition.getTypeExtension() instanceof ObjectTypeExtensionDefinition || typeSystemDefinition.getTypeExtension() instanceof InterfaceTypeExtensionDefinition)
                  .filter(typeSystemDefinition -> definitionContainsDirective(typeSystemDefinition.getTypeExtension(), FEDERATION_KEY_DIRECTIVE))
      ).collect(toMap(
        (typeSystemDefinition -> isTypeSystemForBaseType(typeSystemDefinition) ? typeSystemDefinition.getType().getName() : typeSystemDefinition.getTypeExtension().getName()),
        Function.identity())
      );

      HashMap<String, Map<String, TypeSystemDefinition>> extensionEntitiesByNamespace = new HashMap<>();
      extensionEntitiesByNamespace.put(source.getServiceProvider().getNameSpace(), extensionEntities);

      return source.transform(builder -> builder
        .types(types)
        .entitiesByTypeName(baseEntities)
        .fieldCoordinates(fieldCoordinates)
        .entityExtensionsByNamespace(extensionEntitiesByNamespace)
      );
    }

    return source.transform(builder -> builder
        .types(types)
        .fieldCoordinates(fieldCoordinates)
    );
  }

  private Collection<? extends FieldCoordinates> collectOperationsFieldCoordinates(
      Map<Operation, ObjectTypeDefinition> operationMap) {
    Map<String, TypeDefinition> operationTypeMap = operationMap.values().stream()
        .collect(toMap(ObjectTypeDefinition::getName, Function.identity()));
    return collectFieldCoordinates(operationTypeMap);
  }

  private List<FieldCoordinates> collectFieldCoordinates(Map<String, TypeDefinition> types) {
    List<FieldCoordinates> output = new ArrayList<>();
    types.forEach((typeName, typeDefinition) -> getFieldDefinitions(typeDefinition, true)
        .forEach(fieldDefinition -> output.add(coordinates(typeName, fieldDefinition.getName()))));
    return output;
  }

  private boolean isNotEmpty(TypeDefinition typeDefinition) {
    if (typeDefinition instanceof ObjectTypeDefinition) {
      return CollectionUtils.isNotEmpty(((ObjectTypeDefinition) typeDefinition).getFieldDefinition());
    }
    if (typeDefinition instanceof InterfaceTypeDefinition) {
      return CollectionUtils.isNotEmpty(((InterfaceTypeDefinition) typeDefinition).getFieldDefinition());
    }
    if (typeDefinition instanceof UnionTypeDefinition) {
      return Objects.nonNull(((UnionTypeDefinition) typeDefinition).getUnionMemberShip());
    }
    if (typeDefinition instanceof EnumTypeDefinition) {
      return CollectionUtils.isNotEmpty(((EnumTypeDefinition) typeDefinition).getEnumValueDefinition());
    }
    return true;
  }

  private void updateDescWithNamespace(Map<String, TypeDefinition> types, String namespace) {
    types.forEach((k, v) -> {
      v.setDesc(DescriptionUtils.attachNamespace(namespace,v.getDesc()));
    });
  }
}
