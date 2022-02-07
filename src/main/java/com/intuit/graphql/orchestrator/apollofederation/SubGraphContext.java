package com.intuit.graphql.orchestrator.apollofederation;

import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLUnionType;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class SubGraphContext {
  private final GraphQLUnionType entityUnion ;
  private final Map<String, GraphQLObjectType> ownedEntities = new HashMap<>();
  private final Map<String, GraphQLObjectType> extendedEntities = new HashMap<>();
  private final ServiceMetadata serviceMetadata;
}
