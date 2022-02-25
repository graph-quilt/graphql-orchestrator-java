package com.intuit.graphql.orchestrator.federation.metadata;

import static java.lang.String.join;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.ValueWithVariable;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.parser.Parser;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
@Getter
public class KeyDirectiveMetadata {

  private static final Parser parser = new Parser();

  private final SelectionSet fieldSet;

  public static KeyDirectiveMetadata from(Directive directive) {
    Optional<Argument> optionalArgument = directive.getArguments().stream().findFirst();
    if (!optionalArgument.isPresent()) {
      // validation is already being done, this should not happen
      throw new IllegalStateException("key directive argument not found.");
    }
    Argument argument = optionalArgument.get();
    ValueWithVariable valueWithVariable = argument.getValueWithVariable();
    String fieldSetValue = valueWithVariable.getStringValue();
    Document document = parser.parseDocument(join(StringUtils.SPACE, "{",fieldSetValue , "}"));
    OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0);
    return new KeyDirectiveMetadata(operationDefinition.getSelectionSet());
  }

  public List<String> getKeyFieldNames() {
    // TODO complex field set
    return fieldSet.getSelections().stream()
        .filter(selection -> selection instanceof Field) // TODO validate that selections are Fields only
        .map(selection -> (Field) selection)
        .map(field -> field.getName())
        .collect(Collectors.toList());
  }
}
