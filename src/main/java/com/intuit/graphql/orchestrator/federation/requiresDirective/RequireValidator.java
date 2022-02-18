package com.intuit.graphql.orchestrator.federation.requiresDirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.exceptions.FederationDirectiveInvalidProviderException;
import com.intuit.graphql.orchestrator.federation.keydirective.exceptions.KeyDirectiveException;
import com.intuit.graphql.orchestrator.stitching.StitchingException;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.language.Document;
import graphql.parser.Parser;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

import static com.intuit.graphql.orchestrator.utils.XtextUtils.FEDERATION_REQUIRES_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.checkFieldSetValidity;

public class RequireValidator {

    public void validate(XtextGraph sourceGraph, TypeDefinition typeDefinition,  Directive requireDirective) {
        String containerName = typeDefinition.getName();

        checkDirectivesArgumentSize(requireDirective.getArguments(), containerName);

        Optional<Argument> argument = requireDirective.getArguments().stream().findFirst();
        if(argument.isPresent()) {
            checkRequireArgumentName(argument.get(), containerName);

            String fieldSet = argument.get().getValueWithVariable().getStringValue();
            checkFieldSetValidity(sourceGraph, typeDefinition, fieldSet);
        }
    }

    private void checkDirectivesArgumentSize(List<Argument> argumentList, String containerName) {
        if(CollectionUtils.isEmpty(argumentList) || argumentList.size() != 1) {
            throw new KeyDirectiveException("f");
        }
    }

    private void checkRequireArgumentName(Argument requireArgument, String containerName) {
        if(!StringUtils.equals("fields", requireArgument.getName())) {
            throw new KeyDirectiveException(containerName);
        }
    }
}
