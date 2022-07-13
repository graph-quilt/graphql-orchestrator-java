package com.intuit.graphql.orchestrator.schema.transform;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;

/**
 * This class holds data about a fieldDefinition with resolver directive.
 *
 * If a parent type has two child fields and both has resolver directive, then each child field
 * shall have a corresponding instance of this class.
 */
@Getter
public class FieldResolverContext {

  private final Map<String, FieldDefinition> parentTypeFields;

  private final FieldDefinition fieldDefinition;
  private final TypeDefinition parentTypeDefinition;
  private final boolean requiresTypeNameInjection;
  private final ResolverDirectiveDefinition resolverDirectiveDefinition;
  private final String serviceNamespace;
  private final Set<String> requiredFields;

  private final FieldContext targetFieldContext;
  private final FieldDefinition targetFieldDefinition;
  private final String targetServiceNamespace;

  public FieldResolverContext(Builder builder) {
    this.fieldDefinition = builder.fieldDefinition;
    this.parentTypeDefinition = builder.parentTypeDefinition;
    this.parentTypeFields = builder.parentTypeFields;
    this.requiresTypeNameInjection = builder.requiresTypeNameInjection;
    this.resolverDirectiveDefinition = builder.resolverDirectiveDefinition;
    this.serviceNamespace = builder.serviceNamespace;
    this.targetFieldContext = builder.targetFieldContext;
    this.targetFieldDefinition = builder.targetFieldDefinition;
    this.requiredFields = builder.requiredFields;
    this.targetServiceNamespace = builder.targetServiceNamespace;
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

    private final Map<String, FieldDefinition> parentTypeFields = new HashMap<>();

    private FieldDefinition fieldDefinition;
    private TypeDefinition parentTypeDefinition;
    private boolean requiresTypeNameInjection;
    private ResolverDirectiveDefinition resolverDirectiveDefinition;
    private String serviceNamespace;

    private FieldContext targetFieldContext;
    private FieldDefinition targetFieldDefinition;
    private String targetServiceNamespace;
    private Set<String> requiredFields;

    public Builder() {
    }

    public Builder(FieldResolverContext sourceObject) {
      this.fieldDefinition = sourceObject.getFieldDefinition();
      this.parentTypeDefinition = sourceObject.getParentTypeDefinition();
      this.parentTypeFields.putAll(sourceObject.parentTypeFields);
      this.requiresTypeNameInjection = sourceObject.isRequiresTypeNameInjection();
      this.resolverDirectiveDefinition = sourceObject.getResolverDirectiveDefinition();
      this.serviceNamespace = sourceObject.getServiceNamespace();
      this.targetFieldContext = sourceObject.getTargetFieldContext();
      this.targetFieldDefinition = sourceObject.getTargetFieldDefinition();
      this.targetServiceNamespace = sourceObject.getTargetServiceNamespace();
      this.requiredFields = sourceObject.getRequiredFields();
    }

    public FieldResolverContext.Builder fieldDefinition(FieldDefinition fieldDefinition) {
      this.fieldDefinition = fieldDefinition;
      return this;
    }

    public FieldResolverContext.Builder parentTypeDefinition(TypeDefinition parentTypeDefinition) {
      this.parentTypeDefinition = parentTypeDefinition;
      parentTypeFields.putAll(
          getFieldDefinitions(parentTypeDefinition).stream()
              .collect(Collectors.toMap(FieldDefinition::getName, Function.identity())));
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

    public FieldResolverContext.Builder targetServiceNamespace(String targetServiceNamespace) {
      this.targetServiceNamespace = targetServiceNamespace;
      return this;
    }

    public FieldResolverContext build() {
      return new FieldResolverContext(this);
    }

  }

}
