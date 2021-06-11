package com.intuit.graphql.orchestrator.integration;

import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.graphql.orchestrator.TestHelper;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.datafetcher.ServiceDataFetcher;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.stitching.Stitcher;
import com.intuit.graphql.orchestrator.stitching.XtextStitcher;
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class StitchingTest {

  private static final Stitcher stitcher = XtextStitcher.newBuilder().build();
  private static final String url = "http://localhost";
  private static final HashMap<String, String> schemaMap = new HashMap<>();
  private static final HashMap<String, String> inlineSchemaMap = new HashMap<>();
  private static final List<ServiceProvider> providers = new ArrayList<>();
  private static RuntimeGraph runtimeGraph;

  @BeforeClass
  public static void init() {
    setSchemaMap();
    setCustomQueryMap();
    setupProviders();

    runtimeGraph = stitcher.stitch(providers);
  }

  private static ServiceProvider serviceProvider(String url, String namespace, Map<String, String> sdlFiles) {
    return TestServiceProvider.newBuilder().namespace(namespace).sdlFiles(sdlFiles).build();
  }

  private static void setSchemaMap() {
    // add resource files here in (NAMESPACE, PATH) format
    schemaMap.put("EPS", "top_level/eps/schema2.graphqls");
    schemaMap.put("PERSON", "top_level/person/schema1.graphqls");
    schemaMap.put("V40", "nested/v4os/schema.graphqls");
    schemaMap.put("TURBO", "nested/turbo/schema.graphqls");
  }

  private static void setCustomQueryMap() {
    // add inline schemas here
    inlineSchemaMap.put("SVC_b", "schema { query: Query } type Query { a: A } \"\n"
        + "         \"type A { b: B @adapter(service: 'foo') } type B {d: D}\"\n"
        + "         \"type D { field: String}\"\n"
        + "         \"directive @adapter(service:String!) on FIELD_DEFINITION");
    inlineSchemaMap.put("SVC_bb", "schema { query: Query } type Query { a: A } \"\n"
        + "         \"type A {  bbc: BB }  type BB {cc: String}");
    inlineSchemaMap.put("SVC_abc","schema { query: Query } type Query { a: A } \"\n"
        + "         \"type A {  bbcd: C }  type C {cc: String}");
    inlineSchemaMap.put("SVC_Second", "schema { query: Query } type Query { a: A } "
        + "type A {  bbbb: BAB }  type BAB {fieldBB: String}");
    inlineSchemaMap.put("AMBC","schema { query: Query } type Query { a: A } \"\n"
        + "         \"type A {  bba: CDD }  type CDD {ccdd: String}");
    inlineSchemaMap.put("TTBB","schema { query: Query } type Query { a: A } \"\n"
        + "         \"type A {  bbab: BBD }  type BBD {cc: String}");
  }

  private static void setupProviders() {
    schemaMap.keySet().forEach(k -> {
      providers.add(serviceProvider(url, k, TestHelper.getFileMapFromList(schemaMap.get(k))));
    });

    inlineSchemaMap.keySet().forEach(k -> {
      providers.add(XtextGraphBuilder
          .build(TestServiceProvider.newBuilder().namespace(k).serviceType(ServiceType.GRAPHQL)
              .sdlFiles(ImmutableMap.of("schema.graphqls", inlineSchemaMap.get(k))).build()).getServiceProvider());
    });
  }

  @Test
  public void testTopLevelStitching() {
    final GraphQLSchema graphQLSchema = runtimeGraph.getExecutableSchema();
    //String print = new SchemaPrinter().print(graphQLSchema);
    assertThat(graphQLSchema).isNotNull();
    //DataLoader assertions
    assertThat(runtimeGraph.getBatchLoaderMap()).hasSize(providers.size());
    // Queries
    GraphQLObjectType queryType = graphQLSchema.getQueryType();
    assertThat(queryType).isNotNull();

    GraphQLFieldDefinition personField = queryType.getFieldDefinition("person");
    assertThat(personField).isNotNull();

    GraphQLObjectType personType = (GraphQLObjectType) personField.getType();
    assertThat(personType.getName()).isEqualTo("Person");

    assertThat(personType.getFieldDefinition("id")).isNotNull();
    assertThat(personType.getFieldDefinition("id").getType()).isEqualTo(GraphQLID);
    assertThat(personType.getFieldDefinition("name")).isNotNull();
    assertThat(personType.getFieldDefinition("name").getType()).isEqualTo(GraphQLString);
    assertThat(personType.getFieldDefinition("income")).isNotNull();
    assertThat(personType.getFieldDefinition("income").getType()).isEqualTo(GraphQLInt);
    GraphQLFieldDefinition addressField = personType.getFieldDefinition("address");
    assertThat(addressField).isNotNull();

    GraphQLFieldDefinition profileField = queryType.getFieldDefinition("Profile");
    assertThat(profileField).isNotNull();
    GraphQLNonNull nonNullProfileType = (GraphQLNonNull) profileField.getType();
    assertThat(nonNullProfileType).isNotNull();

    GraphQLObjectType profileType = (GraphQLObjectType) nonNullProfileType.getWrappedType();
    assertThat(profileType).isNotNull();
    assertThat(profileType.getName()).isEqualTo("Profile");
    assertThat(profileType.getFieldDefinition("prefFirstName")).isNotNull();
    assertThat(
        profileType.getFieldDefinition("prefFirstName").getType()).isEqualTo(GraphQLString);
    assertThat(profileType.getFieldDefinition("prefLastName")).isNotNull();
    assertThat(
        profileType.getFieldDefinition("prefLastName").getType()).isEqualTo(GraphQLString);
    assertThat(profileType.getFieldDefinition("version")).isNotNull();
    assertThat(profileType.getFieldDefinition("version").getType()).isEqualTo(GraphQLInt);

    GraphQLArgument argument = profileField.getArgument("corpId");
    assertThat(argument).isNotNull();
    GraphQLNonNull argCorpIdType = (GraphQLNonNull) argument.getType();
    assertThat(argCorpIdType).isNotNull();

    //Mutations
    GraphQLObjectType mutationType = graphQLSchema.getMutationType();
    assertThat(mutationType).isNotNull();

    GraphQLFieldDefinition upsertProfile = mutationType.getFieldDefinition("upsertProfile");
    assertThat(upsertProfile).isNotNull();
    GraphQLArgument argument1 = upsertProfile.getArgument("corpId");
    assertThat(argument1).isNotNull();
    GraphQLNonNull argCorpIdType1 = (GraphQLNonNull) argument1.getType();
    assertThat(argCorpIdType1).isNotNull();
  }

  @Test
  public void testNestedStitching() {
    //DataLoader assertions (number of batchloaders == number of services)
    assertThat(runtimeGraph.getBatchLoaderMap()).hasSize(providers.size());

    final GraphQLSchema graphQLSchema = runtimeGraph.getExecutableSchema();
    //String print = new SchemaPrinter().print(graphQLSchema);
    assertThat(graphQLSchema).isNotNull();
    // Queries
    final GraphQLObjectType queryType = runtimeGraph.getOperation(Operation.QUERY);
    assertThat(queryType).isNotNull();

    final GraphQLFieldDefinition consumer = queryType.getFieldDefinition("consumer");
    assertThat(consumer).isNotNull();

    final GraphQLObjectType consumerType = (GraphQLObjectType) consumer.getType();
    assertThat(consumerType.getName()).isEqualTo("ConsumerType");

    final GraphQLFieldDefinition finance = consumerType.getFieldDefinition("finance");
    assertThat(finance).isNotNull();
    assertThat(((GraphQLNamedType)finance.getType()).getName()).isEqualTo("FinanceType");
    final GraphQLFieldDefinition financialProfile = consumerType.getFieldDefinition("financialProfile");
    assertThat(financialProfile).isNotNull();
    assertThat(((GraphQLNamedType)financialProfile.getType()).getName())
        .isEqualTo("FinancialProfileType");
    final GraphQLFieldDefinition turboExperiences = consumerType.getFieldDefinition("turboExperiences");
    assertThat(turboExperiences).isNotNull();
    assertThat(((GraphQLNamedType)turboExperiences.getType()).getName())
        .isEqualTo("ExperienceType");

    assertThat(finance).isNotNull();
    final GraphQLObjectType financeType = (GraphQLObjectType) finance.getType();

    assertThat(financeType.getFieldDefinition("fieldFinance").getType()).isEqualTo(GraphQLInt);
    assertThat(financeType.getFieldDefinition("fieldTurbo").getType()).isEqualTo(GraphQLInt);

    //DataFetchers
    final GraphQLCodeRegistry codeRegistry = runtimeGraph.getCodeRegistry().build();
    assertThat(codeRegistry.getDataFetcher(FieldCoordinates.coordinates("Query", "consumer"), consumer))
        .isInstanceOf(
            StaticDataFetcher.class);

    assertThat(codeRegistry.getDataFetcher(FieldCoordinates.coordinates("ConsumerType", "finance"), finance))
        .isInstanceOf(
            StaticDataFetcher.class);

    assertThat(codeRegistry.getDataFetcher(FieldCoordinates.coordinates("ConsumerType", "financialProfile"), finance))
        .isInstanceOf(
            ServiceDataFetcher.class);

    assertThat(
        codeRegistry.getDataFetcher(FieldCoordinates.coordinates("ConsumerType", "turboExperiences"), turboExperiences))
        .isInstanceOf(
            ServiceDataFetcher.class);

  }

  @Test
  public void NestedTypeDescriptionWithNamespaceAndEmptyDescription_TwoSchemaMergeTest() {
    final GraphQLObjectType queryType = runtimeGraph.getOperation(Operation.QUERY);
    final GraphQLFieldDefinition consumer = queryType.getFieldDefinition("consumer");
    final GraphQLObjectType consumerType = (GraphQLObjectType) consumer.getType();

    assertThat(consumerType.getDescription()).isEqualTo("[V40,TURBO]");
  }

  @Test
  public void NestedTypeDescriptionWithNamespaceAndEmptyDescription_MultiSchemaMergeTest() {
    final GraphQLObjectType queryType = runtimeGraph.getOperation(Operation.QUERY);
    final GraphQLFieldDefinition a = queryType.getFieldDefinition("a");
    final GraphQLObjectType aType = (GraphQLObjectType) a.getType();

    assertThat(aType.getDescription()).contains("SVC_abc");
    assertThat(aType.getDescription()).contains("SVC_bb");
    assertThat(aType.getDescription()).contains("AMBC");
    assertThat(aType.getDescription()).contains("TTBB");
    assertThat(aType.getDescription()).contains("SVC_Second");
    assertThat(aType.getDescription()).contains("SVC_b");
  }

}
