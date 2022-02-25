package com.intuit.graphql.orchestrator.federation.requiresdirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.exceptions.DirectiveMissingRequiredArgumentException;
import com.intuit.graphql.orchestrator.federation.exceptions.IncorrectDirectiveArgumentSizeException;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

import static com.intuit.graphql.orchestrator.utils.FederationUtils.FEDERATION_REQUIRES_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.checkFieldSetValidity;

public class RequireValidator {

    public void validate(XtextGraph sourceGraph, TypeDefinition typeDefinition, Directive requireDirective) {
        String containerName = typeDefinition.getName();

        checkDirectivesArgumentSize(requireDirective.getArguments(), containerName);

        Optional<Argument> argument = requireDirective.getArguments().stream().findFirst();
        if(argument.isPresent()) {
            checkRequireArgumentName(argument.get(), containerName);

            String fieldSet = argument.get().getValueWithVariable().getStringValue();
            checkFieldSetValidity(sourceGraph, typeDefinition, fieldSet, FEDERATION_REQUIRES_DIRECTIVE);
        }
    }

    private void checkDirectivesArgumentSize(List<Argument> argumentList, String containerName) {
        if(CollectionUtils.isEmpty(argumentList) || argumentList.size() != 1) {
            throw new IncorrectDirectiveArgumentSizeException(FEDERATION_REQUIRES_DIRECTIVE, containerName, 1);
        }
    }

    private void checkRequireArgumentName(Argument requireArgument, String containerName) {
        if(!StringUtils.equals("fields", requireArgument.getName())) {
            throw new DirectiveMissingRequiredArgumentException(FEDERATION_REQUIRES_DIRECTIVE, containerName);
        }
    }
}