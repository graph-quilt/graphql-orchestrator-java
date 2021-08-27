package com.intuit.graphql.orchestrator.authorization;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.common.ArgumentValueResolver;
import com.intuit.graphql.orchestrator.common.FieldPosition;
import graphql.GraphQLContext;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.language.Field;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLSchema;
import graphql.util.TreeTransformerUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;

@AllArgsConstructor
public class RedactingVisitor<ClaimT> extends QueryVisitorStub {

  private final Pair<String, Object> claimData;
  private final AuthorizationContext<ClaimT> authorizationContext;
  private final GraphQLContext graphQLContext;
  private final ArgumentValueResolver argumentValueResolver;
  private final Map<String, Object> variables;
  private final GraphQLSchema graphQLSchema;


  @Override
  public void visitField(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {

    Field field = queryVisitorFieldEnvironment.getField();

    GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) queryVisitorFieldEnvironment.getParentType();

    FieldPosition fieldPosition = new FieldPosition(parentType.getName(), field.getName());
    GraphQLFieldDefinition fieldDefinition = queryVisitorFieldEnvironment.getFieldDefinition();

    if (hasResolverDirective(fieldDefinition)) {
      TreeTransformerUtil.deleteNode(queryVisitorFieldEnvironment.getTraverserContext());
    }

    if (requiresFieldAuthorization(fieldPosition) && !fieldAccessIsAllowed(fieldPosition, field, fieldDefinition, variables)) {
      TreeTransformerUtil.deleteNode(queryVisitorFieldEnvironment.getTraverserContext());
    }
  }

  private boolean requiresFieldAuthorization(FieldPosition fieldPosition) {
    return authorizationContext.getFieldAuthorization().requiresAccessControl(fieldPosition);
  }

  private boolean fieldAccessIsAllowed(FieldPosition fieldPosition, Field field, GraphQLFieldDefinition fieldDefinition,
                                       Map<String, Object> variables) {
    Map<String, Object> authData = ImmutableMap.of(
            claimData.getLeft(), claimData.getRight(),
            "fieldArguments", argumentValueResolver.resolve(graphQLSchema, fieldDefinition, field, variables)
    );

    FieldAuthorizationRequest fieldAuthorizationRequest = FieldAuthorizationRequest.builder()
            .fieldPosition(fieldPosition)
            .clientId(this.authorizationContext.getClientId())
            .graphQLContext(graphQLContext)
            .authData(authData)
            .build();

    return authorizationContext.getFieldAuthorization().isAccessAllowed(fieldAuthorizationRequest);
  }

}
