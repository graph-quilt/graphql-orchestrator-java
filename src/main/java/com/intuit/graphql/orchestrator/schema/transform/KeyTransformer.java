package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.utils.XtextUtils.FEDERATION_KEY_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.typeContainsDirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.apollofederation.EntityExtensionContext;
import com.intuit.graphql.orchestrator.apollofederation.EntityExtensionDefinition;
import com.intuit.graphql.orchestrator.federation.keydirective.KeyDirectiveValidator;
import com.intuit.graphql.orchestrator.federation.keydirective.exceptions.InvalidLocationForFederationDirective;
import com.intuit.graphql.orchestrator.federation.keydirective.KeyDirectiveDefinition;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import graphql.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class is responsible for checking the merged graph for any key directives. For each field in key
 *  directive, this class will validate the fields to the key directive by ensuring they exist
 */
public class KeyTransformer implements Transformer<XtextGraph, XtextGraph> {

  @VisibleForTesting
  KeyDirectiveValidator keyDirectiveValidator = new KeyDirectiveValidator();

  @Override
  public XtextGraph transform(final XtextGraph source) {
    Map<String, TypeDefinition> entities = source.getTypes().values().stream()
            .filter(typeDefinition -> typeContainsDirective(typeDefinition, FEDERATION_KEY_DIRECTIVE))
            .collect(Collectors.toMap(TypeDefinition::getName, Function.identity()));

    if(entities.size() > 0 && !source.getServiceProvider().isFederationProvider()) {
      throw new InvalidLocationForFederationDirective(FEDERATION_KEY_DIRECTIVE);
    }

    for(final TypeDefinition entityDefinitions : entities.values()) {
      for (final Directive directive : entityDefinitions.getDirectives()) {
        if(directive.getDefinition().getName().equals(FEDERATION_KEY_DIRECTIVE)) {
          List<Argument> arguments = directive.getArguments();
          keyDirectiveValidator.validate(entityDefinitions, arguments);
        }
      }
    }

    // TODO This is temporary code
    source.getTypes().values().stream()
        .forEach(typeDefinition -> {
          if (hasKeyDirective(typeDefinition) && hasExtends(typeDefinition)) {
            EntityExtensionDefinition entityExtensionDefinition = createEntityExtensionDefinition(typeDefinition, source);
            source.addToEntitiesExtension(entityExtensionDefinition.getTypeName(), entityExtensionDefinition);
            List<EntityExtensionContext> entityExtensionContexts = createEntityExtensionContexts(typeDefinition, entityExtensionDefinition, source);
            entityExtensionContexts.forEach(entityExtensionContext ->
                source.addToEntitiesExtensionFields(entityExtensionContext.getFieldCoordinate(), entityExtensionContext)
            );
          };
          if (hasKeyDirective(typeDefinition)) {
            source.addToEntities(typeDefinition);
          };
        });

    return source.transform(builder -> builder.entitiesByTypeName(entities));
  }

  private EntityExtensionDefinition createEntityExtensionDefinition(TypeDefinition typeDefinition, XtextGraph source) {
    List<KeyDirectiveDefinition> keyDirectiveDefinitions = typeDefinition.getDirectives().stream()
        .filter(directive -> directive.getDefinition().getName().equals("key"))
        .map(directive -> KeyDirectiveDefinition.from(directive))
        .collect(Collectors.toList());
    return EntityExtensionDefinition.builder()
        .typeName(typeDefinition.getName())
        .additionFieldDefinitions(getFieldDefinitions(typeDefinition))
        .serviceMetadata(source)
        .keyDirectiveDefinitions(keyDirectiveDefinitions)
        .build();
  }

  private List<FieldDefinition> getFieldDefinitions(TypeDefinition typeDefinition) {
    if (typeDefinition instanceof InterfaceTypeDefinition) {
      InterfaceTypeDefinition interfaceTypeDefinition = (InterfaceTypeDefinition)typeDefinition;
      return new ArrayList<>(interfaceTypeDefinition.getFieldDefinition());
    }
    if (typeDefinition instanceof ObjectTypeDefinition) {
      ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition)typeDefinition;
      return new ArrayList<>(objectTypeDefinition.getFieldDefinition());
    }
    throw new IllegalArgumentException("Expecting argument to be of type InterfaceTypeDefinition "
        + "or ObjectTypeDefinition but got " + typeDefinition.getClass().getSimpleName());
  }

  private List<EntityExtensionContext> createEntityExtensionContexts(TypeDefinition typeDefinition,
      EntityExtensionDefinition entityExtensionDefinition, XtextGraph source) {
    if (typeDefinition instanceof InterfaceTypeDefinition) {
      InterfaceTypeDefinition interfaceTypeDefinition = (InterfaceTypeDefinition)typeDefinition;
      return interfaceTypeDefinition.getFieldDefinition().stream()
          .filter(fieldDefinition -> !containsExternalDirective(fieldDefinition))
          .map(fieldDefinition ->
              EntityExtensionContext.builder()
                  .fieldDefinition(fieldDefinition)
                  .parentTypeDefinition(typeDefinition)
                  .requiresTypeNameInjection(true)
                  .serviceMetadata(source)
                  .thisEntityExtensionDefinition(entityExtensionDefinition)
                  .build()
          )
          .collect(Collectors.toList());
    }
    if (typeDefinition instanceof ObjectTypeDefinition) {
      ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition)typeDefinition;
      return objectTypeDefinition.getFieldDefinition().stream()
          .filter(fieldDefinition -> !containsExternalDirective(fieldDefinition))
          .map(fieldDefinition ->
              EntityExtensionContext.builder()
                  .fieldDefinition(fieldDefinition)
                  .parentTypeDefinition(typeDefinition)
                  .requiresTypeNameInjection(true)
                  .serviceMetadata(source)
                  .thisEntityExtensionDefinition(entityExtensionDefinition)
                  .build()
          )
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  private boolean hasExtends(TypeDefinition typeDefinition) {
    return typeDefinition.getDirectives().stream()
        .anyMatch(directive -> directive.getDefinition().getName().equals("extends"));
  }

  private boolean hasKeyDirective(TypeDefinition typeDefinition) {
    return typeDefinition.getDirectives().stream()
        .anyMatch(directive -> directive.getDefinition().getName().equals("key"));
  }

  private boolean containsExternalDirective(FieldDefinition fieldDefinition) {
    return fieldDefinition.getDirectives().stream()
        .anyMatch(directive -> directive.getDefinition().getName().equals("external"));
  }

}



