package com.intuit.graphql.orchestrator;

import static graphql.language.AstPrinter.printAstCompact;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.GraphQLOrchestrator.Builder;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException;
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher;
import com.intuit.graphql.orchestrator.xtext.BooksServiceNoLinkField;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.ExecutionIdProvider;
import graphql.execution.ExecutionStrategy;
import graphql.language.Document;
import java.math.BigDecimal;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class GraphQLOrchestratorTest {

  private BooksService bookService = new BooksService(BOOK_ASSERTS);
  private BooksServiceNoLinkField bookServiceNoLinkField = new BooksServiceNoLinkField(BOOK_ASSERTS);
  private PetsService petsService = new PetsService(PET_ASSERTS);
  private UserRestService userService = new UserRestService(USER_ASSERTS);

  public static final BiConsumer<ExecutionInput, GraphQLContext> USER_ASSERTS = (executionInput, _context) -> {
    String query = printAstCompact((Document) executionInput.getRoot());
    if (query.contains("userFragment")) {
      //TODO: When RestExecutorBatchLoader supports Fragments
      assertThat(executionInput.getQuery()).contains("fragment", "userFragment", "on", "User");
      assertThat(executionInput.getQuery()).doesNotContain("bookFragment");
      assertThat(executionInput.getQuery()).doesNotContain("petFragment");
    }
  };

  public static final BiConsumer<ExecutionInput, GraphQLContext> PET_ASSERTS = (executionInput, _context) -> {
    if (executionInput.getQuery().contains("petFragment")) {
      assertThat(executionInput.getQuery()).contains("fragment petFragment on Pet");
      assertThat(executionInput.getQuery()).doesNotContain("bookFragment");
    }
  };

  public static final BiConsumer<ExecutionInput, GraphQLContext> BOOK_ASSERTS = (executionInput, _context) -> {
    if (executionInput.getQuery().contains("bookFragment")) {
      assertThat(executionInput.getQuery()).contains("fragment bookFragment on Book");
      assertThat(executionInput.getQuery()).doesNotContain("Pet");
    }
  };

  @Test
  public void testBuilder() {
    final Builder baseBuilder = GraphQLOrchestrator.newOrchestrator();
    assertThatThrownBy(() -> baseBuilder.queryExecutionStrategy(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> baseBuilder.executionIdProvider(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> baseBuilder.instrumentations(null))
        .isInstanceOf(NullPointerException.class);

    final RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder()
        .services(Collections.emptyList()).build().stitchGraph();

    final GraphQLOrchestrator orchestrator = baseBuilder
        .runtimeGraph(runtimeGraph)
        .instrumentations(Collections.emptyList())
        .executionIdProvider(ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER)
        .queryExecutionStrategy(new AsyncExecutionStrategy())
        .mutationExecutionStrategy(new AsyncExecutionStrategy())
        .build();

    assertThat(orchestrator).isNotNull();
  }

  @Test
  public void testMultipleTopLevelFieldFromSameService() throws Exception {
    ServiceProvider[] services = new ServiceProvider[]{petsService};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
        new AsyncExecutionStrategy(), services);
    assertThat(orchestrator.getSchema().isSupportingMutations()).isTrue();

    // Test query using ExecutionInput
    ExecutionInput petsEI = ExecutionInput
        .newExecutionInput()
        .query("{ pets { id name }  pet(id : \"pet-1\") { id name } } ")
        .build();
    Map<String, Object> executionResult = orchestrator.execute(petsEI).get()
        .toSpecification();

    assertThat(executionResult.get("errors")).isNull();
    assertThat(executionResult.get("data")).isNotNull();
    Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data");
    assertThat(dataValue).containsKeys("pets", "pet");
    assertThat((List<Map<String, Objects>>) dataValue.get("pets")).hasSize(3);
    assertThat((Map<String, Objects>) dataValue.get("pet")).hasSize(2);
  }

  @Test
  public void testTopLevelCombinedSchemaWithIncludeDirectiveOnQuery() throws Exception {
    ServiceProvider[] services = new ServiceProvider[]{petsService, bookService, userService};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
        new AsyncExecutionStrategy(), services);
    assertThat(orchestrator.getSchema().isSupportingMutations()).isTrue();

    // Test query using ExecutionInput
    ExecutionInput booksAndPetsEI = ExecutionInput
        .newExecutionInput()
        .query("query BooksPetsAndUsers($includeType: Boolean!) { books { id name } "
            + "pets { id name type @include(if: $includeType) }"
            + "users { id firstName lastName @include(if: $includeType) } }")
        .variables(ImmutableMap.of("includeType", Boolean.TRUE))
        .build();
    Map<String, Object> executionResult = orchestrator.execute(booksAndPetsEI).get()
        .toSpecification();

    assertThat(executionResult.get("errors")).isNull();
    assertThat(executionResult.get("data")).isNotNull();
    Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data");
    assertThat(dataValue).containsKeys("pets", "books", "users");
    assertThat((List<Map<String, Objects>>) dataValue.get("pets")).hasSize(3);
    assertThat((List<Map<String, Objects>>) dataValue.get("books")).hasSize(3);
    assertThat((List<Map<String, Objects>>) dataValue.get("users")).hasSize(3);
  }

  @Test
  public void testTopLevelCombinedSchemaWithExecutionInput() throws Exception {
    ServiceProvider[] services = new ServiceProvider[]{petsService, bookService, userService};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
        new AsyncExecutionStrategy(), services);
    assertThat(orchestrator.getSchema().isSupportingMutations()).isTrue();

    // Test query using ExecutionInput
    ExecutionInput booksAndPetsEI = ExecutionInput
        .newExecutionInput().query("{ books { id name } pets { id name } users { id firstName lastName } }")
        .build();
    Map<String, Object> executionResult = orchestrator.execute(booksAndPetsEI).get()
        .toSpecification();

    assertThat(executionResult.get("errors")).isNull();
    assertThat(executionResult.get("data")).isNotNull();
    Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data");
    assertThat(dataValue).containsKeys("pets", "books", "users");
    assertThat((List<Map<String, Objects>>) dataValue.get("pets")).hasSize(3);
    assertThat((List<Map<String, Objects>>) dataValue.get("books")).hasSize(3);
    assertThat((List<Map<String, Objects>>) dataValue.get("users")).hasSize(3);
  }

  @Test
  public void testTopLevelCombinedSchemaWithExecutionInputBuilder() throws Exception {
    ServiceProvider[] services = new ServiceProvider[]{petsService, bookService, userService};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
        new AsyncExecutionStrategy(), services);
    assertThat(orchestrator.getSchema().isSupportingMutations()).isTrue();

    // Test query using ExecutionInput.Builder
    ExecutionInput.Builder booksEIBuilder = ExecutionInput
        .newExecutionInput().query("{ books { id name } }");

    Map<String, Object> executionResult = orchestrator.execute(booksEIBuilder).get().toSpecification();
    assertThat(executionResult.get("errors")).isNull();
    assertThat(executionResult.get("data")).isNotNull();
    Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data");
    assertThat(dataValue).containsKeys("books");
    assertThat((List<Map<String, Objects>>) dataValue.get("books")).hasSize(3);
  }

  @Test
  public void testTopLevelCombinedSchemaWithUnaryOperator() throws Exception {
    ServiceProvider[] services = new ServiceProvider[]{petsService, bookService, userService};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
        new AsyncExecutionStrategy(), services);
    assertThat(orchestrator.getSchema().isSupportingMutations()).isTrue();

    // Test query using UnaryOperator as source of query
    UnaryOperator<ExecutionInput.Builder> builderFunc = ExecutionInputTestUtil
        .builderFunc("{ \"query\": \"{ pets { id name }}\" }");

    Map<String, Object> executionResult = orchestrator.execute(builderFunc).get().toSpecification();
    assertThat(executionResult.get("errors")).isNull();
    assertThat(executionResult.get("data")).isNotNull();
    Map<String, Object> datavalue = (Map<String, Object>) executionResult.get("data");
    assertThat(datavalue).containsKeys("pets");
    assertThat((List<Map<String, Objects>>) datavalue.get("pets")).hasSize(3);
  }

  @Test
  public void testMutationOnTopLevelCombinedSchema() throws Exception {

    ServiceProvider[] services = new ServiceProvider[]{bookService, new MutablePetsService(), userService};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
        new AsyncExecutionStrategy(), services);
    assertThat(orchestrator.getSchema().isSupportingMutations()).isTrue();

    Map<String, Object> newpetMap = new HashMap<>();
    newpetMap.put("id", "pet-9");
    newpetMap.put("name", "Krypto");
    newpetMap.put("age", "1");
    newpetMap.put("weight", 5);
    newpetMap.put("purebred", Boolean.FALSE);
    newpetMap.put("tag", "DOG");

    Map<String, Object> newUserMap = new HashMap<>();
    newUserMap.put("id", "user-9");
    newUserMap.put("username", "naomi");
    newUserMap.put("password", "iamnaomi");
    newUserMap.put("firstName", "Naomi");
    newUserMap.put("lastName", "Connelly");

    // Add Pet
    ExecutionInput addPetEI = ExecutionInput
        .newExecutionInput()
        .query(
            "mutation AddNewPetAndUser($newpet: InputPet!, $newuser: NewUserInput!) {addPet(pet: $newpet) @merge (if: true) {id name}  addUser(newUser: $newuser) { id firstName }}")
        .variables(ImmutableMap.of("newpet", newpetMap, "newuser", newUserMap))
        .build();

    Map<String, Object> executionResult = orchestrator.execute(addPetEI).get().toSpecification();
    assertThat(executionResult.get("errors")).isNull();
    assertThat(executionResult.get("data")).isNotNull();
    Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data");
    assertThat(dataValue).containsKeys("addPet");
    assertThat((Map<String, Object>) dataValue.get("addPet"))
        .contains(new SimpleEntry<>("id", "pet-9"));
    assertThat((Map<String, Object>) dataValue.get("addPet"))
        .contains(new SimpleEntry<>("name", "Krypto"));

    assertThat(dataValue).containsKeys("addUser");
    assertThat((Map<String, Object>) dataValue.get("addUser"))
        .contains(new SimpleEntry<>("id", "user-9"));
    assertThat((Map<String, Object>) dataValue.get("addUser"))
        .contains(new SimpleEntry<>("firstName", "Naomi"));
  }

  @Test
  public void testQueryOnNestedCombinedSchema() throws Exception {
    ServiceProvider[] services = new ServiceProvider[]{new PersonService(),
        new NestedBooksService(), new NestedPetsService()};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
        new AsyncExecutionStrategy(), services);
    assertThat(orchestrator.getSchema().isSupportingMutations()).isTrue();

    // Test query using ExecutionInput
    ExecutionInput personEI = ExecutionInput.newExecutionInput()
        .query("{ person { id name book {id name} pets { name }  } }")
        .build();

    Map<String, Object> executionResult = orchestrator.execute(personEI).get().toSpecification();
    assertThat(executionResult.get("errors")).isNull();
    assertThat(executionResult.get("data")).isNotNull();
    Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data");
    assertThat(dataValue.get("person")).isNotNull();
    Map<String, Object> personVALUE = (Map<String, Object>) dataValue.get("person");
    assertThat(personVALUE.get("pets")).isNotNull();
    List<Map<String, Object>> petsValue = (List<Map<String, Object>>) personVALUE.get("pets");
    assertThat(petsValue).hasSize(3);
    assertThat(personVALUE.get("book")).isNotNull();
    Map<String, Object> bookValue = (Map<String, Object>) personVALUE.get("book");
    assertThat(bookValue)
        .contains(new SimpleEntry("id", "book-1"), new SimpleEntry("name", "GraphQL Advanced Stitching"));

  }

  @Test
  public void canQueryToplevelWithResultTypeOfBaseInterface() throws Exception {
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(
        new AsyncExecutionStrategy(), null, new StarWarsService());
    assertThat(orchestrator.getSchema().isSupportingMutations()).isTrue();

    // Test query using ExecutionInput
    ExecutionInput executionInput = ExecutionInput
        .newExecutionInput().query("{ hero { __typename ... charIdName appearsIn ... on Droid { primaryFunction }} } "
            + "fragment charIdName on Character { id name }")
        .build();

    Map<String, Object> executionResult = orchestrator.execute(executionInput).get().toSpecification();
    assertThat(executionResult.get("errors")).isNull();
    assertThat(executionResult.get("data")).isNotNull();
    Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data");
    assertThat(dataValue).containsKeys("hero");
    Map<String, Object> heroValue = (Map<String, Object>) dataValue.get("hero");
    assertThat(heroValue.keySet()).hasSize(5);
    assertThat(heroValue).contains(new SimpleEntry<>("__typename", "Droid"));
    assertThat(heroValue).contains(new SimpleEntry<>("name", "R2-D2"));
    assertThat(heroValue).contains(new SimpleEntry<>("primaryFunction", "Rescuing Luke"));
  }

  @Test
  public void canQueryToplevelWithResultTypeOfInterfaceImpl() throws Exception {
    ServiceProvider[] services = new ServiceProvider[]{new StarWarsService()};

    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(
        new AsyncExecutionStrategy(), null, services);
    assertThat(orchestrator.getSchema().isSupportingMutations()).isTrue();

    // Test query using ExecutionInput
    ExecutionInput executionInput = ExecutionInput
        .newExecutionInput().query("{ human { name appearsIn homePlanet} }")
        .build();

    Map<String, Object> executionResult = orchestrator.execute(executionInput).get().toSpecification();
    assertThat(executionResult.get("errors")).isNull();
    assertThat(executionResult.get("data")).isNotNull();
    Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data");
    assertThat(dataValue).containsKeys("human");
    Map<String, Object> human = (Map<String, Object>) dataValue.get("human");
    assertThat(human.keySet()).hasSize(3);
    assertThat(human).contains(new SimpleEntry<>("name", "Obi-Wan Kenobi"));
    assertThat(human).contains(new SimpleEntry<>("homePlanet", "Stewjon"));
  }


  @Test
  public void canQueryToplevelWithResultTypeOfWrappedInterfaceImpl() throws Exception {
    ServiceProvider[] services = new ServiceProvider[]{new StarWarsService()};

    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(
        new AsyncExecutionStrategy(), null, services);
    assertThat(orchestrator.getSchema().isSupportingMutations()).isTrue();

    // Test query using ExecutionInput
    ExecutionInput executionInput = ExecutionInput
        .newExecutionInput().query("{ characters { name appearsIn } }")
        .build();

    Map<String, Object> executionResult = orchestrator.execute(executionInput).get().toSpecification();
    assertThat(executionResult.get("errors")).isNull();
    assertThat(executionResult.get("data")).isNotNull();
    Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data");
    assertThat(dataValue).containsKeys("characters");
    List<Map<String, Object>> characters = (List<Map<String, Object>>) dataValue.get("characters");
    assertThat(characters).hasSize(2);
    assertThat(characters.get(0)).contains(new SimpleEntry<>("name", "Obi-Wan Kenobi"));
    assertThat(characters.get(1)).contains(new SimpleEntry<>("name", "R2-D2"));
  }

  @Test
  public void canQueryGoalService() throws Exception {
    TestCase testCase = TestCase.newTestCase()
        .service(new GoalsService())
        .query("query goalsQuery($goalId: Long) { userGoals { id  creationTime linkedProviders { "
            + " id name  ... on DebtProvider { currentValue } } } "
            + "userGoalImages(userGoalId: $goalId) { imageUrl imageBlob } }")
        .variables(ImmutableMap.of("goalId", Long.valueOf(1)))
        .build();

    testCase.run();
    testCase.assertHashNoErrors();
    testCase.assertHasData();

    List<Map<String, Object>> userGoals = (List<Map<String, Object>>) testCase.getDataField("userGoals");
    assertThat(userGoals).hasSize(2);
    assertThat(userGoals.get(0).get("__typename")).isNull();
    assertThat(userGoals.get(0).get("id")).isNotNull();
    assertThat(userGoals.get(0).get("creationTime")).isNotNull();
    assertThat((List) userGoals.get(0).get("linkedProviders")).hasSize(1);
    List<Map<String, Object>> linkedProviders0 = (List) userGoals.get(0).get("linkedProviders");
    assertThat(linkedProviders0).hasSize(1);
    assertThat(linkedProviders0.get(0).get("id")).isEqualTo("dp-1");
    assertThat(linkedProviders0.get(0).get("name")).isEqualTo("some debt provider");
    assertThat(linkedProviders0.get(0).get("currentValue")).isEqualTo(new BigDecimal(1.5));

    assertThat(userGoals.get(1).get("__typename")).isNull();
    assertThat(userGoals.get(1).get("id")).isNotNull();
    assertThat(userGoals.get(1).get("creationTime")).isNotNull();
    assertThat((List) userGoals.get(1).get("linkedProviders")).hasSize(1);
    List<Map<String, Object>> linkedProviders1 = (List) userGoals.get(1).get("linkedProviders");
    assertThat(linkedProviders1).hasSize(1);
    assertThat(linkedProviders1.get(0).get("id")).isEqualTo("dp-2");
    assertThat(linkedProviders1.get(0).get("name")).isEqualTo("some debt provider 2");
    assertThat(linkedProviders1.get(0).get("currentValue")).isEqualTo(new BigDecimal(5.0));

    List<Map<String, Object>> userGoalImages = (List<Map<String, Object>>) testCase.getDataField("userGoalImages");
    assertThat(userGoalImages).hasSize(1);
    assertThat(userGoalImages.get(0).get("imageUrl")).isEqualTo("SomeImageUrl");
    assertThat(userGoalImages.get(0).get("imageBlob")).isEqualTo("SomeImageBlob");
  }

  public static GraphQLOrchestrator createGraphQLOrchestrator(ExecutionStrategy queryExecutionStrategy,
      ExecutionStrategy mutationExecutionStrategy, ServiceProvider... services) {

    RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder()
        .services(Arrays.asList(services)).build().stitchGraph();

    GraphQLOrchestrator.Builder builder = GraphQLOrchestrator.newOrchestrator();
    builder.runtimeGraph(runtimeGraph);
    builder.instrumentations(Collections.emptyList());
    builder.executionIdProvider(ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER);
    if (Objects.nonNull(queryExecutionStrategy)) {
      builder.queryExecutionStrategy(queryExecutionStrategy);
    }
    if (Objects.nonNull(mutationExecutionStrategy)) {
      builder.mutationExecutionStrategy(mutationExecutionStrategy);
    }
    return builder.build();
  }

  public static GraphQLOrchestrator createGraphQLOrchestrator(ExecutionStrategy queryExecutionStrategy,
      ExecutionStrategy mutationExecutionStrategy, ServiceProvider service) {

    RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder().service(service)
        .build().stitchGraph();

    GraphQLOrchestrator.Builder builder = GraphQLOrchestrator.newOrchestrator();
    builder.runtimeGraph(runtimeGraph);
    builder.instrumentations(Collections.emptyList());
    builder.executionIdProvider(ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER);
    if (Objects.nonNull(queryExecutionStrategy)) {
      builder.queryExecutionStrategy(queryExecutionStrategy);
    }
    if (Objects.nonNull(mutationExecutionStrategy)) {
      builder.mutationExecutionStrategy(mutationExecutionStrategy);
    }
    return builder.build();
  }

  @Test(expected = FieldMergeException.class)
  public void cannotBuildDueToQueryNestedFieldsHasArguments() throws Exception {
    TestCase.newTestCase()
        .service(new ServiceA())
        .service(new ServiceB())
        .build();

  }

  @Test(expected = FieldMergeException.class)
  public void cannotBuildDueToQueryNestedFieldsHasDirectives() throws Exception {
    TestCase.newTestCase()
        .service(new ServiceA())
        .service(new ServiceC())
        .build();

  }

  @Test(expected = FieldMergeException.class)
  public void cannotBuildDueToMutationNestedFieldsHasArguments() throws Exception {
    TestCase.newTestCase()
        .service(new ServiceD())
        .service(new ServiceE())
        .build();

  }
}