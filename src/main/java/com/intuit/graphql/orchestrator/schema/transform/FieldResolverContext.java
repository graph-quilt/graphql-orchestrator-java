package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import java.util.Set;
import java.util.function.Consumer;
import lombok.Getter;

/**
 * This class holds data about a fieldDefinition with resolver directive.
 *
 * If a parent type has two child fields and both has resolver directive, then each child field
 * shall have a corresponding instance of this class.
 */
@Getter
public class FieldResolverContext {

  private final FieldDefinition fieldDefinition;
  private final TypeDefinition parentTypeDefinition;
  private final boolean requiresTypeNameInjection;
  private final ResolverDirectiveDefinition resolverDirectiveDefinition;
  private final String serviceNamespace;
  private final Set<String> requiredFields;

  private final FieldContext targetFieldContext;
  private final FieldDefinition targetFieldDefinition;

  public FieldResolverContext(Builder builder) {
    this.fieldDefinition = builder.fieldDefinition;
    this.parentTypeDefinition = builder.parentTypeDefinition;
    this.requiresTypeNameInjection = builder.requiresTypeNameInjection;
    this.resolverDirectiveDefinition = builder.resolverDirectiveDefinition;
    this.serviceNamespace = builder.serviceNamespace;
    this.targetFieldContext = builder.targetFieldContext;
    this.targetFieldDefinition = builder.targetFieldDefinition;
    this.requiredFields = builder.requiredFields;
  }

  public String getFieldName() {
    return fieldDefinition.getName();
  }

  public String getParentTypename() {
    return parentTypeDefinition.getName();
  }

  public FieldResolverContext transform(Consumer<FieldResolverContext.Builder> consumer) {
    FieldResolverContext.Builder builder = new FieldResolverContext.Builder(this);
    consumer.accept(builder);
    return builder.build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private FieldDefinition fieldDefinition;
    private TypeDefinition parentTypeDefinition;
    private boolean requiresTypeNameInjection;
    private ResolverDirectiveDefinition resolverDirectiveDefinition;
    private String serviceNamespace;

    private FieldContext targetFieldContext;
    private FieldDefinition targetFieldDefinition;
    private Set<String> requiredFields;

    public Builder() {
    }

    public Builder(FieldResolverContext copy) {
      this.fieldDefinition = copy.getFieldDefinition();
      this.parentTypeDefinition = copy.getParentTypeDefinition();
      this.requiresTypeNameInjection = copy.isRequiresTypeNameInjection();
      this.resolverDirectiveDefinition = copy.getResolverDirectiveDefinition();
      this.serviceNamespace = copy.getServiceNamespace();
      this.targetFieldContext = copy.getTargetFieldContext();
      this.targetFieldDefinition = copy.getTargetFieldDefinition();
      this.requiredFields = copy.getRequiredFields();
    }

    public FieldResolverContext.Builder fieldDefinition(FieldDefinition fieldDefinition) {
      this.fieldDefinition = fieldDefinition;
      return this;
    }

    public FieldResolverContext.Builder parentTypeDefinition(TypeDefinition parentTypeDefinition) {
      this.parentTypeDefinition = parentTypeDefinition;
      return this;
    }

    public FieldResolverContext.Builder requiresTypeNameInjection(boolean requiresTypeNameInjection) {
      this.requiresTypeNameInjection = requiresTypeNameInjection;
      return this;
    }

    public FieldResolverContext.Builder resolverDirectiveDefinition(ResolverDirectiveDefinition resolverDirectiveDefinition) {
      this.resolverDirectiveDefinition = resolverDirectiveDefinition;
      return this;
    }

    public FieldResolverContext.Builder serviceNamespace(String serviceNamespace) {
      this.serviceNamespace = serviceNamespace;
      return this;
    }

    public FieldResolverContext.Builder targetFieldContext(FieldContext targetFieldContext) {
      this.targetFieldContext = targetFieldContext;
      return this;
    }

    public FieldResolverContext.Builder targetFieldDefinition(FieldDefinition targetFieldDefinition) {
      this.targetFieldDefinition = targetFieldDefinition;
      return this;
    }

    public FieldResolverContext.Builder requiredFields(Set<String> requiredFields) {
      this.requiredFields = requiredFields;
      return this;
    }

    public FieldResolverContext build() {
      return new FieldResolverContext(this);
    }

  }

}
