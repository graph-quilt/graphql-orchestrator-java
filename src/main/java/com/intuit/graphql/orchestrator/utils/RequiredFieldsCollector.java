package com.intuit.graphql.orchestrator.utils;

import static graphql.schema.FieldCoordinates.coordinates;

import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;

/**
 * Collects required fields for fields with {@code @resolver} and fields with {@code @requires}
 * directives.
 */
@Builder
public class RequiredFieldsCollector {

  @NonNull private final String parentTypeName;
  @NonNull private final Set<Field> fieldsWithResolver;
  @NonNull private final Map<String, Field> excludedFields;
  @NonNull private final ServiceMetadata serviceMetadata;

  public Set<Field> get() {
    Set<Field> output = new HashSet<>();
    for (Field field : fieldsWithResolver) {
      FieldCoordinates fieldCoordinates = coordinates(parentTypeName, field.getName());
      // should be not null always of fieldsWithResolver was properly collected.
      FieldResolverContext fieldResolverContext =
          serviceMetadata.getFieldResolverContext(fieldCoordinates);
      Objects.requireNonNull(fieldResolverContext);

      output.addAll(
          fieldResolverContext.getRequiredFields().stream()
              .filter(s -> !excludedFields.containsKey(s))
              .map(s -> Field.newField(s).build())
              .collect(Collectors.toSet()));
    }
    return output;
  }
}
