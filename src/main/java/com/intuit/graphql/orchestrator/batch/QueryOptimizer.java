package com.intuit.graphql.orchestrator.batch;

import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.language.Field;
import graphql.language.OperationDefinition.Operation;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import org.apache.commons.lang3.ObjectUtils;

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
    private final ServiceMetadata serviceMetadata;
    private final SelectionSet filteredSelection;

    public QueryOptimizer(Operation operationType, ServiceMetadata serviceMetadata, SelectionSet filteredSelection) {
        this.filteredSelection = filteredSelection;
        this.serviceMetadata = serviceMetadata;
        this.operationType = operationType;
    }

    public static <T> Predicate<T> distinctByFieldName(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static void createSelectionSetTree(SelectionSet selectionSet, HashMap<String, Set<Field>> selectionSetMap) {
        selectionSet.getSelections().stream().forEach(field -> {
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

    public static SelectionSet mergeFilteredSelection(Field node, HashMap<String, Set<Field>> selectionSetTree) {
        Field field;
        SelectionSet.Builder selectionSetBuilder = SelectionSet.newSelectionSet();
        SelectionSet.Builder childrenSelectionSetBuilder = SelectionSet.newSelectionSet();
        if (ObjectUtils.isEmpty(selectionSetTree.get(node.getName()))) { // check if leaf node
            field = new Field(node.getName(), node.getArguments());
            selectionSetBuilder.selection(field);
            return selectionSetBuilder.build();
        }
        List<SelectionSet> childrenSelectionSets = new ArrayList<>();
        for (Field f : selectionSetTree.get(node.getName())) {
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
                    .filter(distinctByFieldName(Field::getName))
                    .forEach(rootNode -> QueryOptimizer.mergeFilteredSelection(rootNode, selectionSetTree)
                            .getSelections()
                            .stream()
                            .forEach(mergedSelectionSetBuilder::selection));
            return mergedSelectionSetBuilder.build();
        }
        return filteredSelection;
    }

    private boolean canOptimizeQuery(SelectionSet selectionSet) {
        if (serviceMetadata.hasFieldResolverDirective()) return false;
        if (selectionSet == null) return true;
        for (Selection selection : selectionSet.getSelections()) {
            if (selection.getClass() != Field.class
                    || ((Field) selection).getDirectives().size() != 0
                    || !canOptimizeQuery(((Field) selection).getSelectionSet()))
                return false;
        }
        return true;
    }
}
