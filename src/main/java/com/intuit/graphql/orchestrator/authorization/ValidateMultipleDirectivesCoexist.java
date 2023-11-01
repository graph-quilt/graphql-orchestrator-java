package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.orchestrator.stitching.InvalidDirectivePairingException;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ValidateMultipleDirectivesCoexist {

        public ValidateMultipleDirectivesCoexist() {
        }

        public void validate(List<Directive> directives) {
            List<String> directiveNames = directives.stream()
                    .map(d -> d.getDefinition().getName())
                    .collect(Collectors.toList());

            if (CollectionUtils.containsAll(directiveNames, Arrays.asList("resolver", "external"))) {
                throw new InvalidDirectivePairingException(Arrays.asList("resolver", "external"));
            }

            if (CollectionUtils.containsAll(directiveNames, Arrays.asList("resolver", "provides"))) {
                throw new InvalidDirectivePairingException(Arrays.asList("resolver", "external"));
            }

            if (CollectionUtils.containsAll(directiveNames, Arrays.asList("resolver", "requires"))) {
                throw new InvalidDirectivePairingException(Arrays.asList("resolver", "external"));
            }

        }

}
