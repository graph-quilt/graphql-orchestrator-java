package com.intuit.graphql.orchestrator.common;

import com.intuit.graphql.orchestrator.authorization.FieldPath;
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

    private final FieldPath fieldPath;

    public FieldPosition(String parentTypename, String fieldName, FieldPath fieldPath) {
        Objects.requireNonNull(parentTypename);
        Objects.requireNonNull(fieldName);
        Objects.requireNonNull(fieldPath);
        this.parentTypename = parentTypename;
        this.fieldName = fieldName;
        this.fieldPath = fieldPath;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", parentTypename, fieldName);
    }

}
