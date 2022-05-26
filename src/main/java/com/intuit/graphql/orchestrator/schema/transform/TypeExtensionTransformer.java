package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.EnumTypeExtensionDefinition;
import com.intuit.graphql.graphQL.InputObjectTypeDefinition;
import com.intuit.graphql.graphQL.InputObjectTypeExtensionDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeExtensionDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeExtensionDefinition;
import com.intuit.graphql.graphQL.UnionTypeDefinition;
import com.intuit.graphql.graphQL.UnionTypeExtensionDefinition;
import com.intuit.graphql.graphQL.util.GraphQLSwitch;
import com.intuit.graphql.orchestrator.utils.XtextUtils;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.Objects;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.XtextResourceSet;

public class TypeExtensionTransformer implements Transformer<XtextGraph, XtextGraph> {


  @Override
  public XtextGraph transform(XtextGraph xtextGraph) {
    TypeDefinitionVisitor typeDefinitionVisitor = new TypeDefinitionVisitor(xtextGraph.getXtextResourceSet());
    XtextUtils.getAllTypes(xtextGraph.getXtextResourceSet()).forEach(typeDefinitionVisitor::doSwitch);
    //TODO: Maybe remove extension Types form resourceSet??
    return xtextGraph;
  }

  class TypeDefinitionVisitor extends GraphQLSwitch<EObject> {

    private final XtextResourceSet xtextResourceSet;

    public TypeDefinitionVisitor(XtextResourceSet xtextResourceSet) {
      this.xtextResourceSet = xtextResourceSet;
    }

    @Override
    public EObject caseObjectTypeDefinition(ObjectTypeDefinition object) {
      XtextUtils.getAllTypeExtensionForName(object.getName(), ObjectTypeExtensionDefinition.class, this.xtextResourceSet)
          .forEach(typeExtension -> {

                object.getFieldDefinition().addAll(typeExtension.getFieldDefinition());

                if (Objects.nonNull(typeExtension.getImplementsInterfaces())) {

                  if (Objects.nonNull(object.getImplementsInterfaces())) {
                    object.getImplementsInterfaces().getNamedType()
                        .addAll(typeExtension.getImplementsInterfaces().getNamedType());
                  } else {
                    object.setImplementsInterfaces(typeExtension.getImplementsInterfaces());
                  }
                }

                object.getDirectives().addAll(typeExtension.getDirectives());
              }
          );

      return object;
    }

    @Override
    public EObject caseInputObjectTypeDefinition(InputObjectTypeDefinition object) {

      XtextUtils.getAllTypeExtensionForName(object.getName(), InputObjectTypeExtensionDefinition.class, this.xtextResourceSet)
          .forEach(typeExtension -> {
                object.getInputValueDefinition().addAll(typeExtension.getInputValueDefinition());
                object.getDirectives().addAll(typeExtension.getDirectives());
              }
          );

      return object;
    }

    @Override
    public EObject caseEnumTypeDefinition(EnumTypeDefinition object) {
      XtextUtils.getAllTypeExtensionForName(object.getName(), EnumTypeExtensionDefinition.class, this.xtextResourceSet)
          .forEach(typeExtension -> {
            object.getEnumValueDefinition().addAll(typeExtension.getEnumValueDefinition());
            object.getDirectives().addAll(typeExtension.getDirectives());
          });

      return object;
    }

    @Override
    public EObject caseInterfaceTypeDefinition(InterfaceTypeDefinition object) {
      XtextUtils.getAllTypeExtensionForName(object.getName(), InterfaceTypeExtensionDefinition.class, this.xtextResourceSet)
          .forEach(typeExtension -> {
            object.getFieldDefinition().addAll(typeExtension.getFieldDefinition());
            object.getDirectives().addAll(typeExtension.getDirectives());
          });
      return object;
    }

    @Override
    public EObject caseUnionTypeDefinition(UnionTypeDefinition object) {

      XtextUtils.getAllTypeExtensionForName(object.getName(), UnionTypeExtensionDefinition.class, this.xtextResourceSet)
          .forEach(typeExtension -> {

            if (Objects.nonNull(typeExtension.getUnionMemberShip()) && Objects.nonNull(typeExtension.getUnionMemberShip().getUnionMembers())) {
              if (Objects.nonNull(object.getUnionMemberShip()) && Objects.nonNull(object.getUnionMemberShip().getUnionMembers())) {
                object.getUnionMemberShip().getUnionMembers().getNamedUnion().addAll(typeExtension.getUnionMemberShip().getUnionMembers().getNamedUnion());
              } else {
                object.setUnionMemberShip(typeExtension.getUnionMemberShip());
              }
            }

          });

      return object;
    }

  }

}