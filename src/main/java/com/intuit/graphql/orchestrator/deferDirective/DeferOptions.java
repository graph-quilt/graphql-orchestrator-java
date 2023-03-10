package com.intuit.graphql.orchestrator.deferDirective;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DeferOptions {
    private boolean nestedDefersAllowed;
}
