package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class FragmentDefinitionRedactor {

  @NonNull
  private FragmentDefinition fragmentDefinition;

  @NonNull
  private GraphQLType typeCondition;

  @NonNull
  private Object authData;

  @NonNull
  private BatchFieldAuthorization fieldAuthorization;

  @NonNull
  private DataFetchingEnvironment dataFetchingEnvironment;

  @NonNull
  private ServiceMetadata serviceMetadata;

  public FragmentDefinitionRedactorResult redact() {
    //GraphQLFieldsContainer rootFieldType = (GraphQLFieldsContainer) unwrapAll(typeCondition);
      return null; // TODO
  }

//  private QueryRedactorResult redactField(Field field, Object authData, BatchFieldAuthorization fieldAuthorization,
//      DataFetchingEnvironment key) {
//
//    QueryRedactor queryRedactor = QueryRedactor.builder()
//        .authData(authData)
//        .fieldAuthorization(fieldAuthorization)
//        .build();
//
//    QueryTransformer queryTransformer = QueryTransformer.newQueryTransformer()
//        .schema(dataFetchingEnvironment.getGraphQLSchema())
//        .variables(dataFetchingEnvironment.getVariables())
//        .fragmentsByName(dataFetchingEnvironment.getFragmentsByName())
//        .rootParentType((GraphQLFieldsContainer)parentType)
//        .root(origField)
//        .build();
//
//    Field transformedField = (Field) queryTransformer.transform(queryRedactor);
//    if (queryRedactor.isFieldAccessDeclined()) {
//      throw fieldAuthorization.getDeniedGraphQLErrorException();
//    }
//    return transformedField;
//
//  }
}
