package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.util.TreeTransformerUtil.changeNode;
import static graphql.util.TreeTransformerUtil.deleteNode;
import static java.util.Objects.requireNonNull;

import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

public class NoExternalReferenceSelectionSetModifier extends NodeVisitorStub {

  private GraphQLFieldsContainer rootType;

  NoExternalReferenceSelectionSetModifier(GraphQLFieldsContainer rootType) {
    this.rootType = rootType;
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
      if (hasResolverDirective(fieldDefinition)) {
        return deleteNode(context);
      }

      // if field node has selection set, store it's type to its node context
      if (node.getSelectionSet() != null) {
        context.setVar(GraphQLType.class, fieldDefinition.getType());
      }
      return TraversalControl.CONTINUE;
    }
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

  // Tests needed for fieldReference not selected in Parent Selection
  // Queries that has field with resolver selected in a Selection as (Field),
  // in FragmentSpread and Inline Fragment
  @Override
  public TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
    GraphQLType parentType = getParentType(context);
    context.setVar(GraphQLType.class, parentType);

    List<GraphQLFieldDefinition> fieldsWithResolver = node.getSelections().stream()
        .filter(selection -> selection instanceof Field)
        // A selection can be a Field, FragmentSpread and InlineFragment
        // For InlineFragment/FragmentSpread just continue since it will have another selection.
        .map(selection -> (Field) selection)
        .filter(field -> {
          GraphQLFieldDefinition fieldDefinition = getFieldDefinition(field.getName(), (GraphQLFieldsContainer) parentType); // not always Field
          return hasResolverDirective(fieldDefinition);
        })
        .map(field -> getFieldDefinition(field.getName(), (GraphQLFieldsContainer) parentType))
        .collect(Collectors.toList());

    List<GraphQLArgument> resolverDirectiveArguments = fieldsWithResolver.stream()
        .flatMap(fieldDefinition -> fieldDefinition.getDirectives().stream())
        .flatMap(graphQLDirective -> graphQLDirective.getArguments().stream())
        .collect(Collectors.toList());


    // TODO get the required fields from resolverDirectiveArguments and check if it exists in selectionSet
    if (CollectionUtils.isNotEmpty(fieldsWithResolver)) {
      SelectionSet newSelectSet = node.transform(builder -> {
        Field newField = Field.newField("petId").build(); // just for test
        builder.selection(newField);
        //builder.selection(newField);
      });
      return changeNode(context, newSelectSet);
    } else {
      return TraversalControl.CONTINUE;
    }
  }

  private GraphQLType getParentType(TraverserContext<Node> context) {
    GraphQLType parentType = context.getParentContext().getVar(GraphQLType.class);
    return GraphQLTypeUtil.unwrapAll(parentType);
  }

}