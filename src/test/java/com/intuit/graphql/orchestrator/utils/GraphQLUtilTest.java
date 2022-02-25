package com.intuit.graphql.orchestrator.utils;

import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.getUniqueIdFromFieldSet;
import static org.assertj.core.api.Assertions.assertThat;

import graphql.language.Document;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.OperationDefinition;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.parser.Parser;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

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

  @Test
  public void canCreateUniqueIdFromFieldSetWithoutChildren() {
    String fieldSet = "{ foo bar c1}";
    String id = getUniqueIdFromFieldSet(fieldSet);
    assertThat(id).isEqualTo("barc1foo");
  }

  @Test
  public void canCreateUniqueIdFromFieldSetWithChildren() {
      String fieldSet = "{ foo bar c1 { d1 d2 d3 { e1 e2}}}";
      String id = getUniqueIdFromFieldSet(fieldSet);
      assertThat(id).isEqualTo("barc1food1d2d3e1e2");
  }

  @Test
  public void reorderedFieldSetResultInSameUniqueId() {
    String fieldSet1 = "{ foo bar c1 { d1 d2 d3 { e1 e2 } } }";
    String fieldSet2 = "{ bar foo c1 { d2 d1 d3 { e2 e1 } } }";
    String id = getUniqueIdFromFieldSet(fieldSet1);
    String id2 = getUniqueIdFromFieldSet(fieldSet2);
    assertThat(id).isEqualTo("barc1food1d2d3e1e2");
    assertThat(id2).isEqualTo(id);
  }
}