package com.intuit.graphql.orchestrator.federation;

import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Collects required fields for fields with {@code @resolver} and fields with {@code @requires}
 * directives.
 */
@Builder
public class RequiredFieldsCollector {

  @NonNull private final String parentTypeName;
  @NonNull private final List<FieldResolverContext> fieldResolverContexts;
  @NonNull private final Set<Field> fieldsWithRequiresDirective;
  @NonNull private final ServiceMetadata serviceMetadata;
  @NonNull private final Map<String, Field> excludeFields;

  public Set<Field> get() {
    Set<Field> output = new HashSet<>();
    output.addAll(getForFederation());
    output.addAll(getForResolver());
    return output;
  }

  private Set<Field> getForResolver() {
    if (CollectionUtils.isEmpty(fieldResolverContexts)) {
      return Collections.emptySet();
    }
    return fieldResolverContexts.stream()
        .map(
            fieldResolverContext -> {
              if (CollectionUtils.isEmpty(fieldResolverContext.getRequiredFields())) {
                return Collections.<Field>emptySet();
              }
              return fieldResolverContext.getRequiredFields().stream()
                  .map(fieldName -> Field.newField(fieldName).build())
                  .collect(Collectors.toSet());
            })
        .flatMap(Collection::stream)
        .filter(this::notInExcludeList)
        .collect(Collectors.toSet());
  }

  private boolean notInExcludeList(Field field) {
    return !excludeFields.keySet().contains(field.getName());
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
              .filter(this::notInExcludeList)
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
              .filter(this::notInExcludeList)
              .collect(Collectors.toSet());
    }

    return Stream.concat(keysFieldSet.stream(), requiresFieldSet.stream())
        .collect(Collectors.toSet());
  }
}
