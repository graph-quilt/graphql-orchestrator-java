package com.intuit.graphql.orchestrator.schema.fold;

import java.util.Collection;

public interface Foldable<T, U> {

  U fold(U initVal, Collection<T> list);

}
