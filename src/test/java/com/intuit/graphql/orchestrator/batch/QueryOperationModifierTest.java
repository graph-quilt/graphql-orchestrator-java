package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.TestHelper.fragmentDefinitions;
import static com.intuit.graphql.orchestrator.TestHelper.query;
import static com.intuit.graphql.orchestrator.TestHelper.schema;
import static org.assertj.core.api.Assertions.assertThat;

import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class QueryOperationModifierTest {

  private String schema = "type Query {\n"
      + "  foo: Foo\n"
      + "  complexFoo: [Foo!]!\n"
      + "  fooUnion: Union\n"
      + "}\n"
      + "\n"
      + "interface Foo {\n"
      + "  a: String\n"
      + "}\n"
      + "\n"
      + "type Bar implements Foo {\n"
      + "  a: String\n"
      + "  b: String\n"
      + "}\n"
      + "\n"
      + "union Union = Bar";

  private RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
      .type("Foo", wiring -> wiring.typeResolver(env -> env.getSchema().getObjectType("Bar")))
      .type("Union", wiring -> wiring.typeResolver(env -> env.getSchema().getObjectType("Bar")))
      .build();

  private GraphQLSchema graphQLSchema;

  @Before
  public void setup() {
    graphQLSchema = schema(schema, runtimeWiring);
  }

  @Test
  public void addsTypenameToQuery() {
    final String query = "{\n"
        + "\tfoo {\n"
        + "    a\n"
        + "  }\n"
        + "}";
    final OperationDefinition queryOperation = query(query);

    QueryOperationModifier queryOperationModifier = new QueryOperationModifier();

    final OperationDefinition definition = queryOperationModifier
        .modifyQuery(graphQLSchema, queryOperation, Collections.emptyMap(), Collections.emptyMap());

    final List<String> preOrderResult = GraphQLTestUtil
        .printPreOrder(definition, graphQLSchema, Collections.emptyMap());
    assertThat(preOrderResult).containsExactly("foo", "a", "__typename");
  }

  @Test
  public void addsTypenameToQueryForComplexObjects() {
    final String query = "{\n"
        + "\tcomplexFoo {\n"
        + "    a\n"
        + "  }\n"
        + "}";
    final OperationDefinition queryOperation = query(query);

    QueryOperationModifier queryOperationModifier = new QueryOperationModifier();

    final OperationDefinition definition = queryOperationModifier
        .modifyQuery(graphQLSchema, queryOperation, Collections.emptyMap(), Collections.emptyMap());

    final List<String> preOrderResult = GraphQLTestUtil
        .printPreOrder(definition, graphQLSchema, Collections.emptyMap());
    assertThat(preOrderResult).containsExactly("complexFoo", "a", "__typename");
  }

  @Test
  public void doesNotModifyQueryWithTypename() {
    final String queryWithTypename = "{\n"
        + "\tfoo {\n"
        + "    a\n"
        + "    __typename\n"
        + "  }\n"
        + "}";

    final OperationDefinition queryOperation = query(queryWithTypename);

    QueryOperationModifier queryOperationModifier = new QueryOperationModifier();

    final OperationDefinition definition = queryOperationModifier
        .modifyQuery(graphQLSchema, queryOperation, Collections.emptyMap(), Collections.emptyMap());

    final List<String> preOrderResult = GraphQLTestUtil
        .printPreOrder(definition, graphQLSchema, Collections.emptyMap());
    assertThat(preOrderResult).containsExactly("foo", "a", "__typename");
  }

  @Test
  public void addsTypenameToUnion() {
    final String queryWithUnion = "{ fooUnion { ...on Bar { b } } }";

    final OperationDefinition queryOperation = query(queryWithUnion);

    final QueryOperationModifier queryOperationModifier = new QueryOperationModifier();

    final OperationDefinition definition = queryOperationModifier
        .modifyQuery(graphQLSchema, queryOperation, Collections.emptyMap(), Collections.emptyMap());

    final List<String> preOrderResult = GraphQLTestUtil
        .printPreOrder(definition, graphQLSchema, Collections.emptyMap());

    assertThat(preOrderResult).containsExactly("fooUnion", "inline:b", "inline:__typename");
  }

  @Test
  @Ignore("to be done later")
  public void addsTypenameToQueryInFragmentDefinition() {
    final String queryWithFragment = "query {\n"
        + "  foo {\n"
        + "    ...FooFragment\n"
        + "  }\n"
        + "}\n"
        + "\n"
        + "fragment FooFragment on Foo {\n"
        + " a \n"
        + "}";

    final OperationDefinition queryOperation = query(queryWithFragment);
    final Map<String, FragmentDefinition> fragmentsByName = fragmentDefinitions(queryWithFragment);

    final QueryOperationModifier queryOperationModifier = new QueryOperationModifier();

    final OperationDefinition definition = queryOperationModifier
        .modifyQuery(graphQLSchema, queryOperation, fragmentsByName, Collections.emptyMap());
  }
}
