package com.intuit.graphql.orchestrator.batch;

import com.intuit.graphql.orchestrator.authorization.FieldAuthorization;
import com.intuit.graphql.orchestrator.authorization.FieldAuthorizationEnvironment;
import com.intuit.graphql.orchestrator.authorization.FieldAuthorizationResult;
import com.intuit.graphql.orchestrator.authorization.SelectionSetMetadata;
import com.intuit.graphql.orchestrator.common.ArgumentValueResolver;
import com.intuit.graphql.orchestrator.federation.RequiredFieldsCollector;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.utils.SelectionCollector;
import graphql.GraphQLContext;
import graphql.GraphQLException;
import graphql.GraphqlErrorException;
import graphql.language.Argument;
import graphql.language.BooleanValue;
import graphql.language.Directive;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import lombok.Builder;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_IF_ARG;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.USE_DEFER;
import static com.intuit.graphql.orchestrator.utils.QueryPathUtils.getNodesAsPathList;
import static com.intuit.graphql.orchestrator.utils.QueryPathUtils.pathListToFQN;
import static com.intuit.graphql.orchestrator.utils.RenameDirectiveUtil.convertGraphqlFieldWithOriginalName;
import static com.intuit.graphql.orchestrator.utils.RenameDirectiveUtil.getRenameKey;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.util.TreeTransformerUtil.changeNode;
import static graphql.util.TreeTransformerUtil.deleteNode;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * This class modifies for query for a downstream provider.
 *
 * One function of this class is to remove external fields.  This occurs if a type
 * is extended by other services and add fields to it.
 *
 * Another function is this class adds required fields to the query if fields
 * are required by other sibling fields which are external or remote.
 */
@Builder
public class AuthDownstreamQueryModifier extends NodeVisitorStub {

  private static final ArgumentValueResolver ARGUMENT_VALUE_RESOLVER = new ArgumentValueResolver(); // thread-safe
  private final List<SelectionSetMetadata> processedSelectionSetMetadata = new ArrayList<>();
  private final List<GraphqlErrorException> declinedFieldsErrors = new ArrayList<>();
  private final List<String> fragmentSpreadsRemoved = new ArrayList<>();

  private boolean hasEmptySelectionSet;

  /**
   * For top level fields, should be Query, Mutation.  in case of fragment, should
   * be the type of parent field
   */
  @NonNull private final GraphQLType rootParentType;
  @NonNull private FieldAuthorization fieldAuthorization;
  @NonNull private GraphQLContext graphQLContext;
  @NonNull private Map<String, Object> queryVariables;
  @NonNull private final GraphQLSchema graphQLSchema;
  @NonNull private final SelectionCollector selectionCollector;
  @NonNull private final ServiceMetadata serviceMetadata; // service metadata for the root
  private Object authData;

