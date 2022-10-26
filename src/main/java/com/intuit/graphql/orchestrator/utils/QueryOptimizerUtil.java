package com.intuit.graphql.orchestrator.utils;

import graphql.language.Field;
import graphql.language.SelectionSet;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryOptimizerUtil {

    public static void createSelectionSetTree(SelectionSet selectionSet, HashMap<String, Set<Field>> selectionSetMap){
        selectionSet.getSelections().stream().forEach(field -> {
            Field f = (Field) field;
            if(f.getSelectionSet() != null){ // check if not a leaf node
                if(selectionSetMap.containsKey(f.getName()) ) {
                    selectionSetMap.get(f.getName())
                            .addAll((f.getSelectionSet().getSelections()
                                    .stream()
                                    .map(s ->(Field) s)
                                    .collect(Collectors.toList())));
                }
                else selectionSetMap.put(f.getName(), new HashSet<>(
                        f.getSelectionSet().getSelections()
                                .stream()
                                .map(s -> (Field)s)
                                .collect(Collectors.toList())));
                createSelectionSetTree(f.getSelectionSet(), selectionSetMap);
            }
        });
    }

    public static SelectionSet mergeFilteredSelection(Field node, HashMap<String, Set<Field>> selectionSetTree){
        Field field;
        SelectionSet.Builder selectionSetBuilder = SelectionSet.newSelectionSet();
        SelectionSet.Builder childrenSelectionSetBuilder = SelectionSet.newSelectionSet();
        if (ObjectUtils.isEmpty(selectionSetTree.get(node.getName()))) { // check if leaf node
            field = new Field(node.getName(), node.getArguments());
            selectionSetBuilder.selection(field);
            return selectionSetBuilder.build();
        }
        List<SelectionSet> childrenSelectionSets = new ArrayList<>();
        for( Field f : selectionSetTree.get(node.getName())) {
            childrenSelectionSets.add(mergeFilteredSelection(f, selectionSetTree));
        }
        childrenSelectionSets
                .stream()
                .map(s -> s.getSelections())
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
                .stream()
                .forEach(childrenSelectionSetBuilder::selection);
        field = new Field(node.getName(), node.getArguments(), childrenSelectionSetBuilder.build());
        selectionSetBuilder.selection(field);

        return selectionSetBuilder.build() ;
    }
}
