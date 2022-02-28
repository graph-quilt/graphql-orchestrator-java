package com.intuit.graphql.orchestrator.federation;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EntityDataFetcerTest {

  @Mock
  private DataFetchingEnvironment dataFetchingEnvironmentMock;

  @Mock
  private EntityExtensionMetadata entityExtensionMetadataMock;

  @Mock
  private GraphQLContext graphQLContextMock;

  @Mock
  private ServiceProvider serviceProviderMock;

  @Mock
  private ServiceProvider baseServiceProviderMock;

  private EntityDataFetcher subjectUnderTest;

  @Before
  public void setup() {
    subjectUnderTest = new EntityDataFetcher(entityExtensionMetadataMock);
  }

  @Test
  public void get_keysAreSelected_success() throws ExecutionException, InterruptedException {
    //    query {
    //      a {
    //        id
    //        newField
    //      }
    //    }

    //    type A @key (fields: "id") {
    //      id: String @external
    //      a1: String
    //    }

    //    extend type A @key (fields: "id") {
    //      id @external
    //      reqField @external
    //      newField @requires(fields: "reqField")
    //    }
    Map<String, Object> testDfeSource = new HashMap<>();
    testDfeSource.put("id", "someId");
    testDfeSource.put("a1", "a1Value");

    Field testDfeField = Field.newField("newField")
        .build();

    KeyDirectiveMetadata keyDirectiveMetadataMock = mock(KeyDirectiveMetadata.class);
    when(keyDirectiveMetadataMock.getKeyFieldNames()).thenReturn(Arrays.asList("id"));

    Map<String, Object> baseServiceResponseMap = new HashMap<>();
    List<Map<String, Object>> baseEntitiesResult = new ArrayList<>();
    baseEntitiesResult.add(ImmutableMap.of(
       "__typename","TestEntityType",
        "id","someId",
        "reqField","reqFieldValue"
    ));
    baseServiceResponseMap.put("data", ImmutableMap.of("_entities", baseEntitiesResult));
    when(baseServiceProviderMock.query(any(ExecutionInput.class), any(GraphQLContext.class)))
        .thenReturn(CompletableFuture.completedFuture(baseServiceResponseMap));



    Map<String, Object> serviceResponseMap = new HashMap<>();
    List<Map<String, Object>> entityExtensionResult = new ArrayList<>();
    entityExtensionResult.add(ImmutableMap.of(
        "__typename","TestEntityType",
        "id","someId",
        "newField","newFieldValue"
    ));
    serviceResponseMap.put("data", ImmutableMap.of("_entities", entityExtensionResult));
    when(serviceProviderMock.query(any(ExecutionInput.class), any(GraphQLContext.class)))
      .thenReturn(CompletableFuture.completedFuture(serviceResponseMap));

    when(entityExtensionMetadataMock.getRequiredFields()).thenReturn(ImmutableSet.of("reqField"));
    when(entityExtensionMetadataMock.getTypeName()).thenReturn("TestEntityType");
    when(entityExtensionMetadataMock.getKeyDirectives()).thenReturn(Collections.singletonList(keyDirectiveMetadataMock));
    when(entityExtensionMetadataMock.getBaseServiceProvider()).thenReturn(baseServiceProviderMock);
    when(entityExtensionMetadataMock.getServiceProvider()).thenReturn(serviceProviderMock);

    when(dataFetchingEnvironmentMock.getContext()).thenReturn(graphQLContextMock);
    when(dataFetchingEnvironmentMock.getSource()).thenReturn(testDfeSource);
    when(dataFetchingEnvironmentMock.getField()).thenReturn(testDfeField);
    CompletableFuture<Object> futureResponse =  subjectUnderTest.get(dataFetchingEnvironmentMock);
    Object result = futureResponse.get();

    assertThat(result).isEqualTo("newFieldValue");
  }

  public void load_keysAreNotSelected_success() {

  }

  public void load_requiredFieldsAreSelected_success() {

  }

  public void load_requiredFieldsAreNotSelected_success() {

  }

  public void load_NonAnnotatedFieldsOfEntityTypeAreSelected_success() {

  }

  // TODO fragments  inlinefragments

}
