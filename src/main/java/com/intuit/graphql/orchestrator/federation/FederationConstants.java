package com.intuit.graphql.orchestrator.federation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FederationConstants {

  public static final Set<String> FED_TYPE_DIRECTIVES_NAMES_SET =
      new HashSet<>(Arrays.asList("key", "extends"));
  public static final Set<String> FED_FIELD_DIRECTIVE_NAMES_SET =
      new HashSet<>(Arrays.asList("external", "requires", "provides"));

}
