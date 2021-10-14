package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.common.ArgumentValueResolver;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.language.AstTransformer;
import graphql.language.Field;
import graphql.language.Node;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLType;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;

@Builder
public class QueryRedactor {

  private static final ArgumentValueResolver ARGUMENT_VALUE_RESOLVER = new ArgumentValueResolver();

  private static final AstTransformer AST_TRANSFORMER = new AstTransformer();

  @NonNull
  private Node<?> root;

  @NonNull
  private GraphQLType rootType;

  @NonNull
  private GraphQLType rootParentType;

  private Object authData;

  @NonNull
  private FieldAuthorization fieldAuthorization;

  @NonNull
  private DataFetchingEnvironment dataFetchingEnvironment;

  @NonNull
  private ServiceMetadata serviceMetadata;

  public QueryRedactorResult redact() {
    SelectionSetRedactor selectionSetRedactor = SelectionSetRedactor.builder()
        .rootFieldType((GraphQLFieldsContainer) rootType)
        .rootFieldParentType((GraphQLFieldsContainer)rootParentType)
        .authData(authData)
        .fieldAuthorization(fieldAuthorization)
        .graphQLContext(dataFetchingEnvironment.getContext())
        .argumentValueResolver(ARGUMENT_VALUE_RESOLVER)
        .variables(dataFetchingEnvironment.getVariables())
        .graphQLSchema(dataFetchingEnvironment.getGraphQLSchema())
        .build();

    Node<?> transformedRoot = AST_TRANSFORMER.transform(root, selectionSetRedactor);
    List<DeclinedField> declinedFields = selectionSetRedactor.getDeclineFields();
    if ( CollectionUtils.isNotEmpty(declinedFields)) {
     // TODO collect errors
    }

    return QueryRedactorResult.builder()
        .node(transformedRoot)
        .build();
  }

}
