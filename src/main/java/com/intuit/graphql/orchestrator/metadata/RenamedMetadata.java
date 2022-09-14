package com.intuit.graphql.orchestrator.metadata;

import com.intuit.graphql.orchestrator.ServiceProvider;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class RenamedMetadata {

    private final ServiceProvider serviceProvider;
    Map<String, String> originalTypeNamesByRenamedName;
    Map<String, String> originalFieldNamesByRenamedName;

    public RenamedMetadata(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        this.originalTypeNamesByRenamedName = new HashMap<>();
        this.originalFieldNamesByRenamedName = new HashMap<>();
    }

    public boolean containsRenamedFields() {
        return !this.getOriginalFieldNamesByRenamedName().isEmpty();
    }
}
