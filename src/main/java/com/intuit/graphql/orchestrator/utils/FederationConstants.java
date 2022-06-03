package com.intuit.graphql.orchestrator.utils;

import graphql.language.Argument;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static graphql.language.TypeName.newTypeName;

public class FederationConstants {

    private FederationConstants(){}

    public static final String FEDERATION_KEY_DIRECTIVE = "key";
    public static final String FEDERATION_EXTERNAL_DIRECTIVE = "external";
    public static final String FEDERATION_EXTENDS_DIRECTIVE = "extends";

    public static final String FEDERATION_INACCESSIBLE_DIRECTIVE = "inaccessible";
    public static final String FEDERATION_REQUIRES_DIRECTIVE = "requires";
    public static final String FEDERATION_PROVIDES_DIRECTIVE = "provides";
    public static final String FEDERATION_FIELDS_ARGUMENT = "fields";

    public static final Set<String> FED_TYPE_DIRECTIVES_NAMES_SET =
            new HashSet<>(Arrays.asList(FEDERATION_KEY_DIRECTIVE, FEDERATION_EXTENDS_DIRECTIVE));

    public static final Set<String> FED_FIELD_DIRECTIVE_NAMES_SET =
            new HashSet<>(
                    Arrays.asList(
                            FEDERATION_EXTERNAL_DIRECTIVE,
                            FEDERATION_REQUIRES_DIRECTIVE,
                            FEDERATION_PROVIDES_DIRECTIVE));

    public static final String REPRESENTATIONS_VAR_NAME = "REPRESENTATIONS";
    public static final String REPRESENTATIONS_ARG_NAME = "representations";
    public static final String REPRESENTATIONS_TYPE_NAME = "[_Any!]!";

    public static final VariableDefinition VARIABLE_DEFINITION =
            VariableDefinition.newVariableDefinition()
                    .name(REPRESENTATIONS_VAR_NAME)
                    .type(newTypeName(REPRESENTATIONS_TYPE_NAME).build())
                    .build();

    public static final Argument REPRESENTATIONS_ARGUMENT =
            Argument.newArgument()
                    .name(REPRESENTATIONS_ARG_NAME)
                    .value(VariableReference.newVariableReference().name(REPRESENTATIONS_VAR_NAME).build())
                    .build();

    public static final String _ENTITIES_FIELD_NAME = "_entities";
    public static final String DATA_RESPONSE_FIELD = "data";
    public static final String ERRORS_RESPONSE_FIELD = "errors";



}
