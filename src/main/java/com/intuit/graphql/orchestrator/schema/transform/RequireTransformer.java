package com.intuit.graphql.orchestrator.schema.transform;

import com.google.common.annotations.VisibleForTesting;
import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.ArgumentsDefinition;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InputValueDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.exceptions.FederationDirectiveInvalidProviderException;
import com.intuit.graphql.orchestrator.federation.requiresDirective.RequireValidator;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.FEDERATION_REQUIRES_DIRECTIVE;

public class RequireTransformer implements Transformer<XtextGraph, XtextGraph> {

    @VisibleForTesting
    RequireValidator requireValidator = new RequireValidator();

    @Override
    public XtextGraph transform(final XtextGraph source) {


        for(final TypeDefinition typeDefinition : source.getTypes().values()) {
            List<FieldDefinition> fieldDefinitions;
            if(typeDefinition instanceof ObjectTypeDefinition || typeDefinition instanceof InterfaceTypeDefinition) {
                fieldDefinitions = getFieldDefinitions(typeDefinition);
            } else {
                fieldDefinitions = new ArrayList<>();
            }

            for (final FieldDefinition fieldDefinition : fieldDefinitions) {
                Directive requireDirective = getRequireArgument(fieldDefinition);
                if(requireDirective != null) {
                    if(source.getServiceProvider().isFederationProvider()) {
                        requireValidator.validate(source, typeDefinition, requireDirective);
                    } else {
                        throw new FederationDirectiveInvalidProviderException(FEDERATION_REQUIRES_DIRECTIVE);
                    }
                }
            }
        }

        return source;
    }

    private Directive getRequireArgument(FieldDefinition fieldDefinition) {
        return fieldDefinition.getDirectives().stream()
                .filter(directive -> directive.getDefinition().getName().equals(FEDERATION_REQUIRES_DIRECTIVE))
                .findFirst()
                .orElse(null);
    }
}
