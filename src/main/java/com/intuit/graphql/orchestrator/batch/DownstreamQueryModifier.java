package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.util.TreeTransformerUtil.changeNode;
import static graphql.util.TreeTransformerUtil.deleteNode;
import static java.util.Objects.requireNonNull;

import com.intuit.graphql.orchestrator.federation.RequiredFieldsCollector;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.utils.FieldEquator;
import com.intuit.graphql.orchestrator.utils.IntrospectionUtil;
import com.intuit.graphql.orchestrator.utils.SelectionCollector;
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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;

/**
 * This class modifies for query for a downstream provider.
 *
 * One function of this class is to remove external fields.  This occurs if a type
 * is extended by other services and add fields to it.
 *
 * Another function is this class adds required fields to the query if fields
 * are required by other sibling fields which are external or remote.
 */
public class DownstreamQueryModifier extends NodeVisitorStub {

  private final GraphQLFieldsContainer rootType;
  private final ServiceMetadata serviceMetadata;
  private final SelectionCollector selectionCollector;

  DownstreamQueryModifier(
      GraphQLFieldsContainer rootType,
      ServiceMetadata serviceMetadata,
      Map<String, FragmentDefinition> fragmentsByName) {
    Objects.requireNonNull(rootType);
    Objects.requireNonNull(serviceMetadata);
    Objects.requireNonNull(fragmentsByName);
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

      // TODO consider the entire condition to be abstracted in
      //  serviceMetadata.isFieldExternal(fieldCoordinates).
      //  This requires a complete set of field coordinates that the service owns
      if (hasResolverDirective(fieldDefinition)
          || isExternalField(parentType.getName(), fieldName)) {
        return deleteNode(context);
      }

      // if field node has selection set, store it's type to its node context
      if (node.getSelectionSet() != null) {
        context.setVar(GraphQLType.class, fieldDefinition.getType());
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
    GraphQLType parentType = getParentType(context);
    context.setVar(GraphQLType.class, parentType);
    return TraversalControl.CONTINUE;
  }

  private static final FieldEquator fieldEquator = new FieldEquator();

  @Override
  public TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
    GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) getParentType(context);
    context.setVar(GraphQLType.class, parentType);
    String parentTypeName = parentType.getName();

    Set<Field> selectedFields = this.selectionCollector.collectFields(node);

    RequiredFieldsCollector fedRequiredFieldsCollector = RequiredFieldsCollector
        .builder()
        .parentTypeName(parentTypeName)
        .serviceMetadata(this.serviceMetadata)
        .fieldsWithResolver(getFieldsWithResolverDirective(selectedFields, parentType))
        .fieldsWithRequiresDirective(getFieldsWithRequiresDirective(selectedFields, parentTypeName))
        .build();

    Set<Field> fieldsToAdd = fedRequiredFieldsCollector.get().stream()
        .filter(field -> !IterableUtils.contains(selectedFields, field, fieldEquator))
        .collect(Collectors.toSet());

    if (CollectionUtils.isNotEmpty(fieldsToAdd)) {
      SelectionSet newNode = node.transform(builder -> {
        for (Field field : fieldsToAdd) {
          builder.selection(field);
        }
      });
      return changeNode(context, newNode);
    }

    return TraversalControl.CONTINUE;
  }

  private Set<GraphQLFieldDefinition> getFieldsWithResolverDirective(Set<Field> selections, GraphQLFieldsContainer parentType) {
    if (CollectionUtils.isEmpty(selections)) {
      return Collections.emptySet();
    }

    return selections.stream()
        .filter(field -> !IntrospectionUtil.INTROSPECTION_FIELDS.contains(field.getName()))
        .map(field -> parentType.getFieldDefinition(field.getName()))
        .filter(FieldResolverDirectiveUtil::hasResolverDirective)
        .collect(Collectors.toSet());
  }

  private Set<Field> getFieldsWithRequiresDirective(Set<Field> selections, String parentTypeName) {
    if (CollectionUtils.isEmpty(selections)) {
      return Collections.emptySet();
    }

    FederationMetadata federationMetadata = this.serviceMetadata.getFederationServiceMetadata();
    return selections.stream()
        .filter(field -> isExternalField(parentTypeName, field.getName()))
        .filter(field -> {
          FieldCoordinates fieldCoordinates = coordinates(parentTypeName, field.getName());
          return federationMetadata.hasRequiresFieldSet(fieldCoordinates);
        })
        .collect(Collectors.toSet());
  }

  private GraphQLType getParentType(TraverserContext<Node> context) {
    GraphQLType parentType = context.getParentContext().getVar(GraphQLType.class);
    return GraphQLTypeUtil.unwrapAll(parentType);
  }
}
