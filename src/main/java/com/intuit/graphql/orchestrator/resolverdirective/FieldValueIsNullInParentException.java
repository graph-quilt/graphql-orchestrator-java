package com.intuit.graphql.orchestrator.resolverdirective;

import graphql.ErrorType;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetchingEnvironment;

/**
 * A class thrown when referring to a parent's resolved value but, though the field is present,
 * the value for the field is null.
 *
 * Resolved value is stored in {@link DataFetchingEnvironment#getSource()}.
 */
public class FieldValueIsNullInParentException extends GraphqlErrorException {

  private static final String ERROR_MESSAGE = "Field value not found in parent's resolved value. "
      + " fieldName=%s, "
      + " parentTypeName=%s, "
      + " resolverDirectiveDefinition=%s,"
      + " serviceNameSpace=%s";

  private FieldValueIsNullInParentException(FieldValueIsNullInParentException.Builder builder) {
    super(builder);
  }

  @Override
  public ErrorType getErrorType() {
    return ErrorType.ExecutionAborted;
  }

  public static FieldValueIsNullInParentException.Builder builder() {
    return new FieldValueIsNullInParentException.Builder();
  }

  public static class Builder extends BuilderBase<FieldValueIsNullInParentException.Builder, FieldValueIsNullInParentException> {

    private String fieldName;
    private String parentTypeName;
    private ResolverDirectiveDefinition resolverDirectiveDefinition;
    private String serviceNameSpace;

    public FieldValueIsNullInParentException.Builder fieldName(String fieldName) {
      this.fieldName = fieldName;
      return this;
    }

    public FieldValueIsNullInParentException.Builder parentTypeName(String parentTypeName) {
      this.parentTypeName = parentTypeName;
      return this;
    }

    public FieldValueIsNullInParentException.Builder resolverDirectiveDefinition(ResolverDirectiveDefinition resolverDirectiveDefinition) {
      this.resolverDirectiveDefinition = resolverDirectiveDefinition;
      return this;
    }

    public FieldValueIsNullInParentException.Builder serviceNameSpace(String serviceNameSpace) {
      this.serviceNameSpace = serviceNameSpace;
      return this;
    }

    public FieldValueIsNullInParentException build() {
      this.message = String.format(ERROR_MESSAGE, fieldName, parentTypeName,
          resolverDirectiveDefinition, serviceNameSpace);
      this.errorClassification = ErrorType.ExecutionAborted;
      return new FieldValueIsNullInParentException(this);
    }
  }

}
