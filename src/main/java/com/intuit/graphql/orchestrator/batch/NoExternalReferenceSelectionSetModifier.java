package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.util.TreeTransformerUtil.changeNode;
import static graphql.util.TreeTransformerUtil.deleteNode;
import static java.util.Objects.requireNonNull;

import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityMetadata;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.SelectionSet;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

public class NoExternalReferenceSelectionSetModifier extends NodeVisitorStub {

  private final GraphQLFieldsContainer rootType;
  private final ServiceMetadata serviceMetadata;

  NoExternalReferenceSelectionSetModifier(GraphQLFieldsContainer rootType, ServiceMetadata serviceMetadata) {
    this.rootType = rootType;
    this.serviceMetadata = serviceMetadata;
  }

  @Override
  public TraversalControl visitField(Field node, TraverserContext<Node> context) {
    if (context.visitedNodes().size() == 0) {
      context.setVar(GraphQLType.class, rootType);
      return TraversalControl.CONTINUE;
    } else {
      GraphQLFieldsContainer parentType = context.getParentContext().getVar(GraphQLType.class);

      String fieldName = node.getName();
      GraphQLFieldDefinition fieldDefinition = getFieldDefinition(fieldName, parentType);
      requireNonNull(fieldDefinition, "Failed to get Field Definition for " + fieldName);

      if (isExternalField(parentType.getName(), fieldDefinition)) {
        return deleteNode(context);
      }

      // if field node has selection set, store it's type to its node context
      if (node.getSelectionSet() != null) {
        context.setVar(GraphQLType.class, fieldDefinition.getType());
      }
      return TraversalControl.CONTINUE;
    }
  }

  private boolean isExternalField(String parentTypename, GraphQLFieldDefinition fieldDefinition) {
    FieldCoordinates fieldCoordinates = coordinates(parentTypename, fieldDefinition.getName());
    // TODO consider the entire condition to be abstracted in
    //  serviceMetadata.isFieldExternal(fieldCoordinates).
    //  This requires a complete set of field coordinates that the service owns
    return hasResolverDirective(fieldDefinition)
        || serviceMetadata.isOwnedByEntityExtension(fieldCoordinates);
  }

  private GraphQLFieldDefinition getFieldDefinition(String name, GraphQLFieldsContainer parentType) {
    if (TypeNameMetaFieldDef.getName().equals(name)) {
      return TypeNameMetaFieldDef;
    }
    return parentType.getFieldDefinition(name);
  }

  @Override
  public TraversalControl visitFragmentDefinition(FragmentDefinition node, TraverserContext<Node> context) {
    // if modifying selection set in a fragment definition, this will be the first code to visit.
    context.setVar(GraphQLType.class, rootType);
    return TraversalControl.CONTINUE;
  }

  @Override
  public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
    GraphQLType parentType = getParentType(context);
    context.setVar(GraphQLType.class, parentType);
    return TraversalControl.CONTINUE;
  }

  @Override
  public TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
    GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) getParentType(context);
    context.setVar(GraphQLType.class, parentType);

    Set<String> selectionsByName = node.getSelections().stream()
        .filter(selection -> selection instanceof Field) // TODO inline / fragments
        .map(selection -> (Field) selection)
        .map(field -> field.getName())
        .collect(Collectors.toSet());

    if (this.serviceMetadata.isEntity(parentType.getName())) {
      FederationMetadata federationMetadata = this.serviceMetadata.getFederationServiceMetadata();
      EntityMetadata entityMetadata = federationMetadata.getEntityMetadataByName(parentType.getName());
      List<String> missingKeys = entityMetadata.getKeyDirectives().stream()
          .flatMap(keyDirectiveMetadata -> keyDirectiveMetadata.getKeyFieldNames().stream())
          .filter(keyFieldname -> !selectionsByName.contains(keyFieldname))
          .collect(Collectors.toList());
      if (CollectionUtils.isNotEmpty(missingKeys)) {
        SelectionSet newNode = node.transform(builder -> {
          for (String missingKey : missingKeys) {
            builder.selection(new Field(missingKey));
          }
        });
        return changeNode(context, newNode);
      }
    }
    return TraversalControl.CONTINUE;
  }

  private GraphQLType getParentType(TraverserContext<Node> context) {
    GraphQLType parentType = context.getParentContext().getVar(GraphQLType.class);
    return GraphQLTypeUtil.unwrapAll(parentType);
  }

}