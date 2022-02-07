package com.intuit.graphql.orchestrator.apollofederation;


import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
@Data
public class SubGraphContext {

  public static final String DELIMITER = ":";

  private final FieldDefinition fieldDefinition;
  private final TypeDefinition parentTypeDefinition;
  private final boolean requiresTypeNameInjection;
  private final ServiceMetadata serviceMetadata;

  // TODO see if these are needed
  // private final GraphQLUnionType entityUnion ;
  // private final Map<String, GraphQLObjectType> ownedEntities = new HashMap<>();
  // private final Map<String, GraphQLObjectType> extendedEntities = new HashMap<>();

  public String createDataLoaderKey() {
    String serviceNamespace = serviceMetadata.getServiceProvider().getNameSpace();
    String parentTypename = getParentTypeDefinition().getName();
    String fieldName = getFieldDefinition().getName();
    return createDataLoaderKey(serviceNamespace, parentTypename, fieldName);
  }

  private static String createDataLoaderKey(String... tokens) {
    return StringUtils.join(tokens, DELIMITER);
  }
}
