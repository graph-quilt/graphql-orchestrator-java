package com.intuit.graphql.orchestrator.resolverdirective;

import lombok.Getter;
import lombok.ToString;

import java.util.function.Consumer;

@Getter
@ToString
public class ResolverArgumentDefinition {

  private final String name;
  private final String value;

  public ResolverArgumentDefinition(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public ResolverArgumentDefinition transform(Consumer<ResolverArgumentDefinition.Builder> consumer) {
    ResolverArgumentDefinition.Builder builder = new ResolverArgumentDefinition.Builder(this);
    consumer.accept(builder);
    return builder.build();
  }

  public static class Builder {

    private String name;
    private String value;

    public Builder() {
    }

    public Builder(ResolverArgumentDefinition copy) {
      this.name = copy.getName();
      this.value = copy.getValue();
    }

    public ResolverArgumentDefinition.Builder name(String name) {
      this.name = name;
      return this;
    }

    public ResolverArgumentDefinition.Builder value(String value) {
      this.value = value;
      return this;
    }

    public ResolverArgumentDefinition build() {
      return new ResolverArgumentDefinition(name, value);
    }
  }

}
