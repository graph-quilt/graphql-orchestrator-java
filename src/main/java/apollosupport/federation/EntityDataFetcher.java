package apollosupport.federation;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDataLoaderUtil.createDataLoaderKey;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.Data;

/**
 * A {@link DataFetcher} for a field added to the Entity.  Note that each field added by
 * a remote SubGrapph may have their own required fields which shall be collected into an
 * {@link EntityInfo}
 */
@Data
public class EntityDataFetcher implements DataFetcher<Object> {

  private EntityInfo entityInfo;

  @Override
  public Object get(DataFetchingEnvironment environment) throws Exception {
    return environment
        .getDataLoader(createDataLoaderKeyFrom(entityInfo, environment))
        .load(environment);
  }

  public static String createDataLoaderKeyFrom(EntityInfo entityInfo, DataFetchingEnvironment environment) {
    String serviceNamespace = entityInfo.getServiceMetadata().getServiceProvider().getNameSpace();
    String parentTypename = entityInfo.getEntityTypeName();
    String fieldname = environment.getFieldDefinition().getName();
    return createDataLoaderKey(serviceNamespace, parentTypename, fieldname);
  }

}
