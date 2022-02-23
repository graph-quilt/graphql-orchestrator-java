package com.intuit.graphql.orchestrator.schema.transform;

import com.google.common.annotations.VisibleForTesting;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.requiresdirective.RequireValidator;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;

import java.util.ArrayList;
import java.util.List;

import static com.intuit.graphql.orchestrator.utils.FederationUtils.FEDERATION_REQUIRES_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;

public class RequireTransformer implements Transformer<XtextGraph, XtextGraph> {

    @VisibleForTesting
    RequireValidator requireValidator = new RequireValidator();

    @Override
    public XtextGraph transform(final XtextGraph source) {

        if(source.getServiceProvider().isFederationProvider()) {
            for(final TypeDefinition typeDefinition : source.getTypes().values()) {
                List<FieldDefinition> fieldDefinitions = (typeDefinition instanceof ObjectTypeDefinition || typeDefinition instanceof InterfaceTypeDefinition) ?
                    getFieldDefinitions(typeDefinition) :  new ArrayList<>();

                for (final FieldDefinition fieldDefinition : fieldDefinitions) {
                    Directive requireDirective = getRequireArgument(fieldDefinition);
                    if(requireDirective != null) {
                        requireValidator.validate(source, typeDefinition, requireDirective);
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
