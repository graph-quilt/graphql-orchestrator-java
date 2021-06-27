package com.intuit.graphql.orchestrator.resolverdirective;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ResolverArgumentDefinition {

  private String name;
  private String value;

  public ResolverArgumentDefinition(String name, String value) {
    this.name = name;
    this.value = value;
  }
}
