package com.intuit.graphql.orchestrator.fieldresolver;

import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class ValueTemplate {

    private FieldResolverContext fieldResolverContext;
    private String template;
    private boolean formatStringRef;

    public ValueTemplate(FieldResolverContext fieldResolverContext, String template) {
        this.fieldResolverContext = fieldResolverContext;
        this.template = template;
        this.formatStringRef = StringUtils.containsAny(template, "[", "{");
    }
    public String compile(Map<String, Object> dataSource) {
        String resolverReferenceTemplate1 = "\"$%s\"";
        String resolverReferenceTemplate2 = "$%s";

        //Create as new String to not interfere with resolver reference
        return CollectionUtils.isNotEmpty(fieldResolverContext.getRequiredFields()) ? fieldResolverContext.getRequiredFields().stream().reduce(template, (formattedTemplate, resolverRef) -> {
            String stringValue;
            Object resolverValue = dataSource.get(resolverRef);
            if(resolverValue == null) {
                stringValue = "null";
            } else if(formatStringRef && resolverValue instanceof String) {
                stringValue = StringUtils.join("\"", resolverValue.toString(), "\"");
            } else {
                stringValue = resolverValue.toString();
            }

            String resolverReference1 = String.format(resolverReferenceTemplate1, resolverRef);
            String resolverReference2 = String.format(resolverReferenceTemplate2, resolverRef);

            String replaceQuotedRef = StringUtils.replace(formattedTemplate, resolverReference1, stringValue);
            return StringUtils.replace(replaceQuotedRef, resolverReference2, stringValue);
        }) : template;
    }

}

