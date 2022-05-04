package com.intuit.graphql.orchestrator.federation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityExtensionMetadata;
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata;
import graphql.ErrorType;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.intuit.graphql.orchestrator.federation.EntityDataFetcher.NO_ENTITY_FIELD;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  public void get_success() throws ExecutionException, InterruptedException {
    //    query {
    //      a {
    //        id a1
    //        reqField
    //        newField
    //      }
    //    }

    //    type A @key (fields: "id") {
    //      id: String
    //      reqField: String
    //      a1: String
    //    }

    //    extend type A @key (fields: "id") {
    //      id @external
    //      reqField @external
    //      newField @requires(fields: "reqField")
    //    }
    Map<String, Object> testDfeSource = new HashMap<>();
    testDfeSource.put("id", "someId"); // added by NoExternalReferenceSelectionSetModifier
    testDfeSource.put(
        "reqField", "reqFieldValue"); // added by NoExternalReferenceSelectionSetModifier
    testDfeSource.put("a1", "a1Value");

    Field testDfeField = Field.newField("newField").build();

    Map<String, Object> serviceResponseMap = new HashMap<>();
    List<Map<String, Object>> entityExtensionResult = new ArrayList<>();
    entityExtensionResult.add(
        ImmutableMap.of(
            "__typename", "TestEntityType",
            "id", "someId",
            "newField", "newFieldValue"));
    serviceResponseMap.put("data", ImmutableMap.of("_entities", entityExtensionResult));
    when(serviceProviderMock.query(any(ExecutionInput.class), any(GraphQLContext.class)))
        .thenReturn(CompletableFuture.completedFuture(serviceResponseMap));

    KeyDirectiveMetadata keyDirectiveMetadataMock = mock(KeyDirectiveMetadata.class);
    when(keyDirectiveMetadataMock.getFieldSet())
        .thenReturn(ImmutableSet.of(Field.newField("id").build()));
    when(entityExtensionMetadataMock.getKeyDirectives())
        .thenReturn(Collections.singletonList(keyDirectiveMetadataMock));
    when(entityExtensionMetadataMock.getTypeName()).thenReturn("TestEntityType");
    when(entityExtensionMetadataMock.getServiceProvider()).thenReturn(serviceProviderMock);
    when(entityExtensionMetadataMock.getRequiredFields("newField"))
        .thenReturn(ImmutableSet.of(Field.newField("reqField").build()));

    when(dataFetchingEnvironmentMock.getContext()).thenReturn(graphQLContextMock);
    when(dataFetchingEnvironmentMock.getSource()).thenReturn(testDfeSource);
    when(dataFetchingEnvironmentMock.getField()).thenReturn(testDfeField);
    CompletableFuture<Object> futureResponse = subjectUnderTest.get(dataFetchingEnvironmentMock);
    Object result = futureResponse.get();

    assertThat(result).isEqualTo("newFieldValue");

    verify(serviceProviderMock, times(1)).query(any(), any());
  }


  @Test
  public void get_no_entity_field_exception() throws ExecutionException, InterruptedException {
    //    query {
    //      a {
    //        id a1
    //        reqField
    //        newField
    //      }
    //    }

    //    type A @key (fields: "id") {
    //      id: String
    //      reqField: String
    //      a1: String
    //    }

    //    extend type A @key (fields: "id") {
    //      id @external
    //      reqField @external
    //      newField @requires(fields: "reqField")
    //    }
    Map<String, Object> testDfeSource = new HashMap<>();
    testDfeSource.put("id", "someId"); // added by NoExternalReferenceSelectionSetModifier
    testDfeSource.put(
            "reqField", "reqFieldValue"); // added by NoExternalReferenceSelectionSetModifier
    testDfeSource.put("a1", "a1Value");

    Field testDfeField = Field.newField("newField").build();

    Map<String, Object> serviceResponseMap = new HashMap<>();
    List<Map<String, Object>> entityExtensionResult = new ArrayList<>();
    entityExtensionResult.add(
            ImmutableMap.of(
                    "__typename", "TestEntityType",
                    "id", "someId",
                    "newField", "newFieldValue"));
    serviceResponseMap.put("data", ImmutableMap.of("_ENTITIES", entityExtensionResult));
    when(serviceProviderMock.query(any(ExecutionInput.class), any(GraphQLContext.class)))
            .thenReturn(CompletableFuture.completedFuture(serviceResponseMap));

    KeyDirectiveMetadata keyDirectiveMetadataMock = mock(KeyDirectiveMetadata.class);
    when(keyDirectiveMetadataMock.getFieldSet())
            .thenReturn(ImmutableSet.of(Field.newField("id").build()));
    when(entityExtensionMetadataMock.getKeyDirectives())
            .thenReturn(Collections.singletonList(keyDirectiveMetadataMock));
    when(entityExtensionMetadataMock.getTypeName()).thenReturn("TestEntityType");
    when(entityExtensionMetadataMock.getServiceProvider()).thenReturn(serviceProviderMock);
    when(entityExtensionMetadataMock.getRequiredFields("newField"))
            .thenReturn(ImmutableSet.of(Field.newField("reqField").build()));

    when(dataFetchingEnvironmentMock.getContext()).thenReturn(graphQLContextMock);
    when(dataFetchingEnvironmentMock.getSource()).thenReturn(testDfeSource);
    when(dataFetchingEnvironmentMock.getField()).thenReturn(testDfeField);

    try {
      subjectUnderTest.get(dataFetchingEnvironmentMock).get();
      Assert.fail();
    } catch (Exception ex) {
      if(ex.getCause() instanceof EntityFetchingException) {
        EntityFetchingException cause = (EntityFetchingException) ex.getCause();
        assertThat(cause.getErrorType()).isEqualTo(ErrorType.DataFetchingException);
        assertThat(cause.getMessage()).contains(NO_ENTITY_FIELD);
      } else {
        Assert.fail();
      }
    }
  }


  public void load_NonAnnotatedFieldsOfEntityTypeAreSelected_success() {
    // future use case
  }

  // TODO edge cases
  //  fragments / inlinefragments
  //  entity extension additional fields other types: object/interface/non string primitive types
  // etc

}
