package com.intuit.graphql.orchestrator.deferDirective;

import graphql.GraphQLContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.language.Argument;
import graphql.language.BooleanValue;
import graphql.language.Directive;
import graphql.schema.DataFetcher;
import graphql.schema.StaticDataFetcher;

import java.util.List;

import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_IF_ARG;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.USE_DEFER;

public class DeferDirectiveInstrumentation extends SimpleInstrumentation {

    @Override
    public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters) {
        GraphQLContext gqlContext = parameters.getEnvironment().getContext();
        boolean useDefer = gqlContext != null && gqlContext.getOrDefault(USE_DEFER, false);

        if(useDefer && !parameters.getEnvironment().getField().getDirectives(DEFER_DIRECTIVE_NAME).isEmpty()) {
            List<Directive> deferDirs = parameters.getEnvironment().getField().getDirectives(DEFER_DIRECTIVE_NAME);
            Argument ifArgument = deferDirs.get(0).getArgument(DEFER_IF_ARG);

            //Only return null for defer fields if it is enabled otherwise remove from query
            if(ifArgument == null || ((BooleanValue) ifArgument.getValue()).isValue()) {
                return new StaticDataFetcher(null);
            }
        }
        return dataFetcher;
    }
}