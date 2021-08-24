package com.intuit.graphql.orchestrator.authorization;

import lombok.*;

import java.util.concurrent.CompletableFuture;

@Getter
public class AuthorizationContext<ClaimT> {

  private final String clientId;

  private final String claimDataName;

  private final CompletableFuture<ClaimT> futureClaimData;

  private final FieldAuthorization fieldAuthorization;

  public AuthorizationContext(
      String clientId,
      String claimDataName,
      CompletableFuture<ClaimT> futureClaimData,
      FieldAuthorization fieldAuthorization) {
    this.clientId = clientId;
    this.claimDataName = claimDataName;
    this.futureClaimData = futureClaimData;
    this.fieldAuthorization = fieldAuthorization;
  }

  public AuthorizationContext(FieldAuthorization fieldAuthorization) {
    this.clientId = null;
    this.claimDataName = null;
    this.futureClaimData = null;
    this.fieldAuthorization = fieldAuthorization;
  }
}
