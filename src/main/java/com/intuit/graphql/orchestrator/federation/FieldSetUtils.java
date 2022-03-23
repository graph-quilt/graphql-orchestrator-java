package com.intuit.graphql.orchestrator.federation;

import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.parser;
import static java.lang.String.join;

import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class FieldSetUtils {

  private FieldSetUtils() {}

  public static Document getParsedFieldSet(String fieldSetRawValue) {
    return parser.parseDocument(join(StringUtils.SPACE, "{",fieldSetRawValue , "}"));
  }

  public static Set<Field> toFieldSet(String fieldSetRawValue) {
    Document document = getParsedFieldSet(fieldSetRawValue);
    OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0);
    return operationDefinition.getSelectionSet().getSelections().stream()
        .filter(selection -> selection instanceof Field) // TODO validate that selections are Fields only
        .map(selection -> (Field) selection)
        .collect(Collectors.toSet());
  }

}
