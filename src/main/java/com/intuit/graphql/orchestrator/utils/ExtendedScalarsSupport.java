package com.intuit.graphql.orchestrator.utils;

import graphql.Scalars;
import graphql.schema.GraphQLScalarType;
import java.util.ArrayList;
import java.util.List;

public class ExtendedScalarsSupport {

  public static final List<GraphQLScalarType> GRAPHQL_EXTENDED_SCALARS = new ArrayList<>();

  static {
    GRAPHQL_EXTENDED_SCALARS.add(Scalars.GraphQLBigDecimal);
    GRAPHQL_EXTENDED_SCALARS.add(Scalars.GraphQLBigInteger);
    GRAPHQL_EXTENDED_SCALARS.add(Scalars.GraphQLByte);
    GRAPHQL_EXTENDED_SCALARS.add(Scalars.GraphQLChar);
    GRAPHQL_EXTENDED_SCALARS.add(Scalars.GraphQLShort);
    GRAPHQL_EXTENDED_SCALARS.add(Scalars.GraphQLLong);
  }

}
