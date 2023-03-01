package com.intuit.graphql.orchestrator.visitors.queryVisitors;

import com.intuit.graphql.orchestrator.utils.GraphQLUtil;
import graphql.ExecutionInput;
import graphql.GraphQLException;
import graphql.language.Argument;
import graphql.language.AstPrinter;
import graphql.language.AstTransformer;
import graphql.language.BooleanValue;
import graphql.language.Definition;
import graphql.language.Directive;
import graphql.language.DirectivesContainer;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_IF_ARG;
import static com.intuit.graphql.orchestrator.utils.IntrospectionUtil.__typenameField;
import static graphql.util.TreeTransformerUtil.changeNode;

/*
* Purpose of class: traverse query and create forked ei whenever there is a valid defer directive
* */
@Builder
public class DeferDirectiveQueryVisitor extends QueryVisitorStub {

    //constants
    public static final String GENERATED_EIS = "GENERATED_EIS";
    private static final AstTransformer AST_TRANSFORMER = new AstTransformer();

    //variables that will be manipulated throughout
    private final List<ExecutionInput> generatedEIs =  new ArrayList<>();
    private final Set<String> neededFragmentSpreads = new HashSet<>();

    //variables that are set when visitor is built
    private ExecutionInput originalEI;
    private final Document rootNode;
    private final Map<String, FragmentDefinition> fragmentDefinitionMap;
    private final PruneChildDeferSelectionsModifier PRUNE_CHILD_MODIFIER = PruneChildDeferSelectionsModifier
            .builder()
            .nestedDefersAllowed(true)
            .build();


    /*
    * Functions:
    *    Updates the field if it contains defer directive.
    *    If it is a valid deferred node, generate new EI
    */
    @Override
    public TraversalControl visitField(Field node, TraverserContext<Node> context) {
        if(containsValidDeferDirective(node)) {
            Node prunedNode = pruneDeferDirective(node);

            if(isValidDeferredSelection(node)) {
                generatedEIs.add(generateDeferredEI(prunedNode, context));
            }

            return changeNode(context, prunedNode);
        }

        return TraversalControl.CONTINUE;
    }

    /*
     * Functions:
     *    Updates the FragmentSpread if it contains defer directive.
     *    If it is a valid deferred node, generate new EI
     */
    @Override
    public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
        if(containsValidDeferDirective(node)) {
            Node prunedNode = pruneDeferDirective(node);

            if(isValidDeferredSelection(node)) {
                neededFragmentSpreads.add(node.getName());
                generatedEIs.add(generateDeferredEI(prunedNode, context));
            }

            return changeNode(context, prunedNode);
        }

        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
        if(containsValidDeferDirective(node)) {
            Node prunedNode = pruneDeferDirective(node);

            if(isValidDeferredSelection(node)) {
                generatedEIs.add(generateDeferredEI(prunedNode, context));
            }

            return changeNode(context, prunedNode);
        }

