package com.intuit.graphql.orchestrator.utils;

import graphql.language.Field;
import graphql.language.SelectionSet;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class SelectionSetUtil {

    public static final String PATH_DELIMITER = ".";

    public static boolean isEmpty(SelectionSet selectionSet) {
       return selectionSet == null || CollectionUtils.isEmpty(selectionSet.getSelections());
    }

    public static Field getFieldWithPath(String path, SelectionSet selectionSet) {
        String[] pathTokens = StringUtils.split(path, PATH_DELIMITER);
        Field output = null;
        for (String pathToken : pathTokens) {
            Optional<Field> optionalField = getField(pathToken, selectionSet);
            if (optionalField.isPresent()) {
                output = optionalField.get();
                selectionSet = output.getSelectionSet();
            } else {
                throw new IllegalArgumentException(String.format("Invalid Path. field with name '%s' not found ", pathToken));
            }
        }
        return  output;
    }

    public static Optional<Field> getField(String fieldName, SelectionSet selectionSet) {
        return selectionSet.getSelections().stream()
                .filter(selection -> selection instanceof Field)
                .map(selection -> (Field) selection)
                .filter(field -> field.getName().equals(fieldName))
                .findAny();
    }
}
