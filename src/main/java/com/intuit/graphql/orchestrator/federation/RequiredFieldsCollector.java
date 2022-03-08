package com.intuit.graphql.orchestrator.federation;

import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Collects required fields for fields with {@code @resolver} and fields with {@code @requires}
 * directives.
 */
@Builder
public class RequiredFieldsCollector {

  private final String parentTypeName;
  private final Set<GraphQLFieldDefinition> fieldsWithResolver;
  private final Set<Field> fieldsWithRequiresDirective;
  private final ServiceMetadata serviceMetadata;

  public Set<Field> get() {
    Set<Field> output = new HashSet<>();
    output.addAll(getForFederation());
    output.addAll(getForResolver());
    return output;
  }

  private Set<Field> getForResolver() {
    // TODO
    return Collections.emptySet();
  }

  private Set<Field> getForFederation() {
    Set<Field> keysFieldSet = Collections.emptySet();
    if (this.serviceMetadata.isEntity(this.parentTypeName)) {
      FederationMetadata federationMetadata = this.serviceMetadata.getFederationServiceMetadata();
      EntityMetadata entityMetadata = federationMetadata.getEntityMetadataByName(parentTypeName);
      keysFieldSet =
          entityMetadata.getKeyDirectives().stream()
              .map(KeyDirectiveMetadata::getFieldSet)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());
    }

    Set<Field> requiresFieldSet = Collections.emptySet();
    if (CollectionUtils.isNotEmpty(fieldsWithRequiresDirective)) {
      FederationMetadata federationMetadata = this.serviceMetadata.getFederationServiceMetadata();
      requiresFieldSet =
          fieldsWithRequiresDirective.stream()
              .map(field -> FieldCoordinates.coordinates(parentTypeName, field.getName()))
              .filter(federationMetadata::hasRequiresFieldSet)
              .map(federationMetadata::getRequireFields)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());
    }

    return Stream.concat(keysFieldSet.stream(), requiresFieldSet.stream())
        .collect(Collectors.toSet());
  }
}
