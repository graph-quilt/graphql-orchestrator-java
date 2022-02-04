package apollosupport.federation;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.dataloader.BatchLoader;

public class EntityBatchDataFetcher implements BatchLoader<DataFetchingEnvironment, DataFetcherResult<Object>>  {

  @Override
  public CompletionStage<List<DataFetcherResult<Object>>> load(List<DataFetchingEnvironment> list) {
    return null;
  }
}
