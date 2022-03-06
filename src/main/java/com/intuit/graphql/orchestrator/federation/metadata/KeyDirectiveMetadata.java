package com.intuit.graphql.orchestrator.federation.metadata;

import static com.intuit.graphql.orchestrator.federation.FieldSetUtils.toFieldSet;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.ValueWithVariable;
import graphql.language.Field;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class KeyDirectiveMetadata {

  private final Set<Field> fieldSet;

  public static KeyDirectiveMetadata from(Directive directive) {
    Optional<Argument> optionalArgument = directive.getArguments().stream().findFirst();
    if (!optionalArgument.isPresent()) {
      // validation is already being done, this should not happen
      throw new IllegalStateException("key directive argument not found.");
    }
    Argument argument = optionalArgument.get();
    ValueWithVariable valueWithVariable = argument.getValueWithVariable();
    String fieldSetValue = valueWithVariable.getStringValue();
    return new KeyDirectiveMetadata(toFieldSet(fieldSetValue));
  }

}
