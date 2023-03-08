package com.intuit.graphql.orchestrator.visitors.queryVisitors;

import com.intuit.graphql.orchestrator.deferDirective.DeferOptions;
import com.intuit.graphql.orchestrator.utils.GraphQLUtil;
import graphql.ExecutionInput;
import graphql.GraphQLException;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorFragmentSpreadEnvironment;
import graphql.analysis.QueryVisitorInlineFragmentEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.language.AstPrinter;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.SelectionSetContainer;
import graphql.util.TraverserContext;
import graphql.util.TreeTransformerUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.deferDirective.DeferUtil.containsEnabledDeferDirective;
import static com.intuit.graphql.orchestrator.deferDirective.DeferUtil.hasNonDeferredSelection;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.AST_TRANSFORMER;
import static com.intuit.graphql.orchestrator.utils.IntrospectionUtil.__typenameField;
import static com.intuit.graphql.orchestrator.utils.NodeUtils.removeDirectiveFromNode;

@Builder
public class DeferQueryCreatorVisitor extends QueryVisitorStub {

    @NonNull private final Document rootNode;

    @NonNull private final OperationDefinition operationDefinition;
    @NonNull private final Map<String, FragmentDefinition> fragmentDefinitionMap;
    @NonNull private final PruneChildDeferSelectionsModifier childModifier;
    @NonNull private ExecutionInput originalEI;

    @NonNull private DeferOptions deferOptions;

    @Getter
    private final List<ExecutionInput> generatedEIs =  new ArrayList<>();



    /*
     * Functions:
     *    Updates the field if it contains defer directive.
     *    If it is a valid deferred node, generate new EI
     */
    @Override
    public void visitField(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
        updateDeferredInfoForNode(queryVisitorFieldEnvironment.getField(), queryVisitorFieldEnvironment.getTraverserContext());
    }

    /*
     * Functions:
     *    Updates the FragmentSpread if it contains defer directive.
     *    If it is a valid deferred node, generate new EI
     */
    @Override
    public void visitInlineFragment(QueryVisitorInlineFragmentEnvironment queryVisitorInlineFragmentEnvironment) {
        updateDeferredInfoForNode(queryVisitorInlineFragmentEnvironment.getInlineFragment(), queryVisitorInlineFragmentEnvironment.getTraverserContext());
    }

    @Override
    public void visitFragmentSpread(QueryVisitorFragmentSpreadEnvironment queryVisitorFragmentSpreadEnvironment) {
        updateDeferredInfoForNode(queryVisitorFragmentSpreadEnvironment.getFragmentSpread(), queryVisitorFragmentSpreadEnvironment.getTraverserContext());
    }

    private void updateDeferredInfoForNode(Node node, TraverserContext<Node> context) {
        if(containsEnabledDeferDirective(node)) {
            Node prunedNode = removeDirectiveFromNode(node, DEFER_DIRECTIVE_NAME);

            if(hasNonDeferredSelection(node)) {
                generatedEIs.add(generateDeferredEI(prunedNode, context));
            }

            //update node so children nodes has the correct definition
            TreeTransformerUtil.changeNode(context, prunedNode);
        }
    }

