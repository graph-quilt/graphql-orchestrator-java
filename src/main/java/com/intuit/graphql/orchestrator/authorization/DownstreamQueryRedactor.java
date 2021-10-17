package com.intuit.graphql.orchestrator.authorization;

import graphql.language.AstTransformer;
import graphql.language.Node;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLType;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;

@Builder
public class DownstreamQueryRedactor {

  private static final AstTransformer AST_TRANSFORMER = new AstTransformer();

  @NonNull private Node<?> root;
  @NonNull private GraphQLType rootType;
  @NonNull private GraphQLType rootParentType;
  @NonNull private FieldAuthorization fieldAuthorization;
  @NonNull private DataFetchingEnvironment dataFetchingEnvironment;
  private Object authData; // null and unused in case of DefaultFieldAuthorization

  public DownstreamQueryRedactorResult redact() {
    DownstreamQueryRedactorVisitor downstreamQueryRedactorVisitor = createQueryRedactorVisitor();
    Node<?> transformedRoot = AST_TRANSFORMER.transform(root, downstreamQueryRedactorVisitor);
    List<SelectionSetFieldsCounter> emptySelectionSets = downstreamQueryRedactorVisitor.getEmptySelectionSets();
    if (CollectionUtils.isNotEmpty(emptySelectionSets)) {
      throw DownstreamCreateQueryException.builder()
          .message("Downstream query result has empty selection set.")
          .build();
    } else {
      return new DownstreamQueryRedactorResult(transformedRoot, downstreamQueryRedactorVisitor
          .getDeclineFieldErrors());
    }
  }

  private DownstreamQueryRedactorVisitor createQueryRedactorVisitor() {
      return DownstreamQueryRedactorVisitor.builder()
          .rootFieldParentType((GraphQLFieldsContainer) rootParentType)
          .fieldAuthorization(fieldAuthorization)
          .authData(authData)
          .graphQLContext(dataFetchingEnvironment.getContext())
          .variables(dataFetchingEnvironment.getVariables())
          .graphQLSchema(dataFetchingEnvironment.getGraphQLSchema())
          .build();
    }
}
