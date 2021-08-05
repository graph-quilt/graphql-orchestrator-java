package com.intuit.graphql.orchestrator.batch;

import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

public class FieldResolverBatchResultTransformerTest {

  @Test(expected = IllegalArgumentException.class)
  public void constructorEmptyResolverSelectedFields() {

    FieldResolverContext fieldResolverContext = FieldResolverContext.builder()
        .build();

    new FieldResolverBatchResultTransformer(ArrayUtils.EMPTY_STRING_ARRAY, fieldResolverContext) ;
  }

}
