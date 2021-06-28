package com.intuit.graphql.orchestrator.fieldresolver;

import com.intuit.graphql.orchestrator.batch.QueryOperationModifier;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLSchema;

import java.util.Map;

public class TypenameInjector {

    // consider moving QueryOperationModifier code here if the sole purpose is to inject typename
    private final QueryOperationModifier queryOperationModifier = new QueryOperationModifier();


    public OperationDefinition process(OperationDefinition operationDefinition,
                                       GraphQLSchema graphQLSchema,
                                       Map<String, FragmentDefinition> fragmentsByName,
                                       Map<String, Object> variables) {

        return queryOperationModifier.modifyQuery(graphQLSchema, operationDefinition, fragmentsByName, variables);
    }

}
