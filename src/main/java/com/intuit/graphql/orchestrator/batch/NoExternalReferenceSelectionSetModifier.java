package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.util.TreeTransformerUtil.changeNode;
import static graphql.util.TreeTransformerUtil.deleteNode;
import static java.util.Objects.requireNonNull;

import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.utils.SelectionCollector;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

public class NoExternalReferenceSelectionSetModifier extends NodeVisitorStub {

  private final GraphQLFieldsContainer rootType;
  private final ServiceMetadata serviceMetadata;
  private final SelectionCollector selectionCollector;

  NoExternalReferenceSelectionSetModifier(GraphQLFieldsContainer rootType,
      ServiceMetadata serviceMetadata,
      Map<String, FragmentDefinition> fragmentsByName) {
    this.rootType = rootType;
    this.serviceMetadata = serviceMetadata;
    this.selectionCollector = new SelectionCollector(fragmentsByName);
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

  @Override
  public TraversalControl visitOperationDefinition(OperationDefinition node, TraverserContext<Node> context) {
    context.setVar(GraphQLType.class, rootType);
    return TraversalControl.CONTINUE;
  }

  @Override
  public TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
    GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) getParentType(context);
    context.setVar(GraphQLType.class, parentType);
    String parentTypeName = parentType.getName();

    Map<String, Field> selectedFields = this.selectionCollector.collectFields(node);
    Set<Field> fieldsToAdd = new HashSet<>();
    getFieldsWithResolverDirective(parentTypeName, selectedFields)
        .forEach(fieldResolverContext -> {
          Set<String> fldResolverReqdFields = fieldResolverContext.getRequiredFields();
          if (CollectionUtils.isNotEmpty(fldResolverReqdFields)) {
            fieldsToAdd.addAll(
                fldResolverReqdFields.stream()
                .filter(s -> !selectedFields.containsKey(s))
                .map(s -> Field.newField(s).build())
                .collect(Collectors.toSet()));
          }
        });

    if (CollectionUtils.isNotEmpty(fieldsToAdd)) {
      SelectionSet newNode = node.transform(builder -> {
        for (Field field : fieldsToAdd) {
          // DON'T use builder.selections(fieldsToAdd).  it will clear then add selection
          builder.selection(field);
        }
      });
      return changeNode(context, newNode);
    }

    return TraversalControl.CONTINUE;
  }


  private List<FieldResolverContext> getFieldsWithResolverDirective(
      String parentTypename,  Map<String, Field> selectedFields) {
    return selectedFields.values().stream()
        .map(
            field -> {
              FieldCoordinates fieldCoordinates = coordinates(parentTypename, field.getName());
              return serviceMetadata.getFieldResolverContext(fieldCoordinates);
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private GraphQLType getParentType(@SuppressWarnings("rawtypes") TraverserContext<Node> context) {
    GraphQLType parentType = context.getParentContext().getVar(GraphQLType.class);
    return GraphQLTypeUtil.unwrapAll(parentType);
  }

}