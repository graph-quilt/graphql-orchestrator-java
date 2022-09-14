package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.graphQL.NamedType;
import lombok.Getter;
import lombok.ToString;

import java.util.function.Consumer;

@Getter
@ToString
public class ResolverArgumentDefinition {

  private final String name;
  private final String value;

  @ToString.Exclude
  private final NamedType namedType;

  public ResolverArgumentDefinition(String name, String value) {
    this(name, value, null);
  }

  public ResolverArgumentDefinition(String name, String value, NamedType namedType) {
    this.name = name;
    this.value = value;
    this.namedType = namedType;
  }

  public ResolverArgumentDefinition transform(Consumer<ResolverArgumentDefinition.Builder> consumer) {
    ResolverArgumentDefinition.Builder builder = new ResolverArgumentDefinition.Builder(this);
    consumer.accept(builder);
    return builder.build();
  }

  public static class Builder {

    private String name;
    private String value;
    private NamedType namedType;

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

    public ResolverArgumentDefinition.Builder namedType(NamedType namedType) {
      this.namedType = namedType;
      return this;
    }

    public ResolverArgumentDefinition build() {
      return new ResolverArgumentDefinition(name, value, namedType);
    }
  }

}
