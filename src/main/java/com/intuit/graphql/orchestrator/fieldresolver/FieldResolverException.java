package com.intuit.graphql.orchestrator.fieldresolver;

import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveException;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import lombok.Getter;

@Getter
public class FieldResolverException extends ResolverDirectiveException {

    private final FieldResolverContext fieldResolverContext;

    public FieldResolverException(String message, FieldResolverContext fieldResolverContext) {
        super(formatMessage(message, fieldResolverContext));
        this.fieldResolverContext = fieldResolverContext;
    }

    private static String formatMessage(String originalMessage, FieldResolverContext fieldResolverContext) {
        String errorMessage = originalMessage + "  fieldName=%s, "
                + " parentTypeName=%s, "
                + " resolverDirectiveDefinition=%s,"
                + " serviceNameSpace=%s";

        return String.format(errorMessage,
                fieldResolverContext.getFieldName(),
                fieldResolverContext.getParentTypename(),
                fieldResolverContext.getResolverDirectiveDefinition(),
                fieldResolverContext.getServiceNamespace());
    }

}
