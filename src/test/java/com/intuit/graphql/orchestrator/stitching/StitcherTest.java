package com.intuit.graphql.orchestrator.stitching;

import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.graphql.orchestrator.TestHelper;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.datafetcher.ServiceDataFetcher;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.schema.type.conflict.resolver.TypeConflictException;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class StitcherTest {

  private final Stitcher stitcher = XtextStitcher.newBuilder().build();
  private final String DEFAULT_URL = "http://localhost";

  @Test
  public void testTopLevelStitching() {

    ServiceProvider provider1 = serviceProvider(DEFAULT_URL, "EPS", TestHelper.getFileMapFromList(
        "top_level/eps/schema2.graphqls"));
    ServiceProvider provider2 = serviceProvider(DEFAULT_URL, "PERSON",
        TestHelper.getFileMapFromList("top_level/person/schema1.graphqls"));
    List<ServiceProvider> serviceContextList = Arrays.asList(provider1, provider2);
    RuntimeGraph runtimeGraph = stitcher.stitch(serviceContextList);
    final GraphQLSchema graphQLSchema = runtimeGraph.getExecutableSchema();
    //String print = new SchemaPrinter().print(graphQLSchema);
    assertThat(graphQLSchema).isNotNull();
    //DataLoader assertions
    assertThat(runtimeGraph.getBatchLoaderMap().size()).isEqualTo(serviceContextList.size());
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

    ServiceProvider provider1 = serviceProvider(DEFAULT_URL, "V4O", TestHelper.getFileMapFromList(
        "nested/v4os/schema.graphqls"));
    ServiceProvider provider2 = serviceProvider(DEFAULT_URL, "TURBO",
        TestHelper.getFileMapFromList("nested/turbo/schema.graphqls"));
    List<ServiceProvider> serviceContextList = Arrays.asList(provider1, provider2);
    RuntimeGraph runtimeGraph = stitcher.stitch(serviceContextList);

    //DataLoader assertions (number of batchloaders == number of services)
    assertThat(runtimeGraph.getBatchLoaderMap().size()).isEqualTo(serviceContextList.size());

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
  public void makesCorrectGraphOnScalarConflict() {
    String schema1 = "type Query { foo : A } scalar A ";
    String schema2 = "type Query { bar : A } scalar A";

    List<ServiceProvider> services = Arrays.asList(
        TestServiceProvider.newBuilder().namespace("A").sdlFiles(ImmutableMap.of("schema1", schema1)).build(),
        TestServiceProvider.newBuilder().namespace("B").sdlFiles(ImmutableMap.of("schema2", schema2)).build()
    );

    RuntimeGraph runtimeGraph = stitcher.stitch(services);
    GraphQLObjectType query = runtimeGraph.getOperationMap().get(Operation.QUERY);
    assertThat(query).isNotNull();

    assertThat(query.getFieldDefinition("foo")).isNotNull();
    assertThat(query.getFieldDefinition("bar")).isNotNull();

    assertThat(query.getFieldDefinition("foo").getType())
        .isEqualTo(query.getFieldDefinition("bar").getType());

  }

  @Test
  public void makesCorrectGraphOnInBuiltScalar() {
    String schema1 = "type Query { foo : Long }  ";
    String schema2 = "type Query { bar : Long } scalar Long";

    List<ServiceProvider> services = Arrays.asList(
        TestServiceProvider.newBuilder().namespace("A").sdlFiles(ImmutableMap.of("schema1", schema1)).build(),
        TestServiceProvider.newBuilder().namespace("B").sdlFiles(ImmutableMap.of("schema2", schema2)).build()
    );

    RuntimeGraph runtimeGraph = stitcher.stitch(services);
    GraphQLObjectType query = runtimeGraph.getOperationMap().get(Operation.QUERY);
    assertThat(query).isNotNull();

    assertThat(query.getFieldDefinition("foo")).isNotNull();
    assertThat(query.getFieldDefinition("bar")).isNotNull();

    assertThat(query.getFieldDefinition("foo").getType())
        .isEqualTo(query.getFieldDefinition("bar").getType());

  }

  @Test
  public void throwsExceptionOnGoldenTypeConflict() {
    String schema1 = "type Query { foo: PageInfo } type PageInfo { id: String }";
    String schema2 = "type Query { bar: PageInfo } type PageInfo { id: String }";

    List<ServiceProvider> services = Arrays.asList(
            TestServiceProvider.newBuilder().namespace("A").sdlFiles(ImmutableMap.of("schema1", schema1)).build(),
            TestServiceProvider.newBuilder().namespace("B").sdlFiles(ImmutableMap.of("schema2", schema2)).build()
    );

    assertThatThrownBy(() -> stitcher.stitch(services)).isInstanceOf(TypeConflictException.class);

  }

  @Test
  public void throwsExceptionOnGoldenInterfaceConflict() {
    String schema1 = "type Query { foo: Nod } interface Nod { id: String } type foo implements Nod {id: String}";
    String schema2 = "type Query { bar: Nod } interface Nod { id: String } type bar implements Nod {id: String}";

    List<ServiceProvider> services = Arrays.asList(
            TestServiceProvider.newBuilder().namespace("A").sdlFiles(ImmutableMap.of("schema1", schema1)).build(),
            TestServiceProvider.newBuilder().namespace("B").sdlFiles(ImmutableMap.of("schema2", schema2)).build()
    );

    assertThatThrownBy(() -> stitcher.stitch(services)).isInstanceOf(TypeConflictException.class);
  }

  @Test
  public void NestedTypeDescriptionWithNamespaceAndEmptyDescription_TwoSchemaMergeTest() {
    ServiceProvider provider1 = serviceProvider(DEFAULT_URL, "V4O", TestHelper.getFileMapFromList(
        "nested/v4os/schema.graphqls"));
    ServiceProvider provider2 = serviceProvider(DEFAULT_URL, "TURBO",
        TestHelper.getFileMapFromList("nested/turbo/schema.graphqls"));
    List<ServiceProvider> serviceContextList = Arrays.asList(provider1, provider2);
    RuntimeGraph runtimeGraph = stitcher.stitch(serviceContextList);

    final GraphQLObjectType queryType = runtimeGraph.getOperation(Operation.QUERY);
    final GraphQLFieldDefinition consumer = queryType.getFieldDefinition("consumer");
    final GraphQLObjectType consumerType = (GraphQLObjectType) consumer.getType();

    assertThat(consumerType.getDescription()).isEqualTo("[V4O,TURBO]");
  }

  @Test
  public void NestedTypeDescriptionWithNamespaceAndEmptyDescription_MultiSchemaMergeTest() {
    String schema1 = "schema { query: Query } type Query { a: A } "
        + "type A { b: B @adapter(service: 'foo') } type B {d: D}"
        + "type D { field: String}"
        + "directive @adapter(service:String!) on FIELD_DEFINITION";

    String schema2 = "schema { query: Query } type Query { a: A } "
        + "type A {  bbc: BB }  type BB {cc: String}";

    String schema3 = "schema { query: Query } type Query { a: A } "
        + "type A {  bbcd: C }  type C {cc: String}";

    String schema4 = "schema { query: Query } type Query { a: A } "
        + "type A {  bbbb: BAB }  type BAB {fieldBB: String}";

    String schema5 = "schema { query: Query } type Query { a: A } "
        + "type A {  bba: CDD }  type CDD {ccdd: String}";

    String schema6 = "schema { query: Query } type Query { a: A } "
        + "type A {  bbab: BBD }  type BBD {cc: String}";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_b").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_bb").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    XtextGraph xtextGraph3 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_abc").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema3)).build());

    XtextGraph xtextGraph4 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_Second").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema4)).build());

    XtextGraph xtextGraph5 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("AMBC").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema5)).build());

    XtextGraph xtextGraph6 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("TTBB").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema6)).build());

    List<ServiceProvider> providerList = Arrays.asList(
        xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider(),
        xtextGraph3.getServiceProvider(), xtextGraph4.getServiceProvider(),
        xtextGraph5.getServiceProvider(), xtextGraph6.getServiceProvider());
    RuntimeGraph runtimeGraph = stitcher.stitch(providerList);

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

  @Test
  public void NestedTypeDescriptionWithNamespaceAndDescription_MultiSchemaMergeTest() {
    String schema1 = "schema { query: Query } type Query { a: A } "
        + "type A { b: B @adapter(service: 'foo') } type B {d: D}"
        + "type D { field: String}"
        + "directive @adapter(service:String!) on FIELD_DEFINITION";

    String schema2 = "schema { query: Query } type Query { a: A } "
        + "\"description for schema2\"type A {  bbc: BB }  type BB {cc: String}";

    String schema3 = "schema { query: Query } type Query { a: A } "
        + "\"description for schema3\"type A {  bbcd: C }  type C {cc: String}";

    String schema4 = "schema { query: Query } type Query { a: A } "
        + "type A {  bbbb: BAB }  type BAB {fieldBB: String}";

    XtextGraph xtextGraph1 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_b").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build());

    XtextGraph xtextGraph2 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_bb").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build());

    XtextGraph xtextGraph3 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_abc").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema3)).build());

    XtextGraph xtextGraph4 = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC_Second").serviceType(ServiceType.REST)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema4)).build());

    List<ServiceProvider> providerList = Arrays.asList(
        xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider(),
        xtextGraph3.getServiceProvider(), xtextGraph4.getServiceProvider());
    RuntimeGraph runtimeGraph = stitcher.stitch(providerList);

    final GraphQLObjectType queryType = runtimeGraph.getOperation(Operation.QUERY);
    final GraphQLFieldDefinition a = queryType.getFieldDefinition("a");
    final GraphQLObjectType aType = (GraphQLObjectType) a.getType();

    assertThat(aType.getDescription()).contains("SVC_abc");
    assertThat(aType.getDescription()).contains("SVC_bb");
    assertThat(aType.getDescription()).contains("SVC_Second");
    assertThat(aType.getDescription()).contains("SVC_b");
    assertThat(aType.getDescription()).contains("description for schema3");
    assertThat(aType.getDescription()).contains("description for schema2");
  }

  @Test
  public void testTopLevelFederationStitching() {
    ServiceProvider provider1 = serviceProvider(DEFAULT_URL, "Employee",
            TestHelper.getFileMapFromList("top_level/federation/employee.graphqls"),
            ServiceType.APOLLO_SUBGRAPH);

    ServiceProvider provider2 = serviceProvider(DEFAULT_URL, "Inventory",
            TestHelper.getFileMapFromList("top_level/federation/inventory.graphqls"),
            ServiceType.APOLLO_SUBGRAPH);

    List<ServiceProvider> serviceContextList = Arrays.asList(provider1, provider2);

    RuntimeGraph runtimeGraph = stitcher.stitch(serviceContextList);
    final GraphQLSchema graphQLSchema = runtimeGraph.getExecutableSchema();
    final GraphQLObjectType queryType = runtimeGraph.getOperation(Operation.QUERY);

    assertThat(graphQLSchema).isNotNull();
    assertThat(graphQLSchema.getQueryType().getFieldDefinitions().size()).isEqualTo(3);

    assertThat(queryType.getFieldDefinition("employeeById")).isNotNull();
    assertThat(queryType.getFieldDefinition("getSoldProducts")).isNotNull();
    assertThat(queryType.getFieldDefinition("getStoreByIdAndName")).isNotNull();
  }

  private ServiceProvider serviceProvider(String url, String namespace, Map<String, String> sdlFiles) {
    return serviceProvider(url, namespace, sdlFiles, ServiceType.GRAPHQL);
  }

  private ServiceProvider serviceProvider(String url, String namespace, Map<String, String> sdlFiles, ServiceType serviceType) {
    return TestServiceProvider.newBuilder().namespace(namespace).sdlFiles(sdlFiles).serviceType(serviceType).build();
  }
}
