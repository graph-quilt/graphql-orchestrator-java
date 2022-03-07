package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.util.TreeTransformerUtil.changeNode;
import static graphql.util.TreeTransformerUtil.deleteNode;
import static java.util.Objects.requireNonNull;

import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;

public class NoExternalReferenceSelectionSetModifier extends NodeVisitorStub {

  private final GraphQLFieldsContainer rootType;
  private final ServiceMetadata serviceMetadata;
  private final SelectionCollector selectionCollector;

  NoExternalReferenceSelectionSetModifier(GraphQLFieldsContainer rootType, ServiceMetadata serviceMetadata, Map<String, FragmentDefinition> fragmentsByName) {
    Objects.nonNull(rootType);
    Objects.nonNull(serviceMetadata);
    Objects.nonNull(fragmentsByName);
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
      if (hasResolverDirective(fieldDefinition) || isExternalField(parentType.getName(), fieldName)) {
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
    String parentTypeName = parentType.getName();

    Set<Field> selectedFields = this.selectionCollector.collectFields(node);

    if (this.serviceMetadata.isEntity(parentTypeName)) {
      FederationMetadata federationMetadata = this.serviceMetadata.getFederationServiceMetadata();
      EntityMetadata entityMetadata = federationMetadata.getEntityMetadataByName(parentTypeName);
      Set<Field> keysFieldSet = getEntityKeys(entityMetadata.getKeyDirectives());
      Set<Field> requiresFieldSet = getRequiresFieldSet(getEntityExternalFields(selectedFields, parentTypeName), parentTypeName);
      List<Field> fieldsToAdd = Stream.concat(keysFieldSet.stream(), requiresFieldSet.stream())
          .filter(field -> !selectedFields.contains(field))
          .collect(Collectors.toList());

      if (CollectionUtils.isNotEmpty(fieldsToAdd)) {
        SelectionSet newNode = node.transform(builder -> builder.selections(fieldsToAdd));
        return changeNode(context, newNode);
      }
    }
    return TraversalControl.CONTINUE;
  }

  private Set<Field> getRequiresFieldSet(List<Field> externalFields, String parentTypename) {
    FederationMetadata federationMetadata = this.serviceMetadata.getFederationServiceMetadata();
    return externalFields.stream()
        .map(field -> FieldCoordinates.coordinates(parentTypename, field.getName()))
        .filter(federationMetadata::hasRequiresFieldSet)
        .map(federationMetadata::getRequireFields)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private List<Field> getEntityExternalFields(Set<Field> selections, String parentTypeName) {
    if (CollectionUtils.isEmpty(selections)) {
      return Collections.emptyList();
    }

    return selections.stream()
        .filter(field -> isExternalField(parentTypeName, field.getName()))
        .collect(Collectors.toList());
  }

  private Set<Field> getEntityKeys(List<KeyDirectiveMetadata> keyDirectives) {
    return keyDirectives.stream()
        .map(KeyDirectiveMetadata::getFieldSet)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private GraphQLType getParentType(TraverserContext<Node> context) {
    GraphQLType parentType = context.getParentContext().getVar(GraphQLType.class);
    return GraphQLTypeUtil.unwrapAll(parentType);
  }

}