        return TraversalControl.CONTINUE;
    }

    /*
    * Checks if it is necessary to create ei for deferred field.
    * Currently, selection should be skipped if all the children field are deferred resulting in an empty selection set.
    */
    private boolean isValidDeferredSelection(Node selection) {
        Optional<SelectionSet> possibleSelectionSet = ((List<Node>)selection.getChildren())
                .stream()
                .filter(SelectionSet.class::isInstance)
                .map(SelectionSet.class::cast)
                .findAny();

                //it is leaf node with no children
        return !possibleSelectionSet.isPresent() ||
                // at least one of the children are not deferred
                possibleSelectionSet.get().getChildren().stream().anyMatch(child -> !containsValidDeferDirective(child));
    }

    /*
    * Verifies that Node has defer directive that is not disabled
    */
    private boolean containsValidDeferDirective(Node node) {
        return node instanceof DirectivesContainer && containsValidDeferDirective((DirectivesContainer) node);
    }

    /*
     * Verifies that DirectiveContainer has defer directive that is not disabled
     */
    private boolean containsValidDeferDirective(DirectivesContainer directivesContainer) {
        //DirectivesContainer returns list as List<Object> so casting to correct type
        List<Directive> nodesDirectives = directivesContainer.getDirectives();

        return nodesDirectives
                .stream()
                .filter(directive ->  DEFER_DIRECTIVE_NAME.equals(directive.getName()))
                .findFirst()
                .map(directive -> {
                    Argument ifArg = directive.getArgument(DEFER_IF_ARG);
                    return ifArg == null || ((BooleanValue) ifArg.getValue()).isValue();
                })
                .orElse(false);

    }

    /*
    * Generates an Execution Input given the node and the context
    * */
    private ExecutionInput generateDeferredEI(Node currentNode, TraverserContext<Node> context) {
        //prune defer information from children
        Node prunedNode = AST_TRANSFORMER.transform(currentNode, PRUNE_CHILD_MODIFIER);
        List<Node> parentNodes = getParentDefinitions(context);
        //builds new OperationDefinition consisting with only pruned nodes
        for (Node parentNode : parentNodes) {
            prunedNode = constructNewPrunedNode(parentNode, prunedNode);
        }

        //Gets all the definitions for the fragment spreads
        List<FragmentDefinition> fragmentSpreadDefs = this.neededFragmentSpreads
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

    /*
    * Transforms node by removing defer directive from the node.
    * Returns transformed node
    * Throws an exception if it is at a location that is not supported.
    * */
    private Node pruneDeferDirective(Node deferredNode) {
        List<Directive> prunedDirectives;
        if(deferredNode instanceof Field) {
            prunedDirectives = ((Field) deferredNode).getDirectives()
                    .stream()
                    .filter(directive -> !DEFER_DIRECTIVE_NAME.equals(directive.getName()))
                    .collect(Collectors.toList());

            return ((Field) deferredNode).transform(builder -> builder.directives(prunedDirectives));
        } else if(deferredNode instanceof InlineFragment) {
            prunedDirectives = ((InlineFragment) deferredNode).getDirectives()
                    .stream()
                    .filter(directive -> !DEFER_DIRECTIVE_NAME.equals(directive.getName()))
                    .collect(Collectors.toList());

            return ((InlineFragment) deferredNode).transform(builder -> builder.directives(prunedDirectives));
        }  else if(deferredNode instanceof FragmentSpread) {
            prunedDirectives = ((FragmentSpread) deferredNode).getDirectives()
                    .stream()
                    .filter(directive -> !DEFER_DIRECTIVE_NAME.equals(directive.getName()))
                    .collect(Collectors.toList());

            return ((FragmentSpread) deferredNode).transform(builder -> builder.directives(prunedDirectives));
        } else {
            throw new GraphQLException("Unsupported type has defer directive.");
        }
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
    //Could possibly change this to a builder for the QueryCreatorResult
    @Override
    public Map<String, Object> getResults() {
        HashMap results = new HashMap();
        results.put(GENERATED_EIS, this.generatedEIs);
        return results;
    }

    public static DeferDirectiveQueryVisitorBuilder builder() {
        return new DeferDirectiveQueryVisitorBuilder();
    }

    public static class DeferDirectiveQueryVisitorBuilder {
        ExecutionInput originalEI;
        Document rootNode;
        Map<String, FragmentDefinition> fragmentDefinitionMap;

        public DeferDirectiveQueryVisitorBuilder originalEI (ExecutionInput originalEI) {
            this.originalEI = originalEI;
            return rootNode(originalEI);
        }

        public DeferDirectiveQueryVisitorBuilder rootNode(ExecutionInput ei) {
            this.rootNode = GraphQLUtil.parser.parseDocument(ei.getQuery());
            return fragmentDefinitionMap(rootNode);
        }

        public DeferDirectiveQueryVisitorBuilder fragmentDefinitionMap(Document rootNode) {
            this.fragmentDefinitionMap = rootNode.getDefinitionsOfType(FragmentDefinition.class)
                    .stream()
                    .collect(Collectors.toMap(FragmentDefinition::getName ,Function.identity()));
            return this;
        }
    }
}
