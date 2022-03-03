package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.utils.FederationUtils.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.FEDERATION_REQUIRES_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.containsExternalDirective;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.isBaseType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getDirectivesFromDefinition;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.typeContainsDirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.orchestrator.federation.keydirective.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.xtext.util.Strings;

/**
 * This class is responsible for checking the merged graph for any key directives. For each field in key
 *  directive, this class will validate the fields to the key directive by ensuring they exist
 */
public class KeyTransformer implements Transformer<XtextGraph, XtextGraph> {

  @VisibleForTesting
  KeyDirectiveValidator keyDirectiveValidator = new KeyDirectiveValidator();

  @Override
  public XtextGraph transform(final XtextGraph source) {
    if(source.getServiceProvider().isFederationProvider()) {
      Map<String, TypeDefinition> entitiesByTypename = new HashMap<>();
      Map<String, TypeDefinition> entityExtensionsByTypename = new HashMap<>();

      Map<String, TypeDefinition> entities = source.getTypes().values().stream()
          .filter(typeDefinition -> typeContainsDirective(typeDefinition, FEDERATION_KEY_DIRECTIVE))
          .collect(Collectors.toMap(TypeDefinition::getName, Function.identity()));

      FederationMetadata federationMetadata = new FederationMetadata(source);
      for(final TypeDefinition entityDefinition : entities.values()) {
        List<KeyDirectiveMetadata> keyDirectives = new ArrayList<>();
        getDirectivesFromDefinition(entityDefinition, FEDERATION_KEY_DIRECTIVE).stream()
            .peek(directive -> keyDirectiveValidator.validate(source, entityDefinition, directive.getArguments()))
            .forEach(directive -> keyDirectives.add(KeyDirectiveMetadata.from(directive))
            );

        if (isBaseType(entityDefinition)) {
          entitiesByTypename.put(entityDefinition.getName(), entityDefinition);
          federationMetadata.addEntity(EntityMetadata.builder()
              .typeName(entityDefinition.getName())
              .keyDirectives(keyDirectives)
              .fields(EntityMetadata.getFieldsFrom(entityDefinition))
              .federationMetadata(federationMetadata)
              .build());
        } else {
          EntityExtensionMetadata entityExtensionMetadata = EntityExtensionMetadata.builder()
              .typeName(entityDefinition.getName())
              .keyDirectives(keyDirectives)
              .requiredFieldsByFieldName(getRequiredFields(entityDefinition))
              .federationMetadata(federationMetadata)
              .build();
          source.addToEntityExtensionMetadatas(entityExtensionMetadata);
          entityExtensionsByTypename.put(entityDefinition.getName(), entityDefinition);
          federationMetadata.addEntityExtension(entityExtensionMetadata);
        }
      }
      source.addFederationMetadata(federationMetadata);
      Map<String, Map<String, TypeDefinition>> entityExtensionByNamespace = new HashMap<>();
      entityExtensionByNamespace.put(source.getServiceProvider().getNameSpace(), entityExtensionsByTypename);
      return source.transform(builder -> builder
          .entitiesByTypeName(entitiesByTypename)
          .entityExtensionsByNamespace(entityExtensionByNamespace));
    } else {
      return source;
    }
  }

  private Map<String, Set<String>> getRequiredFields(TypeDefinition entityDefinition) {
    Map<String, Set<String>> output = new HashMap<>();
    getFieldDefinitions(entityDefinition).stream()
        .filter(fieldDefinition -> !containsExternalDirective(fieldDefinition))
        .forEach(fieldDefinition -> {
          Set<String> regFields = getDirectivesFromDefinition(fieldDefinition, FEDERATION_REQUIRES_DIRECTIVE)
              .stream()
              .flatMap(directive -> {
                Optional<Argument> optionalArgument = directive.getArguments().stream().findFirst();
                if (!optionalArgument.isPresent()) {
                  // validation is already being done, this should not happen
                  throw new IllegalStateException("require directive argument not found.");
                }
                Argument argument = optionalArgument.get();
                ValueWithVariable valueWithVariable = argument.getValueWithVariable();
                String fieldSetValue = valueWithVariable.getStringValue();
                return new HashSet<>(Strings.split(fieldSetValue, StringUtils.SPACE)).stream();
              })
              .collect(Collectors.toSet());
          output.put(fieldDefinition.getName(), regFields);
        });
    return output;
  }

}
