package com.intuit.graphql.orchestrator.authorization;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;
import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.getFieldDefinition;
import static com.intuit.graphql.orchestrator.utils.QueryPathUtils.getParentNodesAsPathList;
import static com.intuit.graphql.orchestrator.utils.QueryPathUtils.pathListToFQN;
import static graphql.util.TreeTransformerUtil.deleteNode;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.intuit.graphql.orchestrator.common.ArgumentValueResolver;
import graphql.GraphQLContext;
import graphql.GraphqlErrorException;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class DownstreamQueryRedactorVisitor extends NodeVisitorStub {

  private static final ArgumentValueResolver ARGUMENT_VALUE_RESOLVER = new ArgumentValueResolver(); // thread-safe
  private final List<SelectionSetMetadata> processedSelectionSetMetadata = new ArrayList<>();
  private final List<GraphqlErrorException> declinedFieldsErrors = new ArrayList<>();

  @NonNull private GraphQLFieldsContainer rootFieldParentType;
  @NonNull private FieldAuthorization fieldAuthorization;
  @NonNull private GraphQLContext graphQLContext;
  @NonNull private Map<String, Object> queryVariables;
  @NonNull private GraphQLSchema graphQLSchema;
  private Object authData;

  @Override
  public TraversalControl visitField(Field currentField, TraverserContext<Node> context) {
    GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) this.getParentType(context);
    requireNonNull(parentType, "Failed to get parent type for " + currentField.getName());
    GraphQLFieldDefinition currentFieldFieldDefinition = getFieldDefinition(currentField.getName(), parentType);
    requireNonNull(currentFieldFieldDefinition, "Failed to get GraphQLFieldDefinition for " + currentField.getName());

    context.setVar(GraphQLType.class, currentFieldFieldDefinition.getType());

    if (hasResolverDirective(currentFieldFieldDefinition)) {
      decreaseParentSelectionSetCount(context.getParentContext());
      return deleteNode(context);
    } else {
      FieldAuthorizationResult fieldAuthorizationResult = authorize(currentField,
          currentFieldFieldDefinition, parentType, context);
      if (!fieldAuthorizationResult.isAllowed()) {
        decreaseParentSelectionSetCount(context.getParentContext());
        this.declinedFieldsErrors.add(fieldAuthorizationResult.getGraphqlErrorException());
        return deleteNode(context);
      }
    }
    return TraversalControl.CONTINUE;
  }

  @SuppressWarnings("rawtypes") // Node defined raw in graphql.language.NodeVisitor
  private GraphQLType getParentType(TraverserContext<Node> context) {
    GraphQLType parentType;
    if (context.visitedNodes().size() == 0) {
      parentType = this.rootFieldParentType;
    } else {
      parentType = requireNonNull(context.getParentContext().getVar(GraphQLType.class));
    }
    return GraphQLTypeUtil.unwrapAll(parentType);
  }

  @SuppressWarnings("rawtypes") // Node defined raw in graphql.language.NodeVisitor
  private void decreaseParentSelectionSetCount(TraverserContext<Node> parentContext) {
    if (nonNull(parentContext) && nonNull(parentContext.getVar(SelectionSetMetadata.class))) {
      parentContext.getVar(SelectionSetMetadata.class).decreaseRemainingSelection();
    }
  }

  private FieldAuthorizationResult authorize(Field node, GraphQLFieldDefinition fieldDefinition,
      GraphQLFieldsContainer parentType, TraverserContext<Node> context) {
    String fieldName = node.getName();
    FieldCoordinates fieldCoordinates = FieldCoordinates.coordinates(parentType.getName(), fieldName);
    Map<String, Object> argumentValues = ARGUMENT_VALUE_RESOLVER.resolve(graphQLSchema, fieldDefinition, node,
        queryVariables);
    FieldAuthorizationEnvironment fieldAuthorizationEnvironment = FieldAuthorizationEnvironment
        .builder()
        .field(node)
        .fieldCoordinates(fieldCoordinates)
        .authData(authData)
        .argumentValues(argumentValues)
        .path(getParentNodesAsPathList(context))
        .build();
    return fieldAuthorization.authorize(fieldAuthorizationEnvironment);
  }

  @Override
  public TraversalControl visitFragmentDefinition(FragmentDefinition node, TraverserContext<Node> context) {
    // if modifying selection set in a fragment definition, this will be the first code to visit.
    context.setVar(GraphQLType.class, graphQLSchema.getType(node.getTypeCondition().getName()));
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
    if (!context.isVisited()) {
      int selectionCount = node.getSelections().size();
      List<Object> pathList = getParentNodesAsPathList(context);
      String selectionSetPath = pathListToFQN(pathList);
      SelectionSetMetadata selectionSetMetadata = new SelectionSetMetadata(selectionCount, selectionSetPath);
      context.setVar(SelectionSetMetadata.class, selectionSetMetadata);
      processedSelectionSetMetadata.add(selectionSetMetadata);
    }

    GraphQLType parentType = getParentType(context);
    context.setVar(GraphQLType.class, parentType);
    return TraversalControl.CONTINUE;
  }

  public List<GraphqlErrorException> getDeclineFieldErrors() {
    return declinedFieldsErrors;
  }

  public List<SelectionSetMetadata> getEmptySelectionSets() {
    return this.processedSelectionSetMetadata.stream()
        .filter(selectionSetMetadata -> selectionSetMetadata.getRemainingSelectionsCount() == 0)
        .collect(Collectors.toList());
  }

}
