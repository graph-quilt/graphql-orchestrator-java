package com.intuit.graphql.orchestrator.fieldresolver;

import com.intuit.graphql.graphQL.InputValueDefinition;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentNotAFieldOfParentException;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.stitching.StitchingException;
import lombok.AllArgsConstructor;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.isReferenceToFieldInParentType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isObjectType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isPrimitiveType;
import static com.intuit.graphql.utils.XtextTypeUtils.unwrapAll;

@AllArgsConstructor
public class ResolverArgumentDefinitionValidator {

  private final ResolverArgumentDefinition resolverArgumentDefinition;
  private final InputValueDefinition inputValueDefinition;
  private final FieldResolverContext fieldResolverContext;

  public void validate() {
    NamedType resolverArgumentType = unwrapAll(inputValueDefinition.getNamedType());
    String resolverArgumentValue = resolverArgumentDefinition.getValue();
    if (isPrimitiveType(resolverArgumentType)) {
      if (isLiteralAVariableReference(resolverArgumentValue)) {
        validateResolverArgumentsAreFieldsOfParent();
      }
      // else could be int, boolean string literal which is fine
    } else if (isObjectType(resolverArgumentType)) {
      if (isLiteralAVariableReference(resolverArgumentValue)) {
        validateResolverArgumentsAreFieldsOfParent();
      } else if (!isLiteralAnObject(resolverArgumentValue)) {
        String errorMessage = "Unsupported resolver argument value.";
        throw new FieldResolverArgumentException(
            errorMessage, resolverArgumentDefinition, fieldResolverContext);
      }
    } else {
      // this will only happen if graphql spec has been updated and using old graphql implementation
      String errorMessage = "Unsupported NamedType. " + resolverArgumentType.getClass().getName();
      throw new StitchingException(errorMessage);
    }
  }

  private boolean isLiteralAVariableReference(String resolverArgumentValue) {
    return resolverArgumentValue.startsWith("$");
  }

  private boolean isLiteralAnObject(String resolverArgumentValue) {
    return resolverArgumentValue.startsWith("{") && resolverArgumentValue.endsWith("}");
  }

  private void validateResolverArgumentsAreFieldsOfParent() {
    TypeDefinition parentTypeDefinition = fieldResolverContext.getParentTypeDefinition();

    if (!isReferenceToFieldInParentType(resolverArgumentDefinition.getValue(), parentTypeDefinition)) {
      String fieldreference = resolverArgumentDefinition.getValue();
      throw new ResolverArgumentNotAFieldOfParentException(fieldreference, parentTypeDefinition.getName());
    }
  }
}
