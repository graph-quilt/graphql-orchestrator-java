package com.intuit.graphql.orchestrator.federation;

import graphql.ErrorType;
import graphql.GraphqlErrorException;

public class EntityFetchingException extends GraphqlErrorException {

  private EntityFetchingException(EntityFetchingException.Builder builder) {
    super(builder);
  }

  @Override
  public ErrorType getErrorType() {
    return ErrorType.DataFetchingException;
  }

  public static EntityFetchingException.Builder builder() {
    return new EntityFetchingException.Builder();
  }

  public static class Builder extends BuilderBase<EntityFetchingException.Builder, EntityFetchingException> {

    private static final String ERROR_MESSAGE = "Failed to execute entity data fetcher. "
        + " fieldName=%s, "
        + " parentTypeName=%s, "
        + " serviceNameSpace=%s";

    private String fieldName;
    private String parentTypeName;
    private String serviceNameSpace;

    public EntityFetchingException.Builder fieldName(String fieldName) {
      this.fieldName = fieldName;
      return this;
    }

    public EntityFetchingException.Builder parentTypeName(String parentTypeName) {
      this.parentTypeName = parentTypeName;
      return this;
    }

    public EntityFetchingException.Builder serviceNameSpace(String serviceNameSpace) {
      this.serviceNameSpace = serviceNameSpace;
      return this;
    }

    public EntityFetchingException build() {
      this.message = String.format(ERROR_MESSAGE, fieldName, parentTypeName,
          serviceNameSpace);
      this.errorClassification = ErrorType.ExecutionAborted;
      return new EntityFetchingException(this);
    }
  }
}
