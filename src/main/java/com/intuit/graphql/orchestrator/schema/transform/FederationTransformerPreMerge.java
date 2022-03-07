package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.keydirective.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import com.intuit.graphql.orchestrator.federation.requiresdirective.RequireValidator;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_REQUIRES_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FED_FIELD_DIRECTIVE_NAMES_SET;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.isBaseType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getDirectivesWithNameFromDefinition;

public class FederationTransformerPreMerge implements Transformer<XtextGraph, XtextGraph> {
    RequireValidator requireValidator = new RequireValidator();
    KeyDirectiveValidator keyDirectiveValidator = new KeyDirectiveValidator();

    @Override
    public XtextGraph transform(XtextGraph source) {
        if(!source.getServiceProvider().isFederationProvider()) {
            return source;
        }

        Map<String, TypeDefinition> entitiesByTypename = new HashMap<>();
        Map<String, TypeDefinition> entityExtensionsByTypename = new HashMap<>();

        source.getTypes().values().forEach(typeDefinition -> {
            getDirectivesWithNameFromDefinition(typeDefinition, FEDERATION_KEY_DIRECTIVE).forEach(directive -> {
                FederationMetadata federationMetadata = new FederationMetadata(source);
                List<KeyDirectiveMetadata> keyDirectives = new ArrayList<>();
                keyDirectiveValidator.validate(source, typeDefinition, directive.getArguments());

                if (isBaseType(typeDefinition)) {
                    entitiesByTypename.put(typeDefinition.getName(), typeDefinition);
                    federationMetadata.addEntity(FederationMetadata.EntityMetadata.builder()
                        .typeName(typeDefinition.getName())
                        .keyDirectives(keyDirectives)
                        .federationMetadata(federationMetadata)
                        .fields(FederationMetadata.EntityMetadata.getFieldsFrom(typeDefinition))
                        .build());
                } else {
                    entityExtensionsByTypename.put(typeDefinition.getName(), typeDefinition);
                    federationMetadata.addEntityExtension(FederationMetadata.EntityExtensionMetadata.builder()
                        .typeName(typeDefinition.getName())
                        .keyDirectives(keyDirectives)
                        .federationMetadata(federationMetadata)
                        .build());
                }
            });

            validateFieldDefinitions(source, typeDefinition);
        });

        Map<String, Map<String, TypeDefinition>> entityExtensionByNamespace = new HashMap<>();
        entityExtensionByNamespace.put(source.getServiceProvider().getNameSpace(), entityExtensionsByTypename);
        return source.transform(builder -> builder
                .entitiesByTypeName(entitiesByTypename)
                .entityExtensionsByNamespace(entityExtensionByNamespace));
    }

    private void validateFieldDefinitions(XtextGraph source, TypeDefinition typeDefinition) {
        getFieldDefinitions(typeDefinition, true)
        .stream()
        .map(fieldDefinition -> getDirectivesWithNameFromDefinition(fieldDefinition, FED_FIELD_DIRECTIVE_NAMES_SET ))
        .flatMap(Collection::stream)
        .forEach(directive -> {
            if(StringUtils.equals(FEDERATION_REQUIRES_DIRECTIVE, directive.getDefinition().getName())) {
                requireValidator.validate(source, typeDefinition, directive);
            }
        });
    }
}
