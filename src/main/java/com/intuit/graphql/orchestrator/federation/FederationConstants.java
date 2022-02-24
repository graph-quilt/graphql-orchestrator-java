package com.intuit.graphql.orchestrator.federation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FederationConstants {

  public static final String FEDERATION_KEY_DIRECTIVE = "key";
  public static final String FEDERATION_EXTERNAL_DIRECTIVE = "external";
  public static final String FEDERATION_EXTENDS_DIRECTIVE = "extends";
  public static final String FEDERATION_REQUIRES_DIRECTIVE = "requires";
  public static final String FEDERATION_PROVIDES_DIRECTIVE = "provides";

  public static final Set<String> FED_TYPE_DIRECTIVES_NAMES_SET =
      new HashSet<>(Arrays.asList("key", "extends"));
  public static final Set<String> FED_FIELD_DIRECTIVE_NAMES_SET =
      new HashSet<>(Arrays.asList("external", "requires", "provides"));

}
