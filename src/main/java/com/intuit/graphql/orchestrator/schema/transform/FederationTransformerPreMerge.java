package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.federation.FieldSetUtils.toFieldSet;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_REQUIRES_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FED_FIELD_DIRECTIVE_NAMES_SET;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.isBaseType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getTypeDefinitionName;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.definitionContainsDirective;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getDirectivesWithNameFromDefinition;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import com.intuit.graphql.orchestrator.federation.validators.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.federation.validators.RequireValidator;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.language.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.ecore.EObject;

public class FederationTransformerPreMerge implements Transformer<XtextGraph, XtextGraph> {
    RequireValidator requireValidator = new RequireValidator();
    KeyDirectiveValidator keyDirectiveValidator = new KeyDirectiveValidator();

    @Override
    public XtextGraph transform(XtextGraph source) {
        if(!source.getServiceProvider().isFederationProvider()) {
            return source;
        }

        FederationMetadata federationMetadata = new FederationMetadata(source);
        Map<String, TypeDefinition> entitiesByTypename = new HashMap<>();
        Map<String, EObject> entityExtensionsByTypename = new HashMap<>();

        Stream.concat(
                source.getTypes().values().stream(),
                source.getEntityExtensionDefinitionsByName().values().stream()
        ).filter(typeDefinition -> definitionContainsDirective(typeDefinition, FEDERATION_KEY_DIRECTIVE))
        .forEach(typeDefinition -> {
                String typeDefinitionName = getTypeDefinitionName(typeDefinition);
                List<KeyDirectiveMetadata> keyDirectives = new ArrayList<>();
                getDirectivesWithNameFromDefinition(typeDefinition, FEDERATION_KEY_DIRECTIVE).forEach(directive -> {
                    keyDirectiveValidator.validate(source, typeDefinition, directive.getArguments());
                    keyDirectives.add(KeyDirectiveMetadata.from(directive));
                });

                if (isBaseType(typeDefinition)) {
                    entitiesByTypename.put(typeDefinitionName, (TypeDefinition) typeDefinition);
                    federationMetadata.addEntity(FederationMetadata.EntityMetadata.builder()
                        .typeName(typeDefinitionName)
                        .keyDirectives(keyDirectives)
                        .federationMetadata(federationMetadata)
                        .fields(FederationMetadata.EntityMetadata.getFieldsFrom(typeDefinition))
                        .build());
                } else {
                    EntityExtensionMetadata entityExtensionMetadata = EntityExtensionMetadata.builder()
                        .typeName(typeDefinitionName)
                        .keyDirectives(keyDirectives)
                        .requiredFieldsByFieldName(getRequiredFields(typeDefinition))
                        .federationMetadata(federationMetadata)
                        .build();
                    source.addToEntityExtensionMetadatas(entityExtensionMetadata);
                    entityExtensionsByTypename.put(typeDefinitionName, typeDefinition);
                    federationMetadata.addEntityExtension(entityExtensionMetadata);
                }
                validateFieldDefinitions(source, typeDefinition);
            });

        source.addFederationMetadata(federationMetadata);

        Map<String, Map<String, EObject>> entityExtensionByNamespace = new HashMap<>();
        entityExtensionByNamespace.put(source.getServiceProvider().getNameSpace(), entityExtensionsByTypename);
        return source.transform(builder -> builder
                .entitiesByTypeName(entitiesByTypename)
                .entityExtensionsByNamespace(entityExtensionByNamespace)
        );
    }

    private void validateFieldDefinitions(XtextGraph source, EObject typeDefinition) {
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

    private Map<String, Set<Field>> getRequiredFields(EObject entityDefinition) {
        Map<String, Set<Field>> output = new HashMap<>();
        getFieldDefinitions(entityDefinition).stream()
            .filter(fieldDefinition -> !containsExternalDirective(fieldDefinition))
            .forEach(fieldDefinition -> {
                Set<Field> regFields = getDirectivesWithNameFromDefinition(fieldDefinition, FEDERATION_REQUIRES_DIRECTIVE)
                    .stream()
                    .map(directive -> {
                        Optional<Argument> optionalArgument = directive.getArguments().stream().findFirst();
                        if (!optionalArgument.isPresent()) {
                            // validation is already being done, this should not happen
                            throw new IllegalStateException("require directive argument not found.");
                        }
                        Argument argument = optionalArgument.get();
                        ValueWithVariable valueWithVariable = argument.getValueWithVariable();
                        String fieldSetValue = valueWithVariable.getStringValue();
                        return toFieldSet(fieldSetValue);
                    })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
                output.put(fieldDefinition.getName(), regFields);
            });
        return output;
    }

    public static boolean containsExternalDirective(FieldDefinition fieldDefinition) {
        return fieldDefinition.getDirectives().stream()
            .anyMatch(directive -> directive.getDefinition().getName().equals("external"));
    }
}
