package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.utils.FederationUtils.FEDERATION_EXTENDS_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.FederationUtils.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.typeContainsDirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.federation.keydirective.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

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

      for(final TypeDefinition entityDefinition : entities.values()) {
        FederationMetadata federationMetadata = new FederationMetadata();
        List<KeyDirectiveMetadata> keyDirectives = new ArrayList<>();
        for (final Directive directive : entityDefinition.getDirectives()) {
          if(directive.getDefinition().getName().equals(FEDERATION_KEY_DIRECTIVE)) {
            List<Argument> arguments = directive.getArguments();
            keyDirectiveValidator.validate(source, entityDefinition, arguments);
          }
        }
        if (isEntity(entityDefinition)) {
          entitiesByTypename.put(entityDefinition.getName(), entityDefinition);
          federationMetadata.addEntity(EntityMetadata.builder()
              .typeName(entityDefinition.getName())
              .keyDirectives(keyDirectives)
              .serviceMetadata(source)
              .fields(EntityMetadata.getFieldsFrom(entityDefinition))
              .build());
        } else {
          entityExtensionsByTypename.put(entityDefinition.getName(), entityDefinition);
          federationMetadata.addEntityExtension(EntityExtensionMetadata.builder()
              .typeName(entityDefinition.getName())
              .keyDirectives(keyDirectives)
              //.externalFields() TODO implement
              //.requiredFields() TODO implement
              .serviceMetadata(source)
              .build());
        }
      }
      Map<String, Map<String, TypeDefinition>> entityExtensionByNamespace = new HashMap<>();
      entityExtensionByNamespace.put(source.getServiceProvider().getNameSpace(), entityExtensionsByTypename);
      return source.transform(builder -> builder
          .entitiesByTypeName(entitiesByTypename)
          .entityExtensionsByNamespace(entityExtensionByNamespace));
    } else {
      return source;
    }
  }

  private boolean isEntity(TypeDefinition entityDefinition) {
    return entityDefinition.getDirectives().stream()
        .map(directive -> directive.getDefinition().getName())
        .anyMatch(name -> StringUtils.equals(FEDERATION_EXTENDS_DIRECTIVE, name));
  }
}
