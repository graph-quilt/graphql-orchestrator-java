package com.intuit.graphql.orchestrator.utils;

import static graphql.introspection.Introspection.SchemaMetaFieldDef;
import static graphql.introspection.Introspection.TypeMetaFieldDef;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;

import java.util.Arrays;
import java.util.List;

public class IntrospectionUtil {

  public static final List<String> INTROSPECTION_FIELDS =
      Arrays.asList(
          SchemaMetaFieldDef.getName(), TypeMetaFieldDef.getName(), TypeNameMetaFieldDef.getName());
}
