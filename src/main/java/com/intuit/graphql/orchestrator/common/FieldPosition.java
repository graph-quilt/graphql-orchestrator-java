package com.intuit.graphql.orchestrator.common;

import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class FieldPosition {

    @EqualsAndHashCode.Include
    private final String parentTypename;

    @EqualsAndHashCode.Include
    private final String fieldName;

    public FieldPosition(String parentTypename, String fieldName) {
        Objects.requireNonNull(parentTypename);
        Objects.requireNonNull(fieldName);
        this.parentTypename = parentTypename;
        this.fieldName = fieldName;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", parentTypename, fieldName);
    }

}
