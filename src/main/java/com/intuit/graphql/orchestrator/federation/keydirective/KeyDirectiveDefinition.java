package com.intuit.graphql.orchestrator.federation.keydirective;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.getResolverDirectiveParentTypeName;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.orchestrator.federation.keydirective.exceptions.KeyDirectiveException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * Class to represent @key directive definition
 */
@Getter
@AllArgsConstructor
@ToString
public class KeyDirectiveDefinition {

  private static final String DIRECTIVE_ARG_FIELDS = "fields";

  private final String fields; // named after the schema definition

  private final List<String> keyFieldNames;
  /**
   * Creates an instance of this class based on the given {@link Directive}
   *
   * @param directive the key directive
   * @return an instance of this class
   */
  public static KeyDirectiveDefinition from(Directive directive) {
    // The directive definition is @key(fields: String!)
    Objects.requireNonNull(directive, "directive is null for KeyDirectiveDefinition.from()");
    String fieldValue = null;
    List<String> keyFieldNames = new ArrayList<>();
    for (Argument argument : directive.getArguments()) {
      switch (argument.getName()) {
        case DIRECTIVE_ARG_FIELDS:
          fieldValue = extractFieldValue(argument);
          keyFieldNames.addAll(extractFieldValues(fieldValue));
          break;
        default:
          throw new KeyDirectiveException(String.format("'%s' argument is unexpected for key directive."
              + "  parentType=%s", argument.getName(), getResolverDirectiveParentTypeName(directive)));
      }
    }

    if (StringUtils.isEmpty(fieldValue)) {
      throw new KeyDirectiveException(String.format("@key fields name cannot be empty.  parentType=%s",
          getResolverDirectiveParentTypeName(directive)));
    }

    return new KeyDirectiveDefinition(fieldValue, keyFieldNames);
  }

  /**
   *
   * @param field the field part of resolver directive
   * @return value of the directive argument 'fields'
   */
  private static String extractFieldValue(Argument field) {
    return StringUtils.remove(field.getValueWithVariable().getStringValue(), '"');
  }

  private static List<String> extractFieldValues(String fieldValue) {
    return Arrays.asList(fieldValue.split(" "));
  }

  private static String cleanStringLiteralValue(String name) {
    String s = StringUtils.removeStart(name, "\"");
    return StringUtils.removeEnd(s, "\"");
  }

  public List<String> getKeyFieldNames() {
    return this.keyFieldNames;
  }
}