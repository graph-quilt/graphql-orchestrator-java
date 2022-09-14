package com.intuit.graphql.orchestrator.fieldresolver;

import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveException;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import lombok.Getter;

@Getter
public class FieldResolverArgumentException extends FieldResolverException {

    private final FieldResolverContext fieldResolverContext;

    public FieldResolverArgumentException(String message,
                                          ResolverArgumentDefinition resolverArgumentDefinition,
                                          FieldResolverContext fieldResolverContext) {

        super(formatMessage(message, resolverArgumentDefinition), fieldResolverContext);
        this.fieldResolverContext = fieldResolverContext;
    }

    private static String formatMessage(String originalMessage, ResolverArgumentDefinition resolverArgumentDefinition) {
        String errorMessage = originalMessage + "  resolverArgumentName=%s, resolverArgumentValue=%s";

        return String.format(errorMessage, resolverArgumentDefinition.getName(), resolverArgumentDefinition.getNamedType());
    }

}
