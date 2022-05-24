package com.intuit.graphql.orchestrator.fieldresolver;

import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import graphql.ErrorType;
import graphql.GraphqlErrorException;

/**
 * A class thrown when an error occured during field resolver graphql execution
 */
public class FieldResolverGraphQLError extends GraphqlErrorException {

  private FieldResolverGraphQLError(FieldResolverGraphQLError.Builder builder) {
    super(builder);
  }

  @Override
  public ErrorType getErrorType() {
    return ErrorType.ExecutionAborted;
  }

  public static FieldResolverGraphQLError.Builder builder() {
    return new FieldResolverGraphQLError.Builder();
  }

  public static class Builder extends BuilderBase<FieldResolverGraphQLError.Builder, FieldResolverGraphQLError> {

    private String errorMessage;

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

    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public FieldResolverGraphQLError build() {
      String msgTemplate = "%s. "
          + " fieldName=%s, "
          + " parentTypeName=%s, "
          + " resolverDirectiveDefinition=%s,"
          + " serviceNameSpace=%s";

      this.message = String.format(msgTemplate, errorMessage, fieldName, parentTypeName,
          resolverDirectiveDefinition, serviceNameSpace);
      this.errorClassification = ErrorType.DataFetchingException;
      return new FieldResolverGraphQLError(this);
    }
  }

}
