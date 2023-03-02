package com.intuit.graphql.orchestrator.visitors.queryVisitors;

import com.intuit.graphql.orchestrator.deferDirective.DeferOptions;
import com.intuit.graphql.orchestrator.utils.GraphQLUtil;
import graphql.ExecutionInput;
import graphql.GraphQLException;
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
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import lombok.Builder;
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
import static graphql.util.TreeTransformerUtil.changeNode;

/*
* Purpose of class: traverse query and create forked ei whenever there is a valid defer directive
*/
@Builder
public class DeferDirectiveQueryModifier extends QueryVisitorStub {

    //constants
    public static final String GENERATED_EIS = "GENERATED_EIS";

    //variables that will be manipulated throughout
    private final List<ExecutionInput> generatedEIs =  new ArrayList<>();

    //variables that are set when visitor is built
    @NonNull private final Document rootNode;
    @NonNull private final Map<String, FragmentDefinition> fragmentDefinitionMap;
    @NonNull private final PruneChildDeferSelectionsModifier CHILD_MODIFIER;
    @NonNull private ExecutionInput originalEI;

    @NonNull private DeferOptions deferOptions;


    /*
    * Functions:
    *    Updates the field if it contains defer directive.
    *    If it is a valid deferred node, generate new EI
    */
    @Override
    public TraversalControl visitField(Field node, TraverserContext<Node> context) {
        return updateDeferredInfoForNode(node, context);
    }

    /*
     * Functions:
     *    Updates the FragmentSpread if it contains defer directive.
     *    If it is a valid deferred node, generate new EI
     */
    @Override
    public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
        return updateDeferredInfoForNode(node, context);
    }

    @Override
    public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
       return updateDeferredInfoForNode(node, context);
    }

    private TraversalControl updateDeferredInfoForNode(Node node, TraverserContext<Node> context) {
        if(containsEnabledDeferDirective(node)) {
            Node prunedNode = removeDirectiveFromNode(node, DEFER_DIRECTIVE_NAME);

            if(hasNonDeferredSelection(node)) {
                generatedEIs.add(generateDeferredEI(prunedNode, context));
            }

            return changeNode(context, prunedNode);
        }

        return TraversalControl.CONTINUE;
    }

    /*
    * Generates an Execution Input given the node and the context
    * */
    private ExecutionInput generateDeferredEI(Node currentNode, TraverserContext<Node> context) {
        //prune defer information from children
        Node prunedNode = AST_TRANSFORMER.transform(currentNode, CHILD_MODIFIER);
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

        List<Definition> deferredDefinitions = new ArrayList<>();
        deferredDefinitions.add((OperationDefinition) prunedNode);
        deferredDefinitions.addAll(fragmentSpreadDefs);

        Document  deferredDocument = this.rootNode.transform(builder -> builder.definitions(deferredDefinitions));

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

        if(parentNode instanceof Field || parentNode instanceof FragmentDefinition || parentNode instanceof InlineFragment) {
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

    /*
    * Returns anything that will be used by the parent visitor
    * */
    public QueryCreatorResult.QueryCreatorResultBuilder addResultsToBuilder(QueryCreatorResult.QueryCreatorResultBuilder builder) {
        return builder.forkedDeferEIs(this.generatedEIs);
    }

    public static DeferDirectiveQueryModifierBuilder builder() {
        return new DeferDirectiveQueryModifierBuilder();
    }

    public static class DeferDirectiveQueryModifierBuilder {
        ExecutionInput originalEI;
        Document rootNode;
        Map<String, FragmentDefinition> fragmentDefinitionMap;

        DeferOptions deferOptions;

        PruneChildDeferSelectionsModifier childModifier;


        public DeferDirectiveQueryModifierBuilder originalEI (ExecutionInput originalEI) {
            this.originalEI = originalEI;
            return rootNode(originalEI);
        }

        public DeferDirectiveQueryModifierBuilder rootNode(ExecutionInput ei) {
            this.rootNode = GraphQLUtil.parser.parseDocument(ei.getQuery());
            return fragmentDefinitionMap(rootNode);
        }

        public DeferDirectiveQueryModifierBuilder fragmentDefinitionMap(Document rootNode) {
            this.fragmentDefinitionMap = rootNode.getDefinitionsOfType(FragmentDefinition.class)
                    .stream()
                    .collect(Collectors.toMap(FragmentDefinition::getName ,Function.identity()));
            return this;
        }

        public DeferDirectiveQueryModifierBuilder deferOptions(DeferOptions options) {
            this.deferOptions = options;

            return childModifier(options);
        }

        public DeferDirectiveQueryModifierBuilder childModifier(DeferOptions options) {
            this.CHILD_MODIFIER = PruneChildDeferSelectionsModifier.builder()
                    .deferOptions(options)
                    .build();

            return this;
        }
    }
}
