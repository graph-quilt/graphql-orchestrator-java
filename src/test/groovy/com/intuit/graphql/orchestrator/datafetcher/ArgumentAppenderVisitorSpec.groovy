package com.intuit.graphql.orchestrator.datafetcher

import com.intuit.graphql.orchestrator.TestHelper
import graphql.analysis.QueryTransformer
import graphql.language.Argument
import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import helpers.BaseIntegrationTestSpecification

import static com.intuit.graphql.orchestrator.TestHelper.document

class ArgumentAppenderVisitorSpec extends BaseIntegrationTestSpecification {

    private RuntimeWiring schemaRuntimeWiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", { builder -> builder.defaultDataFetcher({ env -> null }) })
            .build()

    private RuntimeWiring nestedSchemaRuntimeWiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", { builder -> builder.defaultDataFetcher({ env -> null }) })
            .type("NestedType", { builder -> builder.defaultDataFetcher({ env -> null }) })
            .build()

    void addsArgumentToField() {
        given:
        final String schema = "schema { query: Query } type Query { needs_arguments: Int }"
        GraphQLSchema graphQLSchema = TestHelper.schema(schema, schemaRuntimeWiring)

        Document query = document("{ needs_arguments }")

        QueryTransformer queryTransformer = QueryTransformer.newQueryTransformer()
                .fragmentsByName(Collections.emptyMap())
                .root(query)
                .schema(graphQLSchema)
                .variables(Collections.emptyMap())
                .rootParentType(graphQLSchema.getQueryType())
                .build()

        Argument mockArgument = Mock(Argument.class)

        when:
        final Document result = (Document) queryTransformer
                .transform(new ArgumentAppenderVisitor("Query", "needs_arguments", Collections.singletonList(mockArgument)))

        then:
        !result.getDefinitionsOfType(OperationDefinition.class)
                .get(0).getSelectionSet().getSelectionsOfType(Field.class)
                .get(0).collect{ it -> it.getArguments() }
                .isEmpty()
    }

    void addsArgumentToNestedField() {
        given:
        final String nestedSchema = "schema { query: Query } type Query { root : NestedType } type NestedType { needs_arguments: Int }"
        GraphQLSchema graphQLSchema = TestHelper.schema(nestedSchema, nestedSchemaRuntimeWiring)

        Document query = document("{ root { needs_arguments } }")

        QueryTransformer queryTransformer = QueryTransformer.newQueryTransformer()
                .fragmentsByName(Collections.emptyMap())
                .root(query)
                .schema(graphQLSchema)
                .variables(Collections.emptyMap())
                .rootParentType(graphQLSchema.getQueryType())
                .build()

        Argument mockArgument = Mock(Argument.class)

        when:
        final Document result = (Document) queryTransformer
                .transform(new ArgumentAppenderVisitor("NestedType", "needs_arguments", Collections.singletonList(mockArgument)))

        then:
        !result.getDefinitionsOfType(OperationDefinition.class)
                .get(0).getSelectionSet().getSelectionsOfType(Field.class)
                .get(0).getSelectionSet().getSelectionsOfType(Field.class)
                .get(0).collect{ it -> it.getArguments() }
                .isEmpty()
    }
}
