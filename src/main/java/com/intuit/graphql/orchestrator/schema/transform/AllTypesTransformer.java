package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.toDescriptiveString;

import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.UnionTypeDefinition;
import com.intuit.graphql.orchestrator.schema.SchemaTransformationException;
import com.intuit.graphql.orchestrator.utils.DescriptionUtils;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

public class AllTypesTransformer implements Transformer<XtextGraph, XtextGraph> {



  @Override
  public XtextGraph transform(XtextGraph source) {
    Map<String, TypeDefinition> types = XtextUtils
        .getAllTypes(source.getXtextResourceSet())
        .filter(type -> !source.isOperationType(type))
        .filter(this::isNotEmpty)
        .collect(Collectors.toMap(TypeDefinition::getName, Function.identity(),
            (t1, t2) -> {
              throw new SchemaTransformationException(
                  String.format("Duplicate TypeDefinition %s in namespace: %s", toDescriptiveString(t1),
                      source.getServiceProvider().getNameSpace()));
            }));
    updateDescWithNamespace(types, source.getServiceProvider().getNameSpace());
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
