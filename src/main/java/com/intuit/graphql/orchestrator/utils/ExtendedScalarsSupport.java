package com.intuit.graphql.orchestrator.utils;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import java.util.ArrayList;
import java.util.List;

public class ExtendedScalarsSupport {

  public static final List<GraphQLScalarType> GRAPHQL_EXTENDED_SCALARS = new ArrayList<>();

  static {
    GRAPHQL_EXTENDED_SCALARS.add(ExtendedScalars.GraphQLBigDecimal);
    GRAPHQL_EXTENDED_SCALARS.add(ExtendedScalars.GraphQLBigInteger);
    GRAPHQL_EXTENDED_SCALARS.add(ExtendedScalars.GraphQLByte);
    GRAPHQL_EXTENDED_SCALARS.add(ExtendedScalars.GraphQLChar);
    GRAPHQL_EXTENDED_SCALARS.add(ExtendedScalars.GraphQLShort);
    GRAPHQL_EXTENDED_SCALARS.add(ExtendedScalars.GraphQLLong);
  }

}
