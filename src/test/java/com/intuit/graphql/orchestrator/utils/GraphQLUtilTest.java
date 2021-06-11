package com.intuit.graphql.orchestrator.utils;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.junit.Test;

public class GraphQLUtilTest {

  @Test
  public void canCreateTypeFromGraphQLObjectType() {
    GraphQLObjectType graphQLObjectType = GraphQLObjectType.newObject().name("object").build();
    Type type = GraphQLUtil.createTypeBasedOnGraphQLType(graphQLObjectType);
    assertThat(type).isInstanceOf(TypeName.class);
    assertThat(((TypeName) type).getName()).isEqualTo("object");
  }

  @Test
  public void canCreateTypeFromNonNullType() {
    GraphQLObjectType graphQLObjectType = GraphQLObjectType.newObject().name("object").build();
    GraphQLNonNull nonNull = GraphQLNonNull.nonNull(graphQLObjectType);

    Type type = GraphQLUtil.createTypeBasedOnGraphQLType(nonNull);
    assertThat(type).isInstanceOf(NonNullType.class);

    type = ((NonNullType) type).getType();
    assertThat(((TypeName) type).getName()).isEqualTo("object");
  }

  @Test
  public void canCreateTypeFromListType() {
    GraphQLObjectType graphQLObjectType = GraphQLObjectType.newObject().name("object").build();
    GraphQLNonNull nonNull = GraphQLNonNull.nonNull(graphQLObjectType);
    GraphQLList graphQLList = GraphQLList.list(nonNull);

    Type type = GraphQLUtil.createTypeBasedOnGraphQLType(graphQLList);
    assertThat(type).isInstanceOf(ListType.class);

    type = ((ListType) type).getType();
    assertThat(type).isInstanceOf(NonNullType.class);

    type = ((NonNullType) type).getType();
    assertThat(((TypeName) type).getName()).isEqualTo("object");
  }
}