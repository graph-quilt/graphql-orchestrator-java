package com.intuit.graphql.orchestrator.utils;

import graphql.Scalars;
import graphql.introspection.Introspection.DirectiveLocation;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

/**
 * Holds creates and maintains DEPRECATED_DIRECTIVE object and Directives specific util functions. The @skip and
 *
 * @include is used in queries and not used in schema parsing, thus, not included here.
 */
class DirectivesUtil {

  static final String NO_LONGER_SUPPORTED = "No longer supported";

  static final GraphQLDirective DEPRECATED_DIRECTIVE;

  private DirectivesUtil() {
  }

  static {
    DEPRECATED_DIRECTIVE = createDeprecatedDirective();
  }

  /**
   * creates GraphQLDirective for @deprecated directive.
   *
   * @return GraphQLDirective object for @deprecated
   */
  private static GraphQLDirective createDeprecatedDirective() {
    GraphQLArgument reasonArgument = GraphQLArgument.newArgument()
            .name("reason")
            .type(Scalars.GraphQLString)
            .defaultValue(NO_LONGER_SUPPORTED)
            .build();

    return GraphQLDirective.newDirective()
            .name("deprecated")
            .argument(reasonArgument)
            .validLocation(DirectiveLocation.FIELD_DEFINITION)
            .validLocation(DirectiveLocation.ENUM_VALUE)
            .build();
  }

  public static String buildDeprecationReason(List<GraphQLDirective> directives) {
    if (CollectionUtils.isNotEmpty(directives)) {

      Optional<GraphQLDirective> directive = directives.stream()
              .filter(d -> "deprecated".equals(d.getName()))
              .findFirst();

      if (directive.isPresent()) {
        Map<String, String> args = directive.get().getArguments().stream()
                .collect(toMap(GraphQLArgument::getName, arg -> (String) arg.getArgumentValue().getValue()));
        if (args.isEmpty()) {
          return NO_LONGER_SUPPORTED; // default value from spec
        } else {
          // pre flight checks have ensured its valid
          return args.get("reason");
        }
      }
    }
    return null;
  }
}
