package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.batch.AuthDownstreamQueryModifier;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.utils.SelectionCollector;
import graphql.language.AstTransformer;
import graphql.language.FragmentDefinition;
import graphql.language.Node;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import lombok.Builder;
import lombok.NonNull;

import java.util.Map;

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
    AuthDownstreamQueryModifier downstreamQueryModifier = createQueryModifier();
    Node<?> transformedRoot = AST_TRANSFORMER.transform(root, downstreamQueryModifier);
    return new DownstreamQueryRedactorResult(
        transformedRoot,
        downstreamQueryModifier.getDeclineFieldErrors(),
        downstreamQueryModifier.redactedQueryHasEmptySelectionSet(),
        downstreamQueryModifier.getFragmentSpreadsRemoved()
      );
  }

  private AuthDownstreamQueryModifier createQueryModifier() {
    Map<String, FragmentDefinition> fragmentsByName = dataFetchingEnvironment.getFragmentsByName();
    return AuthDownstreamQueryModifier.builder()
        .rootParentType(rootParentType)
        .fieldAuthorization(fieldAuthorization)
        .graphQLContext(dataFetchingEnvironment.getContext())
        .queryVariables(dataFetchingEnvironment.getVariables())
        .graphQLSchema(dataFetchingEnvironment.getGraphQLSchema())
        .selectionCollector(new SelectionCollector(fragmentsByName))
        .serviceMetadata(serviceMetadata)
        .authData(authData)
        .build();
  }
}
