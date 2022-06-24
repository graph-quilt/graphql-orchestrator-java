package com.intuit.graphql.orchestrator.federation;

import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EntityDataFetcherTest {

  @Mock private DataFetchingEnvironment dataFetchingEnvironmentMock;

  @Mock private EntityExtensionMetadata entityExtensionMetadataMock;

  @Mock private GraphQLContext graphQLContextMock;

  @Mock private ServiceProvider serviceProviderMock;

  private EntityDataFetcher subjectUnderTest;

  @Before
  public void setup() {
    subjectUnderTest = new EntityDataFetcher(entityExtensionMetadataMock);
  }

  @Test
  public void t() {}
//
//  @Test
//  public void get_success() throws ExecutionException, InterruptedException {
//    //    query {
//    //      a {
//    //        id a1
//    //        reqField
//    //        newField
//    //      }
//    //    }
//
//    //    type A @key (fields: "id") {
//    //      id: String
//    //      reqField: String
//    //      a1: String
//    //    }
//
//    //    extend type A @key (fields: "id") {
//    //      id @external
//    //      reqField @external
//    //      newField @requires(fields: "reqField")
//    //    }
//    Map<String, Object> testDfeSource = new HashMap<>();
//    testDfeSource.put("id", "someId"); // added by NoExternalReferenceSelectionSetModifier
//    testDfeSource.put(
//        "reqField", "reqFieldValue"); // added by NoExternalReferenceSelectionSetModifier
//    testDfeSource.put("a1", "a1Value");
//
//    Field testDfeField = Field.newField("newField").build();
//
//    Map<String, Object> serviceResponseMap = new HashMap<>();
//    List<Map<String, Object>> entityExtensionResult = new ArrayList<>();
//    entityExtensionResult.add(
//        ImmutableMap.of(
//            "__typename", "TestEntityType",
//            "id", "someId",
//            "newField", "newFieldValue"));
//    serviceResponseMap.put("data", ImmutableMap.of("_entities", entityExtensionResult));
//    when(serviceProviderMock.query(any(ExecutionInput.class), any(GraphQLContext.class)))
//        .thenReturn(CompletableFuture.completedFuture(serviceResponseMap));
//
//    KeyDirectiveMetadata keyDirectiveMetadataMock = mock(KeyDirectiveMetadata.class);
//    when(keyDirectiveMetadataMock.getFieldSet())
//        .thenReturn(ImmutableSet.of(Field.newField("id").build()));
//    when(entityExtensionMetadataMock.getKeyDirectives())
//        .thenReturn(Collections.singletonList(keyDirectiveMetadataMock));
//    when(entityExtensionMetadataMock.getTypeName()).thenReturn("TestEntityType");
//    when(entityExtensionMetadataMock.getServiceProvider()).thenReturn(serviceProviderMock);
//    when(entityExtensionMetadataMock.getRequiredFields("newField"))
//        .thenReturn(ImmutableSet.of(Field.newField("reqField").build()));
//
//    when(dataFetchingEnvironmentMock.getContext()).thenReturn(graphQLContextMock);
//    when(dataFetchingEnvironmentMock.getSource()).thenReturn(testDfeSource);
//    when(dataFetchingEnvironmentMock.getField()).thenReturn(testDfeField);
//    CompletableFuture<Object> futureResponse = subjectUnderTest.get(dataFetchingEnvironmentMock);
//    Object result = futureResponse.get();
//
//    assertThat(result).isEqualTo("newFieldValue");
//
//    verify(serviceProviderMock, times(1)).query(any(), any());
//  }
//
//  @Test
//  public void get_no_entity_field_exception() throws ExecutionException, InterruptedException {
//    //    query {
//    //      a {
//    //        id a1
//    //        reqField
//    //        newField
//    //      }
//    //    }
//
//    //    type A @key (fields: "id") {
//    //      id: String
//    //      reqField: String
//    //      a1: String
//    //    }
//
//    //    extend type A @key (fields: "id") {
//    //      id @external
//    //      reqField @external
//    //      newField @requires(fields: "reqField")
//    //    }
//    Map<String, Object> testDfeSource = new HashMap<>();
//    testDfeSource.put("id", "someId"); // added by NoExternalReferenceSelectionSetModifier
//    testDfeSource.put(
//            "reqField", "reqFieldValue"); // added by NoExternalReferenceSelectionSetModifier
//    testDfeSource.put("a1", "a1Value");
//
//    Field testDfeField = Field.newField("newField").build();
//
//    Map<String, Object> serviceResponseMap = new HashMap<>();
//    List<Map<String, Object>> entityExtensionResult = new ArrayList<>();
//    entityExtensionResult.add(
//            ImmutableMap.of(
//                    "__typename", "TestEntityType",
//                    "id", "someId",
//                    "newField", "newFieldValue"));
//    serviceResponseMap.put("data", ImmutableMap.of("_ENTITIES", entityExtensionResult));
//    when(serviceProviderMock.query(any(ExecutionInput.class), any(GraphQLContext.class)))
//            .thenReturn(CompletableFuture.completedFuture(serviceResponseMap));
//
//    KeyDirectiveMetadata keyDirectiveMetadataMock = mock(KeyDirectiveMetadata.class);
//    when(keyDirectiveMetadataMock.getFieldSet())
//            .thenReturn(ImmutableSet.of(Field.newField("id").build()));
//    when(entityExtensionMetadataMock.getKeyDirectives())
//            .thenReturn(Collections.singletonList(keyDirectiveMetadataMock));
//    when(entityExtensionMetadataMock.getTypeName()).thenReturn("TestEntityType");
//    when(entityExtensionMetadataMock.getServiceProvider()).thenReturn(serviceProviderMock);
//    when(entityExtensionMetadataMock.getRequiredFields("newField"))
//            .thenReturn(ImmutableSet.of(Field.newField("reqField").build()));
//
//    when(dataFetchingEnvironmentMock.getContext()).thenReturn(graphQLContextMock);
//    when(dataFetchingEnvironmentMock.getSource()).thenReturn(testDfeSource);
//    when(dataFetchingEnvironmentMock.getField()).thenReturn(testDfeField);
//
//    try {
//      subjectUnderTest.get(dataFetchingEnvironmentMock).get();
//      Assert.fail();
//    } catch (Exception ex) {
//      if(ex.getCause() instanceof EntityFetchingException) {
//        EntityFetchingException cause = (EntityFetchingException) ex.getCause();
//        assertThat(cause.getErrorType()).isEqualTo(ErrorType.DataFetchingException);
//        assertThat(cause.getMessage()).contains(NO_ENTITY_FIELD);
//      } else {
//        Assert.fail();
//      }
//    }
//  }
//
//  @Test
//  public void get_null_entity_field_exception() throws ExecutionException, InterruptedException {
//    //    query {
//    //      a {
//    //        id a1
//    //        reqField
//    //        newField
//    //      }
//    //    }
//
//    //    type A @key (fields: "id") {
//    //      id: String
//    //      reqField: String
//    //      a1: String
//    //    }
//
//    //    extend type A @key (fields: "id") {
//    //      id @external
//    //      reqField @external
//    //      newField @requires(fields: "reqField")
//    //    }
//    Map<String, Object> testDfeSource = new HashMap<>();
//    testDfeSource.put("id", "someId"); // added by NoExternalReferenceSelectionSetModifier
//    testDfeSource.put(
//            "reqField", "reqFieldValue"); // added by NoExternalReferenceSelectionSetModifier
//    testDfeSource.put("a1", "a1Value");
//
//    Field testDfeField = Field.newField("newField").build();
//
//    HashMap<String, Object> nullEntityResponse = new HashMap<>();
//    nullEntityResponse.put("_entities", null);
//
//    Map<String, Object> serviceResponseMap = new HashMap<>();
//    serviceResponseMap.put("data", nullEntityResponse);
//    when(serviceProviderMock.query(any(ExecutionInput.class), any(GraphQLContext.class)))
//            .thenReturn(CompletableFuture.completedFuture(serviceResponseMap));
//
//    KeyDirectiveMetadata keyDirectiveMetadataMock = mock(KeyDirectiveMetadata.class);
//    when(keyDirectiveMetadataMock.getFieldSet())
//            .thenReturn(ImmutableSet.of(Field.newField("id").build()));
//    when(entityExtensionMetadataMock.getKeyDirectives())
//            .thenReturn(Collections.singletonList(keyDirectiveMetadataMock));
//    when(entityExtensionMetadataMock.getTypeName()).thenReturn("TestEntityType");
//    when(entityExtensionMetadataMock.getServiceProvider()).thenReturn(serviceProviderMock);
//    when(entityExtensionMetadataMock.getRequiredFields("newField"))
//            .thenReturn(ImmutableSet.of(Field.newField("reqField").build()));
//
//    when(dataFetchingEnvironmentMock.getContext()).thenReturn(graphQLContextMock);
//    when(dataFetchingEnvironmentMock.getSource()).thenReturn(testDfeSource);
//    when(dataFetchingEnvironmentMock.getField()).thenReturn(testDfeField);
//
//    try {
//      subjectUnderTest.get(dataFetchingEnvironmentMock).get();
//      Assert.fail();
//    } catch (Exception ex) {
//      if(ex.getCause() instanceof EntityFetchingException) {
//        EntityFetchingException cause = (EntityFetchingException) ex.getCause();
//        assertThat(cause.getErrorType()).isEqualTo(ErrorType.DataFetchingException);
//        assertThat(cause.getMessage()).contains(NO_ENTITY_FIELD);
//      } else {
//        Assert.fail();
//      }
//    }
//  }
//
//  @Test
//  public void get_entity_field_with_validation_errors() throws ExecutionException, InterruptedException {
//    Map<String, Object> testDfeSource = new HashMap<>();
//    testDfeSource.put("id", "someId"); // added by NoExternalReferenceSelectionSetModifier
//    testDfeSource.put("reqField", "reqFieldValue"); // added by NoExternalReferenceSelectionSetModifier
//    testDfeSource.put("a1", "a1Value");
//
//    Field testDfeField = Field.newField("newField").build();
//
//    String errorMsg1 = "Provider Generated Error Message 1.";
//    String errorMsg2 = "Provider Generated Error Message 2.";
//
//    ObjectMapper mapper = new ObjectMapper();
//    GraphQLError error1 = GraphqlErrorBuilder.newError().errorType(ErrorType.ValidationError).message(errorMsg1).build();
//    GraphQLError error2 = GraphqlErrorBuilder.newError().errorType(ErrorType.ValidationError).message(errorMsg2).build();
//    List<Map<String, Object>> errorsResponse = new ArrayList<>();
//
//    errorsResponse.add(mapper.convertValue(error1, new TypeReference<Map<String, Object>>() {}));
//    errorsResponse.add(mapper.convertValue(error2, new TypeReference<Map<String, Object>>() {}));
//
//    HashMap<String, Object> entityResponseMap = new HashMap<>();
//
//    List<Map<String, Object>> entityExtensionResult = new ArrayList<>();
//    entityExtensionResult.add(ImmutableMap.of("id", "someId"));
//
//    entityResponseMap.put("errors", errorsResponse);
//    entityResponseMap.put("data", ImmutableMap.of("_entities", entityExtensionResult));
//
//    when(serviceProviderMock.query(any(ExecutionInput.class), any(GraphQLContext.class)))
//            .thenReturn(CompletableFuture.completedFuture(entityResponseMap));
//
//    KeyDirectiveMetadata keyDirectiveMetadataMock = mock(KeyDirectiveMetadata.class);
//    when(keyDirectiveMetadataMock.getFieldSet())
//            .thenReturn(ImmutableSet.of(Field.newField("id").build()));
//    when(entityExtensionMetadataMock.getKeyDirectives())
//            .thenReturn(Collections.singletonList(keyDirectiveMetadataMock));
//    when(entityExtensionMetadataMock.getTypeName()).thenReturn("TestEntityType");
//    when(entityExtensionMetadataMock.getServiceProvider()).thenReturn(serviceProviderMock);
//    when(entityExtensionMetadataMock.getRequiredFields("newField"))
//            .thenReturn(ImmutableSet.of(Field.newField("reqField").build()));
//
//    when(dataFetchingEnvironmentMock.getContext()).thenReturn(graphQLContextMock);
//    when(dataFetchingEnvironmentMock.getSource()).thenReturn(testDfeSource);
//    when(dataFetchingEnvironmentMock.getField()).thenReturn(testDfeField);
//
//    try {
//      subjectUnderTest.get(dataFetchingEnvironmentMock).get();
//      Assert.fail();
//    } catch (Exception ex) {
//      if(ex.getCause() instanceof EntityFetchingException) {
//        EntityFetchingException cause = (EntityFetchingException) ex.getCause();
//        assertThat(cause.getErrorType()).isEqualTo(ErrorType.DataFetchingException);
//        assertThat(cause.getMessage()).contains(errorMsg1);
//        assertThat(cause.getMessage()).contains(errorMsg2);
//      } else {
//        Assert.fail();
//      }
//    }
//  }
//
//  @Test
//  public void get_empty_data_with_validation_errors() throws ExecutionException, InterruptedException {
//    Map<String, Object> testDfeSource = new HashMap<>();
//    testDfeSource.put("id", "someId"); // added by NoExternalReferenceSelectionSetModifier
//    testDfeSource.put("reqField", "reqFieldValue"); // added by NoExternalReferenceSelectionSetModifier
//    testDfeSource.put("a1", "a1Value");
//
//    Field testDfeField = Field.newField("newField").build();
//
//    String errorMsg1 = "Provider Generated Error Message.";
//
//    ObjectMapper mapper = new ObjectMapper();
//    GraphQLError error1 = GraphqlErrorBuilder.newError().errorType(ErrorType.ValidationError).message(errorMsg1).build();
//    List<Map<String, Object>> errorsResponse = new ArrayList<>();
//
//    errorsResponse.add(mapper.convertValue(error1, new TypeReference<Map<String, Object>>() {}));
//
//    HashMap<String, Object> entityResponseMap = new HashMap<>();
//
//    entityResponseMap.put("errors", errorsResponse);
//    entityResponseMap.put("data", ImmutableMap.builder().build());
//
//    when(serviceProviderMock.query(any(ExecutionInput.class), any(GraphQLContext.class)))
//            .thenReturn(CompletableFuture.completedFuture(entityResponseMap));
//
//    KeyDirectiveMetadata keyDirectiveMetadataMock = mock(KeyDirectiveMetadata.class);
//    when(keyDirectiveMetadataMock.getFieldSet())
//            .thenReturn(ImmutableSet.of(Field.newField("id").build()));
//    when(entityExtensionMetadataMock.getKeyDirectives())
//            .thenReturn(Collections.singletonList(keyDirectiveMetadataMock));
//    when(entityExtensionMetadataMock.getTypeName()).thenReturn("TestEntityType");
//    when(entityExtensionMetadataMock.getServiceProvider()).thenReturn(serviceProviderMock);
//    when(entityExtensionMetadataMock.getRequiredFields("newField"))
//            .thenReturn(ImmutableSet.of(Field.newField("reqField").build()));
//
//    when(dataFetchingEnvironmentMock.getContext()).thenReturn(graphQLContextMock);
//    when(dataFetchingEnvironmentMock.getSource()).thenReturn(testDfeSource);
//    when(dataFetchingEnvironmentMock.getField()).thenReturn(testDfeField);
//
//    try {
//      subjectUnderTest.get(dataFetchingEnvironmentMock).get();
//      Assert.fail();
//    } catch (Exception ex) {
//      if(ex.getCause() instanceof EntityFetchingException) {
//        EntityFetchingException cause = (EntityFetchingException) ex.getCause();
//        assertThat(cause.getErrorType()).isEqualTo(ErrorType.DataFetchingException);
//        assertThat(cause.getMessage()).contains(errorMsg1);
//      } else {
//        Assert.fail();
//      }
//    }
//  }

  public void load_NonAnnotatedFieldsOfEntityTypeAreSelected_success() {
    // future use case
  }

  // TODO edge cases
  //  fragments / inlinefragments
  //  entity extension additional fields other types: object/interface/non string primitive types
  // etc

}
