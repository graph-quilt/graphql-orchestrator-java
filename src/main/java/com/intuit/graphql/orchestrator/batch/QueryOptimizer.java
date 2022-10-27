package com.intuit.graphql.orchestrator.batch;

import graphql.language.Field;
import graphql.language.OperationDefinition.Operation;
import graphql.language.Selection;
import graphql.language.SelectionSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class QueryOptimizer {

    private final Operation operationType;
    private final SelectionSet filteredSelection;

    public QueryOptimizer(Operation operationType, SelectionSet filteredSelection) {
        this.filteredSelection = filteredSelection;
        this.operationType = operationType;
    }

    public static <T> Predicate<T> distinctByFieldName(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static void createSelectionSetTree(SelectionSet selectionSet, HashMap<String, Set<Field>> selectionSetMap) {
        selectionSet.getSelections().forEach(field -> {
            Field f = (Field) field;
            if (f.getSelectionSet() != null) { // check if not a leaf node
                if (selectionSetMap.containsKey(f.getName())) {
                    selectionSetMap.get(f.getName())
                            .addAll((f.getSelectionSet().getSelections()
                                    .stream()
                                    .map(s -> (Field) s)
                                    .collect(Collectors.toList())));
                } else selectionSetMap.put(f.getName(), new HashSet<>(
                        f.getSelectionSet().getSelections()
                                .stream()
                                .map(s -> (Field) s)
                                .collect(Collectors.toList())));
                createSelectionSetTree(f.getSelectionSet(), selectionSetMap);
            }
        });
    }

    public static SelectionSet mergeFilteredSelection(Field node) {
        SelectionSet.Builder selectionSetBuilder = SelectionSet.newSelectionSet();
        SelectionSet.Builder childrenSelectionSetBuilder = SelectionSet.newSelectionSet();
        if (node.getSelectionSet() == null) { // check if leaf node
            selectionSetBuilder.selection(node);
            return selectionSetBuilder.build();
        }
        List<SelectionSet> childrenSelectionSets = new ArrayList<>();

        node.getSelectionSet().getSelections().forEach(selection ->
                childrenSelectionSets.add(mergeFilteredSelection((Field) selection)));

        childrenSelectionSets
                .stream()
                .map(s -> s.getSelections())
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
                .forEach(childrenSelectionSetBuilder::selection);
        selectionSetBuilder.selection(node.transform(builder ->
                builder.selectionSet(childrenSelectionSetBuilder.build())));

        return selectionSetBuilder.build();
    }

    public SelectionSet getTransformedSelectionSet() {
        if (operationType.name().equalsIgnoreCase("QUERY")
                && canOptimizeQuery(filteredSelection)) {
            SelectionSet.Builder mergedSelectionSetBuilder = SelectionSet.newSelectionSet();

            HashMap<String, Set<Field>> selectionSetTree = new HashMap<>();
            QueryOptimizer.createSelectionSetTree(filteredSelection, selectionSetTree);

            filteredSelection.getSelections().stream()
                    .map(rootNode -> (Field) rootNode)
                    //.filter(distinctByFieldName(Field::getName))
                    .forEach(rootNode -> QueryOptimizer.mergeFilteredSelection(rootNode)
                            .getSelections()
                            .stream()
                            .forEach(mergedSelectionSetBuilder::selection));
            return mergedSelectionSetBuilder.build();
        }
        return filteredSelection;
    }

    private boolean canOptimizeQuery(SelectionSet selectionSet) {
        if (selectionSet == null) return true;
        for (Selection selection : selectionSet.getSelections()) {
            if (selection.getClass() != Field.class
                    || !canOptimizeQuery(((Field) selection).getSelectionSet()))
                return false;
        }
        return true;
    }
}