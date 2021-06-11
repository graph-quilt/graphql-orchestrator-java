package com.intuit.graphql.orchestrator.schema.fold;

import java.util.Collection;

public interface Foldable<T> {

  T fold(T initVal, Collection<T> list);

}
