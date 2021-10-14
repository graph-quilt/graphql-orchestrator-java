package com.intuit.graphql.orchestrator.authorization;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.util.TreeTransformerUtil.deleteNode;
import static java.util.Objects.requireNonNull;

import com.intuit.graphql.orchestrator.common.ArgumentValueResolver;
import graphql.GraphQLContext;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.SelectionSet;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public class SelectionSetRedactor extends NodeVisitorStub {

  private GraphQLFieldsContainer rootFieldType;
  private GraphQLFieldsContainer rootFieldParentType;
  private Object authData;
  private FieldAuthorization fieldAuthorization;
  private GraphQLContext graphQLContext;
  private ArgumentValueResolver argumentValueResolver;
  private Map<String, Object> variables;
  private GraphQLSchema graphQLSchema;

  private final List<Field> fieldsContainers = new ArrayList<>();
  private final List<SelectionSetPath> processedSelectionSetPaths = new ArrayList<>();
  private final List<DeclinedField> declinedFields = new ArrayList<>();

  public SelectionSetRedactor(
      GraphQLFieldsContainer rootFieldType,
      GraphQLFieldsContainer rootFieldParentType,
      Object authData,
      FieldAuthorization fieldAuthorization,
      GraphQLContext graphQLContext,
      ArgumentValueResolver argumentValueResolver,
      Map<String, Object> variables,
      GraphQLSchema graphQLSchema) {
    this.rootFieldType = rootFieldType;
    this.rootFieldParentType = rootFieldParentType;
    this.authData = authData;
    this.fieldAuthorization = fieldAuthorization;
    this.graphQLContext = graphQLContext;
    this.argumentValueResolver = argumentValueResolver;
    this.variables = variables;
    this.graphQLSchema = graphQLSchema;
  }

  @Override
  public TraversalControl visitField(Field node, TraverserContext<Node> context) {

    String fieldName = node.getName();

    if (context.visitedNodes().size() == 0) {
      context.setVar(GraphQLType.class, rootFieldType);

      FieldCoordinates fieldCoordinates = FieldCoordinates.coordinates(rootFieldParentType.getName(), fieldName);
      GraphQLFieldDefinition fieldDefinition = getFieldDefinition(fieldName, this.rootFieldParentType);
      if (fieldDefinition.getType() instanceof GraphQLFieldsContainer) {
        this.fieldsContainers.add(node);
      }
      Map<String, Object> argumentValues = argumentValueResolver.resolve(graphQLSchema, fieldDefinition, node, variables);
      FieldAuthorizationResult fieldAuthorizationResult = fieldAuthorization.authorize(fieldCoordinates, node, authData, argumentValues);
      if (!fieldAuthorizationResult.isAllowed()) {
        return deleteNode(context);
      }

      return TraversalControl.CONTINUE;
    } else {
      GraphQLFieldsContainer parentType = context.getParentContext().getVar(GraphQLType.class);
      GraphQLFieldDefinition fieldDefinition = getFieldDefinition(fieldName, parentType);
      if (fieldDefinition.getType() instanceof GraphQLFieldsContainer) {
        this.fieldsContainers.add(node);
      }
      requireNonNull(fieldDefinition, "Failed to get Field Definition for " + fieldName);
      if (hasResolverDirective(fieldDefinition)) {
        return deleteNode(context);
      }

      FieldCoordinates fieldCoordinates = FieldCoordinates.coordinates(parentType.getName(), fieldName);
      Map<String, Object> argumentValues = argumentValueResolver.resolve(graphQLSchema, fieldDefinition, node, variables);
      FieldAuthorizationResult fieldAuthorizationResult = fieldAuthorization.authorize(fieldCoordinates, node, authData, argumentValues);
      if (!fieldAuthorizationResult.isAllowed()) {
        SelectionSetPath parentSelectionSetPath = context.getParentContext().getVar(SelectionSetPath.class);
        parentSelectionSetPath.decreaseRemainingSelection();
        this.declinedFields.add(new DeclinedField(node, parentSelectionSetPath.getPathList()));
        return deleteNode(context);
      }

      // if field node has selection set, store it's type to its node context
      if (node.getSelectionSet() != null) {
        context.setVar(GraphQLType.class, fieldDefinition.getType());
      }
      return TraversalControl.CONTINUE;
    }
  }

  private GraphQLFieldDefinition getFieldDefinition(
      String name, GraphQLFieldsContainer parentType) {
    if (TypeNameMetaFieldDef.getName().equals(name)) {
      return TypeNameMetaFieldDef;
    }
    return parentType.getFieldDefinition(name);
  }

  @Override
  public TraversalControl visitFragmentDefinition(
      FragmentDefinition node, TraverserContext<Node> context) {
    // if modifying selection set in a fragment definition, this will be the first code to visit.
    context.setVar(GraphQLType.class, rootFieldType);
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
    GraphQLType parentType = getParentType(context);
    if (context.getParentNode() instanceof Field) {
      Field parentField = (Field) context.getParentNode();
      if (context.getVar(SelectionSetPath.class) == null) {
        SelectionSetPath selectionSetPath = new SelectionSetPath(parentField.getSelectionSet());
        selectionSetPath.add(parentField.getName());
        context.setVar(SelectionSetPath.class, selectionSetPath);
        processedSelectionSetPaths.add(selectionSetPath);
      } else {
        SelectionSetPath parentSelectionSetPath =
            context.getParentContext().getParentContext().getVar(SelectionSetPath.class);
        SelectionSetPath newSelectionSetPath =
            SelectionSetPath.createRelativePath(
                parentSelectionSetPath, parentField.getName(), parentField.getSelectionSet());
        context.setVar(SelectionSetPath.class, newSelectionSetPath);
        processedSelectionSetPaths.add(newSelectionSetPath);
      }
    }
    context.setVar(GraphQLType.class, parentType);
    return TraversalControl.CONTINUE;
  }

//  @Override
//  public TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
//    GraphQLType parentType = getParentType(context);
//    if (context.getParentNode() instanceof Field) {
//      Field parentField = (Field) context.getParentNode();
//      if (context.getParentContext().getParentContext().isRootContext()) {
//        SelectionSetPath selectionSetPath = new SelectionSetPath(parentField.getSelectionSet());
//        selectionSetPath.add(parentField.getName());
//        context.setVar(SelectionSetPath.class, selectionSetPath);
//        processedSelectionSetPaths.add(selectionSetPath);
//      } else {
//        SelectionSetPath parentSelectionSetPath =
//            context.getParentContext().getParentContext().getVar(SelectionSetPath.class);
//        SelectionSetPath newSelectionSetPath =
//            SelectionSetPath.createRelativePath(
//                parentSelectionSetPath, parentField.getName(), parentField.getSelectionSet());
//        context.setVar(SelectionSetPath.class, newSelectionSetPath);
//        processedSelectionSetPaths.add(newSelectionSetPath);
//      }
//    }
//    context.setVar(GraphQLType.class, parentType);
//    return TraversalControl.CONTINUE;
//  }

  private GraphQLType getParentType(TraverserContext<Node> context) {
    GraphQLType parentType = context.getParentContext().getVar(GraphQLType.class);
    return GraphQLTypeUtil.unwrapAll(parentType);
  }

  public Collection<SelectionSetPath> getProcessedSelectionSets() {
    return this.processedSelectionSetPaths;
  }

  public Boolean isResultAnEmptySelection() {
    return this.processedSelectionSetPaths.stream()
        .allMatch(selectionSetPath -> selectionSetPath.getRemainingSelectionsCount() == 0);
  }

  public List<DeclinedField> getDeclineFields() {
    return this.declinedFields;
  }
}
