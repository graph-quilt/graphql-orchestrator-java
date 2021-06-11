package com.intuit.graphql.orchestrator.resolverdirective;

import graphql.ErrorType;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetchingEnvironment;

/**
 * A class thrown when referring to a parent's resolved value but the field name is not present as key
 *
 * Resolved value is stored in {@link DataFetchingEnvironment#getSource()}.
 */
public class FieldNotFoundInParentException extends GraphqlErrorException {

  private static final String ERROR_MESSAGE = "Field not found in parent's resolved value. "
      + " fieldName=%s, "
      + " parentTypeName=%s, "
      + " resolverDirectiveDefinition=%s,"
      + " serviceNameSpace=%s";

  private FieldNotFoundInParentException(FieldNotFoundInParentException.Builder builder) {
    super(builder);
  }

  @Override
  public ErrorType getErrorType() {
    return ErrorType.ExecutionAborted;
  }

  public static FieldNotFoundInParentException.Builder builder() {
    return new FieldNotFoundInParentException.Builder();
  }

  public static class Builder extends BuilderBase<FieldNotFoundInParentException.Builder, FieldNotFoundInParentException> {

    private String fieldName;
    private String parentTypeName;
    private ResolverDirectiveDefinition resolverDirectiveDefinition;
    private String serviceNameSpace;

    public Builder fieldName(String fieldName) {
      this.fieldName = fieldName;
      return this;
    }

    public Builder parentTypeName(String parentTypeName) {
      this.parentTypeName = parentTypeName;
      return this;
    }

    public Builder resolverDirectiveDefinition(ResolverDirectiveDefinition resolverDirectiveDefinition) {
      this.resolverDirectiveDefinition = resolverDirectiveDefinition;
      return this;
    }

    public Builder serviceNameSpace(String serviceNameSpace) {
      this.serviceNameSpace = serviceNameSpace;
      return this;
    }

    public FieldNotFoundInParentException build() {
      this.message = String.format(ERROR_MESSAGE, fieldName, parentTypeName,
          resolverDirectiveDefinition, serviceNameSpace);
      this.errorClassification = ErrorType.ExecutionAborted;
      return new FieldNotFoundInParentException(this);
    }
  }

}
