package com.intuit.graphql.orchestrator.authorization;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLTypeUtil;
import graphql.util.TraversalControl;
import graphql.util.TreeTransformerUtil;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Builder
public class QueryRedactor<AuthDataT> extends QueryVisitorStub {

  private AuthDataT authData;
  private FieldAuthorization<AuthDataT> fieldAuthorization;

  @Getter
  private boolean fieldAccessDeclined;

  @Override
  public TraversalControl visitFieldWithControl(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
    Map<String, Object> fieldArguments = queryVisitorFieldEnvironment.getArguments();
    if (hasResolverDirective(queryVisitorFieldEnvironment.getFieldDefinition())) {
      return TreeTransformerUtil.deleteNode(queryVisitorFieldEnvironment.getTraverserContext());
    }

    if (queryVisitorFieldEnvironment.getParentType() instanceof GraphQLFieldsContainer) {
      FieldCoordinates fieldCoordinates = createFieldCoordinates(queryVisitorFieldEnvironment);
      if (requiresFieldAuthorization(fieldCoordinates) && !isFieldAccessAllowed(fieldCoordinates, fieldArguments)) {
        TreeTransformerUtil.deleteNode(queryVisitorFieldEnvironment.getTraverserContext());
        this.fieldAccessDeclined = true;
        return TraversalControl.QUIT;
      }
    }

    return TraversalControl.CONTINUE;
  }

  private FieldCoordinates createFieldCoordinates(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
    Field field = queryVisitorFieldEnvironment.getField();
    GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) GraphQLTypeUtil
        .unwrapAll(queryVisitorFieldEnvironment.getParentType());
    return FieldCoordinates.coordinates(parentType.getName(), field.getName());
  }

  private boolean requiresFieldAuthorization(FieldCoordinates fieldCoordinates) {
    return this.fieldAuthorization.requiresAccessControl(fieldCoordinates);
  }

  private boolean isFieldAccessAllowed(FieldCoordinates fieldCoordinates, Map<String, Object> fieldArguments) {

    FieldAuthorizationEnvironment<AuthDataT> fieldAuthorizationEnvironment = FieldAuthorizationEnvironment.<AuthDataT>builder()
            .fieldCoordinates(fieldCoordinates)
            .fieldArguments(fieldArguments)
            .authData(authData)
            .build();

    return this.fieldAuthorization.isAccessAllowed(fieldAuthorizationEnvironment);
  }

}
