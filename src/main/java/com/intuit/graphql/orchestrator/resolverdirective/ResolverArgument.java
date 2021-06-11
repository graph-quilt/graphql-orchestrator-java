package com.intuit.graphql.orchestrator.resolverdirective;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ResolverArgument {

  private String name;
  private String value;

  public ResolverArgument(String name, String value) {
    this.name = name;
    this.value = value;
  }
}
