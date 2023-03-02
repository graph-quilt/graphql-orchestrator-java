package com.intuit.graphql.orchestrator.utils;

import graphql.language.Directive;
import graphql.language.DirectivesContainer;
import graphql.language.Field;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

public class NodeUtils {

    /*
     * Transforms node by removing desired directive from the node.
     * Returns transformed node
     * Throws an exception if it is at a location that is not supported.
     * */
    public static Node removeDirectiveFromNode(Node  node, String directiveName) {
        if(!(node instanceof DirectivesContainer)) {
            throw new RuntimeException("Cannot retrieve directives from a non directive container");
        }

        //DirectivesContainer returns getDirectives as a List<Object> so need to cast to stream correctly
        List<Directive> prunedDirectives = ((List<Directive>)((DirectivesContainer)node).getDirectives())
                .stream()
                .filter(directive -> ObjectUtils.notEqual(directiveName, directive.getName()))
                .collect(Collectors.toList());

        if(node instanceof Field) {
           return ((Field) node).transform(builder -> builder.directives(prunedDirectives));
        } else if(node instanceof InlineFragment) {
            return ((InlineFragment) node).transform(builder -> builder.directives(prunedDirectives));
        }  else if(node instanceof FragmentSpread) {
            return ((FragmentSpread) node).transform(builder -> builder.directives(prunedDirectives));
        } else {
            return node;
        }
        //feel free to add additional types to transform as they come up
        //...there are just 25 options and did not feel all was needed
    }
}
