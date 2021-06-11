package com.intuit.graphql.orchestrator;

import com.intuit.graphql.orchestrator.batch.QueryExecutor;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Represents a Service Provider.
 */
public interface ServiceProvider extends QueryExecutor {

  /**
   * The namespace needs to be unique per provider. This will be appended to each type of the service in the overall
   * schema. This also helps keep the provider's types separate from the other providers and helps in type conflict
   * resolution.
   *
   * @return A unique identifier for the service provider.
   */
  String getNameSpace();

  /**
   * This will represent your graphql schema, you can keep it in single file as well as multiple files.
   *
   * @return The graphql schema files.
   */
  Map<String, String> sdlFiles();

  default Set<String> domainTypes() {
    return Collections.emptySet();
  }

  default ServiceType getSeviceType() {
    return ServiceType.GRAPHQL;
  }

  enum ServiceType {
    GRAPHQL,
    REST
  }
}
