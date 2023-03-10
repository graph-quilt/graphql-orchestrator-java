package com.intuit.graphql.orchestrator.utils;

import graphql.language.Directive;
import graphql.language.DirectivesContainer;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class NodeUtils {

    /**
     * Transforms node by removing desired directive from the node.
     * Returns transformed node
     * Throws an exception if it is at a location that is not supported.
     * @param node: the selection that you would like to prune
     * @param directiveName: the directive you would like to search for
     * @return selection: pruned selection
     * */
    public static Selection removeDirectiveFromNode(Selection node, String directiveName) {
        //DirectivesContainer returns getDirectives as a List<Object> so need to cast to stream correctly
        List<Directive> prunedDirectives = ((List<Directive>)((DirectivesContainer)node).getDirectives())
                .stream()
                .filter(directive -> ObjectUtils.notEqual(directiveName, directive.getName()))
                .collect(Collectors.toList());

        if(node instanceof Field) {
           return ((Field) node).transform(builder -> builder.directives(prunedDirectives));
        } else if(node instanceof InlineFragment) {
            return ((InlineFragment) node).transform(builder -> builder.directives(prunedDirectives));
        }  else {
            return ((FragmentSpread) node).transform(builder -> builder.directives(prunedDirectives));
        }
    }

    /**
     * Check if the selection has children selections
     * @param node selection that you are trying to check
     * @return boolean: true if selection doesn't have selectionset as child, false otherwise
     * */
    public static boolean isLeaf(Selection node) {
        return node.getChildren().isEmpty() ||
                node.getChildren()
                .stream()
                .noneMatch(SelectionSet.class::isInstance);
    }


    /**
     * Generates new node
     * Transforms the parentNode with a new selection set consisting of the pruned child and typename fields
     * @param parentNode node that will be transformed and will contain only selections
     * @param selections selections that need to be in selection set for parentNode
     * @return node that only contains the passed in selections
     * */
    public static Node transformNodeWithSelections(Node parentNode, Selection... selections) {
        SelectionSet prunedSelectionSet = SelectionSet.newSelectionSet()
                .selections(Arrays.asList(selections))
                .build();

        if(parentNode instanceof Field) {
            return ((Field) parentNode).transform(builder -> builder.selectionSet(prunedSelectionSet));
        } else if (parentNode instanceof FragmentDefinition) {
            //add fragment spread names here in case of nested fragment spreads
            return ((FragmentDefinition) parentNode).transform(builder -> builder.selectionSet(prunedSelectionSet));
        } else if (parentNode instanceof InlineFragment) {
            return ((InlineFragment) parentNode).transform(builder -> builder.selectionSet(prunedSelectionSet));
        } else {
            return ((OperationDefinition) parentNode).transform(builder -> builder.selectionSet(prunedSelectionSet));
        }
    }

    /**
     * Retrieves the objects from map matching key
     * @param kvMap: map that will be searched
     * @param keyCollection keys to check
     * @return list of values from map with matching key
     * */
    public static <K, V> List<V> getAllMapValuesWithMatchingKeys(Map<K, V> kvMap, Collection<K> keyCollection) {
        return keyCollection
                .stream()
                .map(kvMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
