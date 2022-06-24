package com.intuit.graphql.orchestrator.resolverdirective

import graphql.Scalars
import graphql.language.AstPrinter
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import helpers.BaseIntegrationTestSpecification

class ResolverArgumentQueryBuilderSpec extends BaseIntegrationTestSpecification {

    void buildsQueryWithScalarType() {
        given:
        GraphQLInputType graphQLInputType = Scalars.GraphQLInt;

        String queryRoot = "consumer.finance.tax"

        final String result = AstPrinter.printAstCompact(new ResolverArgumentQueryBuilder()
                .buildQuery(queryRoot, graphQLInputType))

        expect:
        result == "query {consumer {finance {tax}}}"
    }

    void buildsQueryWithNestedObjectType() {
        given:
        GraphQLInputObjectType nestedType = GraphQLInputObjectType
                .newInputObject()
                .name("Test_Nested_Input_Type")
                .field({ nestedFieldBuilder -> nestedFieldBuilder.name("test_nested_field_1").type(Scalars.GraphQLInt) }).build()

        GraphQLInputObjectType inputType = GraphQLInputObjectType.newInputObject()
                .name("Test_Input_Type")
                .field({ builder -> builder.name("test_field_1").type(Scalars.GraphQLString) })
                .field({ builder ->
                    builder
                            .name("test_field_2")
                            .type(nestedType)
                }
                )
                .build()

        String queryRoot = "consumer.finance.tax"

        final String result = AstPrinter
                .printAstCompact(new ResolverArgumentQueryBuilder().buildQuery(queryRoot, inputType))

        expect:
        result == "query {consumer {finance {tax {test_field_1 test_field_2 {test_nested_field_1}}}}}"
    }

    void invalidResolverFields() {
        given:
        GraphQLInputType type = Scalars.GraphQLInt

        String invalidRoot = "{-13,test_not_valid{{2}"

        when:
        new ResolverArgumentQueryBuilder().buildQuery(invalidRoot, type)

        then:
        thrown(NotAValidFieldReference)
    }
}
