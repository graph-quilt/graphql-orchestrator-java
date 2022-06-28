package com.intuit.graphql.orchestrator.federation;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.CompletableFuture;

import static com.intuit.graphql.orchestrator.batch.DataLoaderKeyUtil.createDataLoaderKey;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityDataFetcherTest {

  private final String entityName = "MockEntity";

  @Mock private DataFetchingEnvironment dataFetchingEnvironmentMock;

  @Mock private DataLoader dataLoaderMock;

  private EntityDataFetcher subjectUnderTest;

  @Before
  public void setup() {
    subjectUnderTest =  new EntityDataFetcher(entityName);
  }

  @Test
  public void load_entityBatchFetcher_Success() throws Exception {
    String requestedEntityFieldName = "ExtEntityField";
    Field requestedEntityField = Field.newField().name(requestedEntityFieldName).alias("AliasedName").build();

    when(dataLoaderMock.load(dataFetchingEnvironmentMock)).thenReturn(CompletableFuture.completedFuture("Success"));
    when(dataFetchingEnvironmentMock.getField()).thenReturn(requestedEntityField);
    when(dataFetchingEnvironmentMock.getDataLoader(anyString())).then(invocationOnMock -> {
      if(invocationOnMock.getArgument(0).equals(createDataLoaderKey(entityName, requestedEntityFieldName))) {
        return dataLoaderMock;
      }
      throw new RuntimeException("Failed to get the batch data loader; key has changed");
    });

    CompletableFuture<Object> result = subjectUnderTest.get(dataFetchingEnvironmentMock);

    assert result.isDone();
    assert result.get().equals("Success");
  }
}
