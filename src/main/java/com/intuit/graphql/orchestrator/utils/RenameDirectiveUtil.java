package com.intuit.graphql.orchestrator.utils;


import graphql.language.Field;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class RenameDirectiveUtil {

    private RenameDirectiveUtil(){}

    public static Field convertGraphqlFieldWithOriginalName(Field renamedField, String originalName) {
        String alias = renamedField.getName();

        return renamedField.transform( builder ->
                builder.alias(alias).name(originalName)
        );
    }

    public static String getRenameKey(String parentTypeName, String aliasName, boolean isOperation) {
        return (isOperation) ? aliasName :StringUtils.join(Arrays.asList(parentTypeName, aliasName), "-");
    }
}