    /*
     * Generates an Execution Input given the node and the context
     * */
    private ExecutionInput generateDeferredEI(Node currentNode, TraverserContext<Node> context) {
        //prune defer information from children
        Node prunedNode = AST_TRANSFORMER.transform(currentNode, childModifier);
        List<Node> parentNodes = getParentDefinitions(context);

        Set<String> neededFragmentSpreads = new HashSet<>();
        if(currentNode instanceof FragmentSpread) {
            neededFragmentSpreads.add(((FragmentSpread) currentNode).getName());
        }

        //builds new OperationDefinition consisting with only pruned nodes
        for (Node parentNode : parentNodes) {
            prunedNode = constructNewPrunedNode(parentNode, prunedNode);

            if(parentNode instanceof FragmentSpread) {
                neededFragmentSpreads.add(((FragmentSpread) parentNode).getName());
            }
        }

        //Gets all the definitions for the fragment spreads
        List<FragmentDefinition> fragmentSpreadDefs = neededFragmentSpreads
                .stream()
                .map(fragmentDefinitionMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        SelectionSet ss = SelectionSet.newSelectionSet().selection((Selection) prunedNode).build();
        OperationDefinition newOperation = this.operationDefinition.transform(builder -> builder.selectionSet(
                ss
        ));

        List<Definition> deferredDefinitions = new ArrayList<>();
        deferredDefinitions.add(newOperation);
        deferredDefinitions.addAll(fragmentSpreadDefs);

        Document deferredDocument = this.rootNode.transform(builder -> builder.definitions(deferredDefinitions));

        String query = AstPrinter.printAst(deferredDocument);

        return originalEI.transform(builder -> builder.query(query));
    }

    private List<Node> getParentDefinitions(TraverserContext<Node> currentNode) {
        return currentNode.getParentNodes()
                .stream()
                .filter(SelectionSetContainer.class::isInstance)
                .collect(Collectors.toList());
    }

    /*
     * Generates new node
     * Transforms the parentNode with a new selection set consisting of the pruned child and typename fields
     * */
    private Node constructNewPrunedNode(Node parentNode, Node prunedChild) {
        List<Selection> selections = new ArrayList<>();
        selections.add((Selection) prunedChild);

        if(parentNode instanceof SelectionSetContainer && !(parentNode instanceof OperationDefinition)) {
            selections.add(__typenameField);
        }

        SelectionSet prunedSelectionSet = SelectionSet.newSelectionSet()
                .selections(selections)
                .build();

        if(parentNode instanceof Field) {
            return ((Field) parentNode).transform(builder -> builder.selectionSet(prunedSelectionSet));
        } else if (parentNode instanceof FragmentDefinition) {
            //add fragment spread names here in case of nested fragment spreads
            return ((FragmentDefinition) parentNode).transform(builder -> builder.selectionSet(prunedSelectionSet));
        } else if (parentNode instanceof InlineFragment) {
            return ((InlineFragment) parentNode).transform(builder -> builder.selectionSet(prunedSelectionSet));
        } else if (parentNode instanceof OperationDefinition) {
            return ((OperationDefinition) parentNode).transform(builder -> builder.selectionSet(prunedSelectionSet));
        }

        throw new GraphQLException("Could not construct query due to invalid directive location");
    }



    public static DeferQueryCreatorVisitor.DeferQueryCreatorVisitorBuilder builder() {
        return new DeferQueryCreatorVisitor.DeferQueryCreatorVisitorBuilder();
    }

    public static class DeferQueryCreatorVisitorBuilder {
        ExecutionInput originalEI;
        OperationDefinition operationDefinition;
        Document rootNode;
        Map<String, FragmentDefinition> fragmentDefinitionMap;

        DeferOptions deferOptions;

        PruneChildDeferSelectionsModifier childModifier;

        public DeferQueryCreatorVisitor.DeferQueryCreatorVisitorBuilder originalEI (ExecutionInput originalEI) {
            this.originalEI = originalEI;
            return rootNode(originalEI);
        }

        public DeferQueryCreatorVisitor.DeferQueryCreatorVisitorBuilder operationDefinition (OperationDefinition operationDefinition) {
            this.operationDefinition = operationDefinition;
            return this;
        }

        public DeferQueryCreatorVisitor.DeferQueryCreatorVisitorBuilder deferOptions(DeferOptions options) {
            this.deferOptions = options;

            return childModifier(options);
        }

        private DeferQueryCreatorVisitor.DeferQueryCreatorVisitorBuilder fragmentDefinitionMap(Document rootNode) {
            this.fragmentDefinitionMap = rootNode.getDefinitionsOfType(FragmentDefinition.class)
                    .stream()
                    .collect(Collectors.toMap(FragmentDefinition::getName , Function.identity()));
            return this;
        }
        private DeferQueryCreatorVisitor.DeferQueryCreatorVisitorBuilder rootNode(ExecutionInput ei) {
            this.rootNode = GraphQLUtil.parser.parseDocument(ei.getQuery());
            return fragmentDefinitionMap(rootNode);
        }
        private DeferQueryCreatorVisitor.DeferQueryCreatorVisitorBuilder childModifier(DeferOptions options) {
            this.childModifier = PruneChildDeferSelectionsModifier.builder()
                    .deferOptions(options)
                    .build();

            return this;
        }
    }


}
