package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_EXTENDS_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.isBaseType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.toDescriptiveString;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.definitionContainsDirective;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getAllTypes;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getType;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getTypeSystemDefinition;

import com.google.common.collect.Streams;
import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeExtensionDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeExtensionDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeExtensionDefinition;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.graphQL.UnionTypeDefinition;
import com.intuit.graphql.orchestrator.schema.SchemaTransformationException;
import com.intuit.graphql.orchestrator.utils.DescriptionUtils;
import com.intuit.graphql.orchestrator.utils.FederationConstants;
import com.intuit.graphql.orchestrator.utils.FederationUtils;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

public class AllTypesTransformer implements Transformer<XtextGraph, XtextGraph> {



  @Override
  public XtextGraph transform(XtextGraph source) {
    Map<String, TypeDefinition> types = getAllTypes(source.getXtextResourceSet())
        .filter(type -> !source.isOperationType(type))
        .filter(this::isNotEmpty)
        .collect(Collectors.toMap(TypeDefinition::getName, Function.identity(),
            (t1, t2) -> {
              throw new SchemaTransformationException(
                  String.format("Duplicate TypeDefinition %s in namespace: %s", toDescriptiveString(t1),
                      source.getServiceProvider().getNameSpace()));
            }));
    updateDescWithNamespace(types, source.getServiceProvider().getNameSpace());

    if(source.isFederationService()) {
      Map<String, TypeDefinition> baseEntities = getAllTypes(source.getXtextResourceSet())
              .filter(typeDefinition -> definitionContainsDirective(typeDefinition, FEDERATION_KEY_DIRECTIVE))
              .filter(FederationUtils::isBaseType)
              .collect(Collectors.toMap(TypeDefinition::getName, Function.identity()));

      Map<String, TypeSystemDefinition>  extensionEntities =  Streams.concat(
          getTypeSystemDefinition(source.getXtextResourceSet())
                  .filter(typeSystemDefinition ->  typeSystemDefinition.getType() != null)
                  .filter(typeSystemDefinition -> definitionContainsDirective(typeSystemDefinition.getType(), FEDERATION_KEY_DIRECTIVE))
                  .filter(typeSystemDefinition -> definitionContainsDirective(typeSystemDefinition.getType(), FEDERATION_EXTENDS_DIRECTIVE)),
          getTypeSystemDefinition(source.getXtextResourceSet())
                  .filter(typeSystemDefinition -> typeSystemDefinition.getTypeExtension() != null)
                  .filter(typeSystemDefinition -> typeSystemDefinition.getTypeExtension() instanceof ObjectTypeExtensionDefinition || typeSystemDefinition.getTypeExtension() instanceof InterfaceTypeExtensionDefinition)
                  .filter(typeSystemDefinition -> definitionContainsDirective(typeSystemDefinition.getTypeExtension(), FEDERATION_KEY_DIRECTIVE))
      ).collect(Collectors.toMap(
        (typeSystemDefinition -> (typeSystemDefinition.getType() != null) ? typeSystemDefinition.getType().getName() : typeSystemDefinition.getTypeExtension().getName()),
        Function.identity())
      );

      HashMap<String, Map<String, TypeSystemDefinition>> extensionEntitiesByNamespace = new HashMap<>();
      extensionEntitiesByNamespace.put(source.getServiceProvider().getNameSpace(), extensionEntities);

      return source.transform(builder -> builder
        .types(types)
        .entitiesByTypeName(baseEntities)
        .entityExtensionsByNamespace(extensionEntitiesByNamespace)
      );
    }

    return source.transform(builder -> builder.types(types));
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
