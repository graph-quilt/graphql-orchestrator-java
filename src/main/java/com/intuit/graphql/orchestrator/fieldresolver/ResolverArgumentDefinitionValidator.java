package com.intuit.graphql.orchestrator.fieldresolver;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isObjectType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isPrimitiveType;
import static com.intuit.graphql.utils.XtextTypeUtils.unwrapAll;

import com.intuit.graphql.graphQL.InputValueDefinition;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.stitching.StitchingException;
import graphql.language.AstValueHelper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ResolverArgumentDefinitionValidator {

  private static final String INVALID_RESOLVER_ARGUMENT_VALUE = "Invalid resolver argument value: %s";
  private static final String UNSUPPORTED_NAMEDTYPE = "Unsupported NamedType: %s";

  private final ResolverArgumentDefinition resolverArgumentDefinition;
  private final InputValueDefinition inputValueDefinition;
  private final FieldResolverContext fieldResolverContext;

  public void validate() {
    NamedType resolverArgumentType = unwrapAll(inputValueDefinition.getNamedType());
    String resolverArgumentValue = resolverArgumentDefinition.getValue();
    if (isPrimitiveType(resolverArgumentType) || isObjectType(resolverArgumentType)) {
      if (!isVariableReference(resolverArgumentValue)) {
        try {
          AstValueHelper.valueFromAst(resolverArgumentValue);
        } catch(graphql.AssertException e) {
          throw new StitchingException(String.format(INVALID_RESOLVER_ARGUMENT_VALUE, resolverArgumentDefinition), e);
        }
      }

    } else {
      // this will only happen if spec for NamedType has been updated
      throw new StitchingException(String.format(UNSUPPORTED_NAMEDTYPE, resolverArgumentType.getClass().getName()));
    }
  }

  private boolean isVariableReference(String resolverArgumentValue) {
    return resolverArgumentValue.startsWith("$");
  }

}
