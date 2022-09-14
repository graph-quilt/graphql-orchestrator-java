package com.intuit.graphql.orchestrator.federation.validators;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.orchestrator.federation.EntityTypeMerger;
import com.intuit.graphql.orchestrator.federation.exceptions.ExternalFieldNotFoundInBaseException;

import java.util.List;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;

public class ExternalValidator {
    public void validatePostMerge(EntityTypeMerger.EntityMergingContext entityMergingContext, FieldDefinition fieldDefinition) {
        List<String> baseFieldNames = getFieldDefinitions(entityMergingContext.getBaseType()).stream()
                .map(FieldDefinition::getName).collect(Collectors.toList());

        if(!baseFieldNames.contains(fieldDefinition.getName())) {
            throw new ExternalFieldNotFoundInBaseException(fieldDefinition.getName());
        }
    }
}
