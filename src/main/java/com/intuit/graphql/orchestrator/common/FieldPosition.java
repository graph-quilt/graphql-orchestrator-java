package com.intuit.graphql.orchestrator.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public class FieldPosition {

    private final String parentTypename;

    private final String fieldName;

    @JsonCreator
    public FieldPosition(@JsonProperty(value="parentTypename", required = true) String parentTypename,
                         @JsonProperty(value="fieldName", required = true) String fieldName) {
        this.parentTypename = parentTypename;
        this.fieldName = fieldName;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", parentTypename, fieldName);
    }

}
