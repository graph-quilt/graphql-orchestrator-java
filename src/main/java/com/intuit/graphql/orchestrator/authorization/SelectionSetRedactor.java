package com.intuit.graphql.orchestrator.authorization;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.common.FieldPosition;
import graphql.GraphQLContext;
import graphql.language.*;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.util.TreeTransformerUtil.deleteNode;
import static java.util.Objects.requireNonNull;

public class SelectionSetRedactor<ClaimT> extends NodeVisitorStub {

    private final GraphQLFieldsContainer rootFieldType;
    private final GraphQLFieldsContainer rootFieldParentType;
    private final Pair<String, Object> claimData;
    private final AuthorizationContext<ClaimT> authorizationContext;
    private final GraphQLContext graphQLContext;

    private final List<SelectionSetPath> processedSelectionSetPaths = new ArrayList<>();

    SelectionSetRedactor(GraphQLFieldsContainer rootFieldType, GraphQLFieldsContainer rootFieldParentType,
                                            Pair<String, Object> claimData,
                                            AuthorizationContext<ClaimT> authorizationContext,
                                            GraphQLContext graphQLContext) {
        this.rootFieldType = rootFieldType;
        this.rootFieldParentType = rootFieldParentType;
        this.claimData = claimData;
        this.authorizationContext = authorizationContext;
        this.graphQLContext = graphQLContext;
    }

    @Override
    public TraversalControl visitField(Field node, TraverserContext<Node> context) {

        if (context.visitedNodes().size() == 0) {
            context.setVar(GraphQLType.class, rootFieldType);

            FieldPosition fieldPosition = new FieldPosition(rootFieldParentType.getName(), node.getName());
            if (requiresFieldAuthorization(fieldPosition) && !fieldAccessIsAllowed(fieldPosition, node)) {
                return deleteNode(context);
            }

            return TraversalControl.CONTINUE;
        } else {
            GraphQLFieldsContainer parentType = context.getParentContext().getVar(GraphQLType.class);

            String fieldName = node.getName();
            GraphQLFieldDefinition fieldDefinition = getFieldDefinition(fieldName, parentType);
            requireNonNull(fieldDefinition, "Failed to get Field Definition for " + fieldName);
            if (hasResolverDirective(fieldDefinition)) {
                return deleteNode(context);
            }

            FieldPosition fieldPosition = new FieldPosition(parentType.getName(), node.getName());
            if (requiresFieldAuthorization(fieldPosition) && !fieldAccessIsAllowed(fieldPosition, node)) {
                SelectionSetPath parentSelectionSetPath = context.getParentContext().getVar(SelectionSetPath.class);
                parentSelectionSetPath.decreaseRemainingSelection();
                return deleteNode(context);
            }

            // if field node has selection set, store it's type to its node context
            if (node.getSelectionSet() != null) {
                context.setVar(GraphQLType.class, fieldDefinition.getType());
            }
            return TraversalControl.CONTINUE;
        }
    }

    private boolean requiresFieldAuthorization(FieldPosition fieldPosition) {
        return authorizationContext.getFieldAuthorization().requiresAccessControl(fieldPosition);
    }

    private boolean fieldAccessIsAllowed(FieldPosition fieldPosition, Field node) {
        Map<String, Object> authData = ImmutableMap.of(
                claimData.getLeft(), claimData.getRight(),
                "fieldArguments", node.getArguments() // TODO converte to Map
        );

        FieldAuthorizationRequest fieldAuthorizationRequest = FieldAuthorizationRequest.builder()
                .fieldPosition(fieldPosition)
                .clientId(this.authorizationContext.getClientId())
                .graphQLContext(graphQLContext)
                .authData(authData)
                .build();

        return authorizationContext.getFieldAuthorization().isAccessAllowed(fieldAuthorizationRequest);
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
            if (parentType == this.rootFieldType) {
                SelectionSetPath selectionSetPath = new SelectionSetPath(parentField.getSelectionSet());
                selectionSetPath.add(parentField.getName());
                context.setVar(SelectionSetPath.class, selectionSetPath);
                processedSelectionSetPaths.add(selectionSetPath);
            } else {
                SelectionSetPath parentSelectionSetPath = context.getParentContext().getParentContext().getVar(SelectionSetPath.class);
                SelectionSetPath newSelectionSetPath = SelectionSetPath.createRelativePath(parentSelectionSetPath, parentField.getName(), parentField.getSelectionSet());
                processedSelectionSetPaths.add(newSelectionSetPath);
            }
        }
        context.setVar(GraphQLType.class, parentType);
        return TraversalControl.CONTINUE;
    }

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
}
