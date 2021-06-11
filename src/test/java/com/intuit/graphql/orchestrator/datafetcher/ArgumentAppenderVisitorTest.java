package com.intuit.graphql.orchestrator.datafetcher;

import static com.intuit.graphql.orchestrator.TestHelper.document;
import static com.intuit.graphql.orchestrator.TestHelper.schema;
import static org.assertj.core.api.Assertions.assertThat;

import graphql.analysis.QueryTransformer;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import java.util.Collections;
import org.junit.Test;

public class ArgumentAppenderVisitorTest {

  private RuntimeWiring schemaRuntimeWiring = RuntimeWiring.newRuntimeWiring()
      .type("Query", builder -> builder.defaultDataFetcher(env -> null))
      .build();

  private RuntimeWiring nestedSchemaRuntimeWiring = RuntimeWiring.newRuntimeWiring()
      .type("Query", builder -> builder.defaultDataFetcher(env -> null))
      .type("NestedType", builder -> builder.defaultDataFetcher(env -> null))
      .build();


  @Test
  public void addsArgumentToField() {
    final String schema = "schema { query: Query } type Query { needs_arguments: Int }";
    GraphQLSchema graphQLSchema = schema(schema, schemaRuntimeWiring);

    Document query = document("{ needs_arguments }");

    QueryTransformer queryTransformer = QueryTransformer.newQueryTransformer()
        .fragmentsByName(Collections.emptyMap())
        .root(query)
        .schema(graphQLSchema)
        .variables(Collections.emptyMap())
        .rootParentType(graphQLSchema.getQueryType())
        .build();

    final Document result = (Document) queryTransformer
        .transform(new ArgumentAppenderVisitor("Query", "needs_arguments", Collections.singletonList(null)));
    assertThat(
        result.getDefinitionsOfType(OperationDefinition.class).get(0).getSelectionSet().getSelectionsOfType(Field.class)
            .get(0))
        .extracting(Field::getArguments)
        .asList()
        .isNotEmpty();

  }

  @Test
  public void addsArgumentToNestedField() {
    final String nestedSchema = "schema { query: Query } type Query { root : NestedType } type NestedType { needs_arguments: Int }";
    GraphQLSchema graphQLSchema = schema(nestedSchema, nestedSchemaRuntimeWiring);

    Document query = document("{ root { needs_arguments } }");

    QueryTransformer queryTransformer = QueryTransformer.newQueryTransformer()
        .fragmentsByName(Collections.emptyMap())
        .root(query)
        .schema(graphQLSchema)
        .variables(Collections.emptyMap())
        .rootParentType(graphQLSchema.getQueryType())
        .build();

    final Document result = (Document) queryTransformer
        .transform(new ArgumentAppenderVisitor("NestedType", "needs_arguments", Collections.singletonList(null)));

    assertThat(
        result.getDefinitionsOfType(OperationDefinition.class).get(0).getSelectionSet().getSelectionsOfType(Field.class)
            .get(0).getSelectionSet().getSelectionsOfType(Field.class).get(0))
        .extracting(Field::getArguments)
        .asList()
        .isNotEmpty();
  }
}