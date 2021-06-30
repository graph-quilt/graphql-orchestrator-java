package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.ObjectFieldWithVariable;
import com.intuit.graphql.graphQL.ValueWithVariable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.getResolverDirectiveParentTypeName;
import static java.util.stream.Collectors.toMap;

/**
 * Class to represent @resolver directive definition
 */
@Getter
@AllArgsConstructor
@ToString
public class ResolverDirectiveDefinition {

  private static final String DIRECTIVE_ARG_FIELD = "field";
  private static final String DIRECTIVE_ARG_ARGUMENT = "arguments";

  private final String field; // named after the schema definition
  private final List<ResolverArgumentDefinition> arguments; // named after the schema definition

  /**
   * Creates an instance of this class based on the given {@link Directive}
   *
   * @param directive the resolver directive
   * @return an instance of this class
   */
  public static ResolverDirectiveDefinition from(Directive directive) {
    // The directive definition is @resolver(field: String!, arguments: [ResolverArgument!])
    // it is guaranteed that field will not be null and entry in arguments will not be null
    Objects.requireNonNull(directive, "directive is null for ResolverDirectiveDefinition.from()");
    String resolverFieldName = null;
    List<ResolverArgumentDefinition> resolverArgumentDefinitions = new ArrayList<>();
    for (Argument argument : directive.getArguments()) {
      switch (argument.getName()) {
        case DIRECTIVE_ARG_FIELD:
          resolverFieldName = extractFieldValue(argument);
          break;
        case DIRECTIVE_ARG_ARGUMENT:
          resolverArgumentDefinitions.addAll(extractArgumentsValue(argument));
          break;
        default:
          throw new ResolverDirectiveException(String.format("'%s' argument is unexpected for resolver directive."
              + "  parentType=%s", argument.getName(), getResolverDirectiveParentTypeName(directive)));
      }
    }

    if (StringUtils.isEmpty(resolverFieldName)) {
      throw new ResolverDirectiveException(String.format("@resolver field name cannot be empty.  parentType=%s",
          getResolverDirectiveParentTypeName(directive)));
    }

    return new ResolverDirectiveDefinition(resolverFieldName, resolverArgumentDefinitions);
  }

  /**
   *
   * @param field the field part of resolver directive
   * @return value of the directive argument 'field'
   */
  private static String extractFieldValue(Argument field) {
    return StringUtils.remove(field.getValueWithVariable().getStringValue(), '"');
  }

  /**
   *
   * @param directiveArguments the arguments part of resolver directive
   * @return list of name-value mapping for directive argument 'arguments'
   */
  private static List<ResolverArgumentDefinition> extractArgumentsValue(Argument directiveArguments) {
    return directiveArguments
        .getValueWithVariable().getArrayValueWithVariable().getValueWithVariable().stream()
        .map(ValueWithVariable::getObjectValueWithVariable)
        .map(objectValueWithVariable -> objectValueWithVariable.getObjectFieldWithVariable()
            .stream().collect(toMap(ObjectFieldWithVariable::getName,
                ObjectFieldWithVariable::getValueWithVariable)))
        .map(m -> new ResolverArgumentDefinition(
                cleanStringLiteralValue(m.get("name").getStringValue()),
                cleanStringLiteralValue(m.get("value").getStringValue())
            )
        )
        .collect(Collectors.toList());
  }

  private static String cleanStringLiteralValue(String name) {
    String s = StringUtils.removeStart(name, "\"");
    return StringUtils.removeEnd(s, "\"");
  }
}