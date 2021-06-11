package com.intuit.graphql.orchestrator.datafetcher;

import static java.util.Objects.requireNonNull;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.PropertyDataFetcher;
import java.util.Map;

/**
 * An {@link AliasablePropertyDataFetcher} that supports returning fields that have been aliased even if the data source
 * is a Map. If the source is not a map, just delegates to {@link PropertyDataFetcher}
 */
public class AliasablePropertyDataFetcher implements DataFetcher {

  private final String propertyName;
  private final PropertyDataFetcher defaultDataFetcher;

  public AliasablePropertyDataFetcher(final String propertyName) {
    this.propertyName = requireNonNull(propertyName);
    this.defaultDataFetcher = PropertyDataFetcher.fetching(propertyName);
  }

  @Override
  public Object get(final DataFetchingEnvironment env) {
    final Object source = env.getSource();
    if (source instanceof Map) {
      return getValueFromMap((Map) source, env);
    }
    return defaultDataFetcher.get(env);

  }

  private Object getValueFromMap(Map source, final DataFetchingEnvironment env) {
    String alias = env.getField().getAlias();

    // if an alias was provided then the map should know about the field by its alias
    if (alias != null && !alias.isEmpty() && source.containsKey(alias)) {
      return source.get(alias);
    }
    return source.get(propertyName);
  }
}
