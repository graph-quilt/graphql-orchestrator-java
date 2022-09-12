package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.batch.DownstreamQueryModifier;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.utils.SelectionCollector;
import graphql.language.AstTransformer;
import graphql.language.Node;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class DownstreamQueryRedactor {

  private static final AstTransformer AST_TRANSFORMER = new AstTransformer();

  @NonNull private Node<?> root;
  @NonNull private GraphQLType rootType;
  @NonNull private GraphQLType rootParentType;
  @NonNull private FieldAuthorization fieldAuthorization;
  @NonNull private DataFetchingEnvironment dataFetchingEnvironment;
  @NonNull private ServiceMetadata serviceMetadata;
  private Object authData; // null and unused in case of DefaultFieldAuthorization

  public DownstreamQueryRedactorResult redact() {
    DownstreamQueryModifier downstreamQueryModifier = createQueryModifier();
    Node<?> transformedRoot = AST_TRANSFORMER.transform(root, downstreamQueryModifier);
    if (downstreamQueryModifier.redactedQueryHasEmptySelectionSet()) {
      throw DownstreamCreateQueryException.builder()
          .message("Downstream query result has empty selection set.")
          .extension("emptySelectionSets", downstreamQueryModifier.getEmptySelectionSets()
              .stream()
              .map(SelectionSetMetadata::getSelectionSetPath)
              .collect(Collectors.toList())
          )
          .build();
    } else {
      return new DownstreamQueryRedactorResult(transformedRoot, downstreamQueryModifier
          .getDeclineFieldErrors());
    }
  }

  private DownstreamQueryModifier createQueryModifier() {
      return DownstreamQueryModifier.builder()
          .rootType(rootType)
          .serviceMetadata(serviceMetadata)
          .selectionCollector(new SelectionCollector(dataFetchingEnvironment.getFragmentsByName()))
          .fieldAuthorization(fieldAuthorization)
          .authData(authData)
          .graphQLContext(dataFetchingEnvironment.getContext())
          .queryVariables(dataFetchingEnvironment.getVariables())
          .graphQLSchema(dataFetchingEnvironment.getGraphQLSchema())
          .build();
    }
}