  @Override
  public TraversalControl visitField(Field node, TraverserContext<Node> context) {
    if (context.visitedNodes().isEmpty()) {
      GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) rootParentType;
      GraphQLFieldDefinition fieldDefinition = getFieldDefinition(node.getName(), parentType);
      requireNonNull(fieldDefinition, "Failed to get Field Definition for " + node.getName());

      context.setVar(GraphQLType.class, fieldDefinition.getType());
      FieldAuthorizationResult fieldAuthorizationResult = authorize(node, fieldDefinition, parentType, context);
      if (!fieldAuthorizationResult.isAllowed()) {
        decreaseParentSelectionSetCount(context.getParentContext());
        this.declinedFieldsErrors.add(fieldAuthorizationResult.getGraphqlErrorException());
        return deleteNode(context);
      }

      List<Directive> directives = node.getDirectives();
      if(containsDeferDirective(directives)) {
        return pruneDeferInfo(node, context, directives);
      }

      if(!serviceMetadata.getRenamedMetadata().getOriginalFieldNamesByRenamedName().isEmpty()) {
        String renamedKey =  getRenameKey(null, node.getName(), true);
        String originalName = serviceMetadata.getRenamedMetadata().getOriginalFieldNamesByRenamedName().get(renamedKey);
        if(originalName != null) {
          return changeNode(context, convertGraphqlFieldWithOriginalName(node, originalName));
        }
      }

      return TraversalControl.CONTINUE;
    } else {
      GraphQLFieldsContainer parentType = context.getParentContext().getVar(GraphQLType.class);
      GraphQLFieldDefinition fieldDefinition = getFieldDefinition(node.getName(), parentType);
      requireNonNull(fieldDefinition, "Failed to get Field Definition for " + node.getName());

      if (serviceMetadata.shouldModifyDownStreamQuery() && (hasResolverDirective(fieldDefinition)
          || isExternalField(parentType.getName(), node.getName()))) {
        decreaseParentSelectionSetCount(context.getParentContext());
        return deleteNode(context);
      } else {
        FieldAuthorizationResult fieldAuthorizationResult = authorize(node, fieldDefinition, parentType, context);
        if (!fieldAuthorizationResult.isAllowed()) {
          decreaseParentSelectionSetCount(context.getParentContext());
          this.declinedFieldsErrors.add(fieldAuthorizationResult.getGraphqlErrorException());
          return deleteNode(context);
        }
      }

      if(!node.getDirectives(DEFER_DIRECTIVE_NAME).isEmpty()) {
        Argument deferArg = node.getDirectives(DEFER_DIRECTIVE_NAME).get(0).getArgument(DEFER_IF_ARG);
        if(graphQLContext.getOrDefault(USE_DEFER, false) && (deferArg == null || ((BooleanValue)deferArg.getValue()).isValue())) {
          decreaseParentSelectionSetCount(context.getParentContext());
          return deleteNode(context);
        } else {
          //remove directive from query since directive is not built in and will fail downstream if added
          List<Directive> directives = node.getDirectives()
                  .stream()
                  .filter(directive -> !DEFER_DIRECTIVE_NAME.equals(directive.getName()))
                  .collect(Collectors.toList());

          return changeNode(context, node.transform(builder -> builder.directives(directives)));
        }
      }

      String renameKey = getRenameKey(parentType.getName(), node.getName(), false);
      String originalName = serviceMetadata.getRenamedMetadata().getOriginalFieldNamesByRenamedName().get(renameKey);

      // if field node has selection set or needs to be renamed, store it's type to its node context
      if (node.getSelectionSet() != null || originalName != null) {
        context.setVar(GraphQLType.class, fieldDefinition.getType());

        if(originalName != null) {
          return changeNode(context, convertGraphqlFieldWithOriginalName(node, originalName));
        }
      }
      return TraversalControl.CONTINUE;
    }
  }

  private boolean isExternalField(String parentTypename, String fieldName) {
    FieldCoordinates fieldCoordinates = coordinates(parentTypename, fieldName);
    return serviceMetadata.isOwnedByEntityExtension(fieldCoordinates);
  }

  private GraphQLFieldDefinition getFieldDefinition(
      String name, GraphQLFieldsContainer parentType) {
    if (TypeNameMetaFieldDef.getName().equals(name)) {
      return TypeNameMetaFieldDef;
    }
    return parentType.getFieldDefinition(name);
  }

  private void decreaseParentSelectionSetCount(TraverserContext<Node> parentContext) {
    if (nonNull(parentContext) && nonNull(parentContext.getVar(SelectionSetMetadata.class))) {
      SelectionSetMetadata selectionSetMetadata = parentContext.getVar(SelectionSetMetadata.class);
      selectionSetMetadata.decreaseRemainingSelection();
      if (!hasEmptySelectionSet && selectionSetMetadata.getRemainingSelectionsCount() == 0) {
        hasEmptySelectionSet = true;
      }
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
        .path(getNodesAsPathList(context))
        .build();
    return fieldAuthorization.authorize(fieldAuthorizationEnvironment);
  }

  @Override
  public TraversalControl visitFragmentDefinition(
      FragmentDefinition node, TraverserContext<Node> context) {
    // if modifying selection set in a fragment definition, this will be the first code to visit.
    String typeName = node.getTypeCondition().getName();
    context.setVar(GraphQLType.class, graphQLSchema.getType(typeName));
    return TraversalControl.CONTINUE;
  }

  @Override
  public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
    String typeName = node.getTypeCondition().getName();
    context.setVar(GraphQLType.class, this.graphQLSchema.getType(typeName));

    List<Directive> directives = node.getDirectives();
    if(containsDeferDirective(directives)) {
      return pruneDeferInfo(node, context, directives);
    }

    return TraversalControl.CONTINUE;
  }

  @Override
  public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
    context.setVar(GraphQLType.class, this.graphQLSchema.getType(node.getName()));

    List<Directive> directives = node.getDirectives();
    if(containsDeferDirective(directives)) {
      return pruneDeferInfo(node, context, directives);
    }

    return TraversalControl.CONTINUE;
  }

  @Override
  public TraversalControl visitOperationDefinition(OperationDefinition node, TraverserContext<Node> context) {
    context.setVar(GraphQLType.class, this.rootParentType);
    return TraversalControl.CONTINUE;
  }

  @Override
  public TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
    GraphQLType parentType = getParentType(context);
    if (parentType instanceof GraphQLUnionType) {
      return TraversalControl.CONTINUE;
    }

    GraphQLFieldsContainer parentFieldContainerType = (GraphQLFieldsContainer) parentType;
    context.setVar(GraphQLType.class, parentFieldContainerType);
    String parentTypeName = parentFieldContainerType.getName();

    Map<String, Field> selectedFields =  this.selectionCollector.collectFields(node);

    RequiredFieldsCollector fedRequiredFieldsCollector = RequiredFieldsCollector
        .builder()
        .excludeFields(selectedFields)
        .parentTypeName(parentTypeName)
        .serviceMetadata(this.serviceMetadata)
        .fieldResolverContexts(getFieldsWithResolverDirective(parentTypeName, selectedFields))
        .fieldsWithRequiresDirective(getFieldsWithRequiresDirective(parentTypeName, selectedFields))
        .build();

    Set<Field> fieldsToAdd = fedRequiredFieldsCollector.get();

    if (!context.isVisited()) {
      int selectionCount = node.getSelections().size() + CollectionUtils.size(fieldsToAdd);
      List<Object> pathList = getNodesAsPathList(context);
      String selectionSetPath = pathListToFQN(pathList);
      SelectionSetMetadata selectionSetMetadata = new SelectionSetMetadata(selectionCount, selectionSetPath);
      context.setVar(SelectionSetMetadata.class, selectionSetMetadata);
      processedSelectionSetMetadata.add(selectionSetMetadata);
    }

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

  private Set<Field> getFieldsWithRequiresDirective(String parentTypename,  Map<String, Field> selectedFields) {
    if (MapUtils.isEmpty(selectedFields)) {
      return Collections.emptySet();
    }

    FederationMetadata federationMetadata = this.serviceMetadata.getFederationServiceMetadata();
    return selectedFields.values().stream()
        .filter(field -> isExternalField(parentTypename, field.getName()))
        .filter(field -> {
          FieldCoordinates fieldCoordinates = coordinates(parentTypename, field.getName());
          return federationMetadata.hasRequiresFieldSet(fieldCoordinates);
        })
        .collect(Collectors.toSet());
  }

  private GraphQLType getParentType(TraverserContext<Node> context) {
    GraphQLType parentType = context.getParentContext().getVar(GraphQLType.class);
    return GraphQLTypeUtil.unwrapAll(parentType);
  }

  private boolean containsDeferDirective(List<Directive> directives) {
    return directives != null && directives.stream()
            .anyMatch(directive -> DEFER_DIRECTIVE_NAME.equals(directive.getName()));
  }

  private TraversalControl pruneDeferInfo(Node node, TraverserContext<Node> context, List<Directive> nodeDirectives) {
    Directive deferDirective = nodeDirectives
            .stream()
            .filter(directive -> DEFER_DIRECTIVE_NAME.equals(directive.getName()))
            .findFirst()
            .get();

    Argument deferArg = deferDirective.getArgument(DEFER_IF_ARG);
    if(graphQLContext.getOrDefault(USE_DEFER, false) && (deferArg == null || ((BooleanValue)deferArg.getValue()).isValue())) {
      decreaseParentSelectionSetCount(context.getParentContext());
      if(node instanceof FragmentSpread) {
        this.fragmentSpreadsRemoved.add(((FragmentSpread)node).getName());
      }

      return deleteNode(context);
    } else {
      final List<Directive> directives = nodeDirectives
              .stream()
              .filter(directive -> !DEFER_DIRECTIVE_NAME.equals(directive.getName()))
              .collect(Collectors.toList());
      //remove directive from query since directive is not built in and will fail downstream if added
      if(node instanceof Field) {
        return changeNode(context, ((Field)node).transform(builder -> builder.directives(directives)));
      } else if(node instanceof InlineFragment) {
        return changeNode(context, ((InlineFragment)node).transform(builder -> builder.directives(directives)));
      } else if(node instanceof FragmentSpread) {
        return changeNode(context, ((FragmentSpread)node).transform(builder -> builder.directives(directives)));
      } else {
        throw new GraphQLException("Not Supported Defer Location.");
      }
    }
  }

  public List<GraphqlErrorException> getDeclineFieldErrors() {
    return declinedFieldsErrors;
  }

  public List<String> getFragmentSpreadsRemoved() {
    return this.fragmentSpreadsRemoved;
  }

  public List<SelectionSetMetadata> getEmptySelectionSets() {
    return this.processedSelectionSetMetadata.stream()
        .filter(selectionSetMetadata -> selectionSetMetadata.getRemainingSelectionsCount() == 0)
        .collect(Collectors.toList());
  }

  public boolean redactedQueryHasEmptySelectionSet() {
    return hasEmptySelectionSet;
  }
}
