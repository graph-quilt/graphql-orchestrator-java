package com.intuit.graphql.orchestrator.batch;

import com.intuit.graphql.orchestrator.federation.RequiredFieldsCollector;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.utils.SelectionCollector;
import graphql.GraphQLContext;
import graphql.language.Argument;
import graphql.language.BooleanValue;
import graphql.language.Directive;
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
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

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
import static com.intuit.graphql.orchestrator.utils.RenameDirectiveUtil.convertGraphqlFieldWithOriginalName;
import static com.intuit.graphql.orchestrator.utils.RenameDirectiveUtil.getRenameKey;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.util.TreeTransformerUtil.changeNode;
import static graphql.util.TreeTransformerUtil.deleteNode;
import static java.util.Objects.requireNonNull;

/**
 * This class modifies for query for a downstream provider.
 *
 * One function of this class is to remove external fields.  This occurs if a type
 * is extended by other services and add fields to it.
 *
 * Another function is this class adds required fields to the query if fields
 * are required by other sibling fields which are external or remote.
 *
 * @deprecated  As of release 5.0.15.  To be replaced by {@link AuthDownstreamQueryModifier}
 *
 */
 @Deprecated
public class DownstreamQueryModifier extends NodeVisitorStub {

  private final GraphQLType rootType;
  private final ServiceMetadata serviceMetadata;
  private final SelectionCollector selectionCollector;
  private final GraphQLSchema graphQLSchema;

  private final GraphQLContext graphQLContext;

  public DownstreamQueryModifier(
          GraphQLType rootType,
          ServiceMetadata serviceMetadata,
          Map<String, FragmentDefinition> fragmentsByName,
          GraphQLSchema graphQLSchema,
          GraphQLContext context) {
    Objects.requireNonNull(rootType);
    Objects.requireNonNull(serviceMetadata);
    Objects.requireNonNull(fragmentsByName);
    this.rootType = rootType;
    this.serviceMetadata = serviceMetadata;
    this.selectionCollector = new SelectionCollector(fragmentsByName);
    this.graphQLSchema = graphQLSchema;
    this.graphQLContext = context;
  }

  @Override
  public TraversalControl visitField(Field node, TraverserContext<Node> context) {
    if (context.visitedNodes().isEmpty()) {
      context.setVar(GraphQLType.class, rootType);
      if(!serviceMetadata.getRenamedMetadata().getOriginalFieldNamesByRenamedName().isEmpty()) {
        String renamedKey =  getRenameKey(null, node.getName(), true);
        String originalName = serviceMetadata.getRenamedMetadata().getOriginalFieldNamesByRenamedName().get(renamedKey);
        if(originalName != null) {
          return changeNode(context, convertGraphqlFieldWithOriginalName(node, originalName));
        }
      }

      if(!node.getDirectives(DEFER_DIRECTIVE_NAME).isEmpty()) {
        Argument deferArg = node.getDirectives(DEFER_DIRECTIVE_NAME).get(0).getArgument(DEFER_IF_ARG);
        if(graphQLContext.getOrDefault(USE_DEFER, false) && (deferArg == null || ((BooleanValue)deferArg.getValue()).isValue())) {
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

      return TraversalControl.CONTINUE;
    } else {
      GraphQLFieldsContainer parentType = context.getParentContext().getVar(GraphQLType.class);

      String fieldName = node.getName();
      GraphQLFieldDefinition fieldDefinition = getFieldDefinition(fieldName, parentType);
      requireNonNull(fieldDefinition, "Failed to get Field Definition for " + fieldName);

      // TODO consider the entire condition to be abstracted in
      //  serviceMetadata.isFieldExternal(fieldCoordinates).
      //  This requires a complete set of field coordinates that the service owns
      if (serviceMetadata.shouldModifyDownStreamQuery() && (hasResolverDirective(fieldDefinition)
          || isExternalField(parentType.getName(), fieldName))) {
        return deleteNode(context);
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

      if(!node.getDirectives(DEFER_DIRECTIVE_NAME).isEmpty()) {
        Argument deferArg = node.getDirectives(DEFER_DIRECTIVE_NAME).get(0).getArgument(DEFER_IF_ARG);
        if(graphQLContext.getOrDefault(USE_DEFER, false) && (deferArg == null || ((BooleanValue)deferArg.getValue()).isValue())) {
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

  @Override
  public TraversalControl visitFragmentDefinition(
      FragmentDefinition node, TraverserContext<Node> context) {
    // if modifying selection set in a fragment definition, this will be the first code to visit.
    context.setVar(GraphQLType.class, rootType);
    return TraversalControl.CONTINUE;
  }

  @Override
  public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
    String typeName = node.getTypeCondition().getName();
    context.setVar(GraphQLType.class, this.graphQLSchema.getType(typeName));
    return TraversalControl.CONTINUE;
  }

  @Override
  public TraversalControl visitOperationDefinition(OperationDefinition node, TraverserContext<Node> context) {
    context.setVar(GraphQLType.class, rootType);
    return TraversalControl.CONTINUE;
  }

  @Override
  public TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
    if (getParentType(context) instanceof GraphQLUnionType) {
      return TraversalControl.CONTINUE;
    }

    GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) getParentType(context);
    context.setVar(GraphQLType.class, parentType);
    String parentTypeName = parentType.getName();

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
}
