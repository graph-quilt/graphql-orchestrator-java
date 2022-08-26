package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeExtensionDefinition;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import com.intuit.graphql.orchestrator.federation.validators.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.federation.validators.RequireValidator;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.language.Field;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.federation.FieldSetUtils.toFieldSet;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_EXTERNAL_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_REQUIRES_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FED_FIELD_DIRECTIVE_NAMES_SET;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.isTypeSystemForBaseType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getDirectivesWithNameFromDefinition;

@Slf4j
public class FederationTransformerPreMerge implements Transformer<XtextGraph, XtextGraph> {
    RequireValidator requireValidator = new RequireValidator();
    KeyDirectiveValidator keyDirectiveValidator = new KeyDirectiveValidator();

    @Override
    public XtextGraph transform(XtextGraph source) {
        if(source.getServiceProvider().isFederationProvider()) {
            FederationMetadata federationMetadata = new FederationMetadata(source.getServiceProvider());

            source.getEntitiesByTypeName().values()
            .forEach(typeDefinition -> {
                String typeDefinitionName = typeDefinition.getName();
                List<KeyDirectiveMetadata> keyDirectives = getDirectivesWithNameFromDefinition(typeDefinition, FEDERATION_KEY_DIRECTIVE)
                        .stream()
                        .peek(keyDirective -> keyDirectiveValidator.validate(source, typeDefinition, keyDirective.getArguments()))
                        .map(KeyDirectiveMetadata::from)
                        .collect(Collectors.toList());

                federationMetadata.addEntity(FederationMetadata.EntityMetadata.builder()
                        .typeName(typeDefinitionName)
                        .keyDirectives(keyDirectives)
                        .federationMetadata(federationMetadata)
                        .fields(FederationMetadata.EntityMetadata.getFieldsFrom(typeDefinition))
                        .build());

                validateFieldDefinitions(source, typeDefinition);
            });

            source.getEntityExtensionsByNamespace().get(source.getServiceProvider().getNameSpace())
                    .values().forEach( typeSystemDefinition ->  {
                String typeDefinitionName;
                List<KeyDirectiveMetadata> keyDirectives;
                Map<String, Set<Field>> requiredFieldsByFieldName;

                if(isTypeSystemForBaseType(typeSystemDefinition)) {
                    TypeDefinition typeDefinition = typeSystemDefinition.getType();
                    typeDefinitionName = typeDefinition.getName();
                    requiredFieldsByFieldName = getRequiredFields(typeDefinition);

                    keyDirectives = getDirectivesWithNameFromDefinition(typeDefinition, FEDERATION_KEY_DIRECTIVE)
                    .stream()
                    .peek(keyDirective -> keyDirectiveValidator.validate(source, typeDefinition, keyDirective.getArguments()))
                    .map(KeyDirectiveMetadata::from)
                    .collect(Collectors.toList());

                    validateFieldDefinitions(source, typeDefinition);
                    if(log.isDebugEnabled()) {
                        log.debug("Service {} created entity {}", source.getServiceProvider().getNameSpace(), typeDefinitionName);
                    }
                } else {
                    TypeExtensionDefinition typeExtensionDefinition = typeSystemDefinition.getTypeExtension();
                    typeDefinitionName = typeExtensionDefinition.getName();
                    requiredFieldsByFieldName = getRequiredFields(typeExtensionDefinition);

                    keyDirectives = getDirectivesWithNameFromDefinition(typeExtensionDefinition, FEDERATION_KEY_DIRECTIVE)
                    .stream()
                    .peek(keyDirective -> keyDirectiveValidator.validate(source, typeExtensionDefinition, keyDirective.getArguments()))
                    .map(KeyDirectiveMetadata::from)
                    .collect(Collectors.toList());

                    validateFieldDefinitions(source, typeExtensionDefinition);
                }

                EntityExtensionMetadata entityExtensionMetadata = EntityExtensionMetadata.builder()
                        .typeName(typeDefinitionName)
                        .keyDirectives(keyDirectives)
                        .requiredFieldsByFieldName(requiredFieldsByFieldName)
                        .federationMetadata(federationMetadata)
                        .build();
                source.addToEntityExtensionMetadatas(entityExtensionMetadata);
                federationMetadata.addEntityExtension(entityExtensionMetadata);
            });

            source.addFederationMetadata(federationMetadata);
        }

        return source;
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

    private void validateFieldDefinitions(XtextGraph source, TypeExtensionDefinition typeDefinition) {
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

    private Map<String, Set<Field>> getRequiredFields(TypeDefinition entityDefinition) {
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

    private Map<String, Set<Field>> getRequiredFields(TypeExtensionDefinition entityDefinition) {
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
            .anyMatch(directive -> directive.getDefinition().getName().equals(FEDERATION_EXTERNAL_DIRECTIVE));
    }
}
