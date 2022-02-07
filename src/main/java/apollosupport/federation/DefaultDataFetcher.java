package apollosupport.federation;

import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.Data;

/**
 * This {@link DataFetcher} is used to fetch fields for an Apollo Federation compliant sub-graph
 */
@Data
public class DefaultDataFetcher implements DataFetcher<Object> {

  private SubGraphContext subGraphContext;

  @Override
  public Object get(final DataFetchingEnvironment environment) {
    String dfeFieldName = environment.getField().getName();
    GraphQLContext context = environment.getContext();
    context.put(dfeFieldName, subGraphContext);
    return environment
        .getDataLoader(subGraphContext.getServiceMetadata().getServiceProvider().getNameSpace())
        .load(environment);
  }

}
