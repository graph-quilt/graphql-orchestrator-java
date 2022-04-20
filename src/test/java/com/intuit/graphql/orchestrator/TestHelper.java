package com.intuit.graphql.orchestrator;

import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.parser;
import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.eclipse.xtext.resource.XtextResourceSet;
import wiremock.com.fasterxml.jackson.databind.ObjectMapper;

public class TestHelper {

  public static final ObjectMapper TEST_MAPPER = new ObjectMapper();

  private static final SchemaGenerator schemaGenerator = new SchemaGenerator();

  private static final SchemaParser schemaParser = new SchemaParser();

  public static Map<String, String> getFileMapFromList(String... fileNames) {
    return Arrays.asList(fileNames)
        .stream().collect(Collectors.toMap(Function.identity(), TestHelper::getResourceAsString));
  }

  public static String getResourceAsString(String fileName) {
    String content = "";
    try {
      content = IOUtils.toString(TestHelper.class.getClassLoader().getResourceAsStream(fileName), Charset.defaultCharset());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return content;
  }

  public static Document document(String query) {
    return parser.parseDocument(query);
  }

  /**
   * parses the string and returns the query operation definition
   *
   * @param query a string that has a query operation
   */
  public static OperationDefinition query(String query) {
    return parse(query, OperationDefinition.class).stream()
        .filter(operationDefinition -> operationDefinition.getOperation().equals(Operation.QUERY))
        .findFirst()
        .orElse(null);
  }

  /**
   * parses the string and returns the mutation operation definition
   *
   * @param mutation a string that has a mutation operation
   */
  public static OperationDefinition mutation(String mutation) {
    return parse(mutation, OperationDefinition.class).stream()
        .filter(operationDefinition -> operationDefinition.getOperation().equals(Operation.MUTATION))
        .findFirst()
        .orElse(null);
  }

  /**
   * parses the string and returns a map of fragment definitions by their name
   *
   * @param query a string that has fragment definitions
   */
  public static Map<String, FragmentDefinition> fragmentDefinitions(String query) {
    return parse(query, FragmentDefinition.class).stream()
        .collect(Collectors.toMap(FragmentDefinition::getName, Function.identity()));
  }

  private static <T extends Definition> List<T> parse(String query, Class<T> clazz) {
    return parser.parseDocument(query).getDefinitionsOfType(clazz);
  }

  /**
   * parses the string and returns the GraphQLSchema. Defaults the runtime wiring to an empty RuntimeWiring.
   *
   * @param schema a GraphQLSchema as a string
   */
  public static GraphQLSchema schema(String schema) {
    return schema(schema, RuntimeWiring.newRuntimeWiring().build());
  }

  /**
   * parses the string and returns the GraphQLSchema, using the provided runtime wiring as the schema runtime wiring.
   *
   * @param schema        the GraphQLSchema as a string
   * @param runtimeWiring a provided runtime-wiring
   */
  public static GraphQLSchema schema(String schema, RuntimeWiring runtimeWiring) {
    return schemaGenerator.makeExecutableSchema(schemaParser.parse(schema), runtimeWiring);
  }

  public static XtextResourceSet toXtextResourceSet(String string) {
    return XtextResourceSetBuilder.newBuilder().file("foo", string).build();
  }

  public static class DefaultTestServiceProvider implements ServiceProvider {

    @Override
    public String getNameSpace() {
      return "DefaultTestService";
    }

    @Override
    public Map<String, String> sdlFiles() {
      return ImmutableMap.of("schema.graphqls", "type Query { s: String }");
    }

    @Override
    public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
        GraphQLContext context) {
      return CompletableFuture
          .completedFuture(ImmutableMap.of("data", ImmutableMap.of("s", "hello")));
    }
  }

}