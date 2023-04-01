package com.intuit.graphql.orchestrator.visitors.queryVisitors;

import com.intuit.graphql.orchestrator.deferDirective.DeferOptions;
import graphql.ExecutionInput;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorFragmentSpreadEnvironment;
import graphql.analysis.QueryVisitorInlineFragmentEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.language.AstPrinter;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.util.TraverserContext;
import graphql.util.TreeTransformerUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intuit.graphql.orchestrator.deferDirective.DeferUtil.containsEnabledDeferDirective;
import static com.intuit.graphql.orchestrator.deferDirective.DeferUtil.hasNonDeferredSelection;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.AST_TRANSFORMER;
import static com.intuit.graphql.orchestrator.utils.IntrospectionUtil.__typenameField;
import static com.intuit.graphql.orchestrator.utils.NodeUtils.getAllMapValuesWithMatchingKeys;
import static com.intuit.graphql.orchestrator.utils.NodeUtils.isLeaf;
import static com.intuit.graphql.orchestrator.utils.NodeUtils.removeDirectiveFromNode;
import static com.intuit.graphql.orchestrator.utils.NodeUtils.transformNodeWithSelections;
import static com.intuit.graphql.orchestrator.utils.TraverserContextUtils.getParentDefinitions;

@Builder
public class DeferQueryExtractor extends QueryVisitorStub {

    @NonNull private final Document rootNode;
    @NonNull private final OperationDefinition operationDefinition;
    @Builder.Default
    @NonNull
    private Map<String, FragmentDefinition> fragmentDefinitionMap = new HashMap<>();
    @NonNull private final PruneChildDeferSelectionsModifier childModifier;
    @NonNull private ExecutionInput originalEI;

    @NonNull private DeferOptions deferOptions;

    @Getter
    private final List<ExecutionInput> extractedEIs =  new ArrayList<>();

    /**
     * Functions:
     *    Updates the field if it contains defer directive.
     *    If it is a valid deferred node, generate new EI and add to list to be extracted
     * @param queryVisitorFieldEnvironment field that is being visited
     */
    @Override
    public void visitField(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
        updateDeferredInfoForNode(queryVisitorFieldEnvironment.getField(), queryVisitorFieldEnvironment.getTraverserContext());
    }

    /**
     * Functions:
     *    Updates the inline fragment if it contains defer directive.
     *    If it is a valid deferred node, generate new EI and add to list to be extracted
     * @param queryVisitorInlineFragmentEnvironment field that is being visited
     */
    @Override
    public void visitInlineFragment(QueryVisitorInlineFragmentEnvironment queryVisitorInlineFragmentEnvironment) {
        updateDeferredInfoForNode(queryVisitorInlineFragmentEnvironment.getInlineFragment(), queryVisitorInlineFragmentEnvironment.getTraverserContext());
    }

    /**
     * Functions:
     *    Updates the fragment spread if it contains defer directive.
     *    If it is a valid deferred node, generate new EI and add to list to be extracted
     * @param queryVisitorFragmentSpreadEnvironment field that is being visited
     */
    @Override
    public void visitFragmentSpread(QueryVisitorFragmentSpreadEnvironment queryVisitorFragmentSpreadEnvironment) {
        updateDeferredInfoForNode(queryVisitorFragmentSpreadEnvironment.getFragmentSpread(), queryVisitorFragmentSpreadEnvironment.getTraverserContext());
    }

    /**
     * Updates node if it contains defer
     * @param node node that is currently being visited
     * @param context context for traversed node
     * */
    private void updateDeferredInfoForNode(Selection node, TraverserContext<Node> context) {
        if(containsEnabledDeferDirective(node)) {
            Selection prunedNode = removeDirectiveFromNode(node, DEFER_DIRECTIVE_NAME);

            if(isLeaf(node) || hasNonDeferredSelection(node)) {
                extractedEIs.add(generateDeferredEI(prunedNode, context));
            }

            //update node so children nodes has the correct definition
            TreeTransformerUtil.changeNode(context, prunedNode);
        }
    }

    /**
     * Generates an Execution Input given the node and the context
     * @param context current selection
     * @param currentNode context for the selection
     * @return An ExecutionInput
     * */
    private ExecutionInput generateDeferredEI(Selection currentNode, TraverserContext<Node> context) {
        //prune defer information from children
        Node prunedNode = AST_TRANSFORMER.transform(currentNode, childModifier);
        List<Node> parentNodes = getParentDefinitions(context);

        Set<String> neededFragmentSpreads = new HashSet<>();
        if(currentNode instanceof FragmentSpread) {
            neededFragmentSpreads.add(((FragmentSpread) currentNode).getName());
        }

        //builds parent nodes with pruned information
        for (Node parentNode : parentNodes) {
            prunedNode = transformNodeWithSelections(parentNode, (Selection)prunedNode, __typenameField);

            if(parentNode instanceof FragmentSpread) {
                neededFragmentSpreads.add(((FragmentSpread) parentNode).getName());
            }
        }

        //Gets all the definitions for the fragment spreads
        List<FragmentDefinition> fragmentSpreadDefs = getAllMapValuesWithMatchingKeys(fragmentDefinitionMap, neededFragmentSpreads);

        SelectionSet ss = SelectionSet.newSelectionSet().selection((Selection) prunedNode).build();
        //builds new OperationDefinition consisting with only pruned nodes
        OperationDefinition newOperation = this.operationDefinition.transform(builder -> builder.selectionSet(ss));

        List<Definition> deferredDefinitions = new ArrayList<>();
        deferredDefinitions.add(newOperation);
        deferredDefinitions.addAll(fragmentSpreadDefs);

        Document deferredDocument = this.rootNode.transform(builder -> builder.definitions(deferredDefinitions));

        String query = AstPrinter.printAst(deferredDocument);

        return originalEI.transform(builder -> builder.query(query));
    }

    /**
     * Builder for class
     * */
    public static class DeferQueryExtractorBuilder {
        PruneChildDeferSelectionsModifier childModifier;
        DeferOptions deferOptions;

        public DeferQueryExtractorBuilder deferOptions(DeferOptions deferOptions) {
            this.deferOptions = deferOptions;
            return childModifier(PruneChildDeferSelectionsModifier.builder().deferOptions(deferOptions).build());
        }

        private DeferQueryExtractorBuilder childModifier(PruneChildDeferSelectionsModifier childModifier) {
            this.childModifier = childModifier;
            return this;
        }

    }
}
