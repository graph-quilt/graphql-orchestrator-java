package com.intuit.graphql.orchestrator.resolverdirective;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.Scalars;
import graphql.language.AstPrinter;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import org.junit.Test;

public class ResolverArgumentQueryBuilderTest {

  @Test
  public void buildsQueryWithScalarType() {
    GraphQLInputType graphQLInputType = Scalars.GraphQLInt;

    String queryRoot = "consumer.finance.tax";

    final String result = AstPrinter.printAstCompact(new ResolverArgumentQueryBuilder()
        .buildQuery(queryRoot, graphQLInputType));

    assertThat(result).isEqualTo("query {consumer {finance {tax}}}");
  }

  @Test
  public void buildsQueryWithNestedObjectType() {
    GraphQLInputObjectType nestedType = GraphQLInputObjectType
        .newInputObject()
        .name("Test_Nested_Input_Type")
        .field(nestedFieldBuilder -> nestedFieldBuilder.name("test_nested_field_1").type(Scalars.GraphQLInt)).build();

    GraphQLInputObjectType inputType = GraphQLInputObjectType.newInputObject()
        .name("Test_Input_Type")
        .field(builder -> builder.name("test_field_1").type(Scalars.GraphQLString))
        .field(builder ->
            builder
                .name("test_field_2")
                .type(nestedType)
        )
        .build();

    String queryRoot = "consumer.finance.tax";

    final String result = AstPrinter
        .printAstCompact(new ResolverArgumentQueryBuilder().buildQuery(queryRoot, inputType));

    assertThat(result).isEqualTo("query {consumer {finance {tax {test_field_1 test_field_2 {test_nested_field_1}}}}}");
  }

  @Test(expected = NotAValidFieldReference.class)
  public void invalidResolverFields() {
    GraphQLInputType type = Scalars.GraphQLInt;

    String invalidRoot = "{-13,test_not_valid{{2}";

    new ResolverArgumentQueryBuilder().buildQuery(invalidRoot, type);
  }
}