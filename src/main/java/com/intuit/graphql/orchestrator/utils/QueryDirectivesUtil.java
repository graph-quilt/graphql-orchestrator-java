package com.intuit.graphql.orchestrator.utils;

import graphql.language.Argument;
import graphql.language.BooleanValue;
import graphql.language.Directive;
import graphql.language.Field;
import graphql.language.Value;
import graphql.language.VariableReference;

import java.util.Map;
import java.util.Optional;

public class QueryDirectivesUtil {

    public static boolean shouldIgnoreNode(Field node, Map<String, Object> queryVariables) {
        Optional<Directive> optionalIncludesDir = node.getDirectives("include").stream().findFirst();
        Optional<Directive> optionalSkipDir = node.getDirectives("skip").stream().findFirst();
        if(optionalIncludesDir.isPresent() || optionalSkipDir.isPresent()) {
            if(optionalIncludesDir.isPresent() && (!getIfValue(optionalIncludesDir.get(), queryVariables))) {
                return true;
            }
            return optionalSkipDir.isPresent() && (getIfValue(optionalSkipDir.get(), queryVariables));
        }

        return false;
    }

    private static boolean getIfValue(Directive directive, Map<String, Object> queryVariables){
        Argument ifArg = directive.getArgument("if");
        Value ifValue = ifArg.getValue();

        boolean defaultValue = directive.getName().equals("skip");

        if(ifValue instanceof VariableReference) {
            String variableRefName = ((VariableReference) ifValue).getName();
            return (boolean) queryVariables.getOrDefault(variableRefName, defaultValue);
        } else if(ifValue instanceof BooleanValue) {
            return ((BooleanValue) ifValue).isValue();
        }
        return false;
    }
}
