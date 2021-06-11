package com.intuit.graphql.orchestrator.schema.transform;

public interface Transformer<S, T> {

  T transform(S source);

}
