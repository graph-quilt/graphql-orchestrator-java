package apollosupport.federation;

import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.Data;

/**
 * A {@link DataFetcher} for an Apollo Federation compliant sub-graph.
 *
 * A sub-graph may own entites and/or extended entities owned by other subgraph.  This
 * metadata is available at {@link SubGraphContext}
 */
@Data
public class SubGraphDataFetcher implements DataFetcher<Object> {

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
