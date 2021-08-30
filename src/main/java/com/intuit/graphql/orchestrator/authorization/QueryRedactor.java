package com.intuit.graphql.orchestrator.authorization;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;

import com.intuit.graphql.orchestrator.common.FieldPosition;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.language.Field;
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
      TreeTransformerUtil.deleteNode(queryVisitorFieldEnvironment.getTraverserContext());
    }

    FieldPosition fieldPosition = createFieldPosition(queryVisitorFieldEnvironment);
    if (requiresFieldAuthorization(fieldPosition) && !isFieldAccessAllowed(fieldPosition, fieldArguments)) {
      TreeTransformerUtil.deleteNode(queryVisitorFieldEnvironment.getTraverserContext());
      this.fieldAccessDeclined = true;
      return TraversalControl.QUIT;
    }

    return TraversalControl.CONTINUE;
  }

  private FieldPosition createFieldPosition(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
    Field field = queryVisitorFieldEnvironment.getField();
    GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) GraphQLTypeUtil
        .unwrapAll(queryVisitorFieldEnvironment.getParentType());
    return new FieldPosition(parentType.getName(), field.getName());
  }

  private boolean requiresFieldAuthorization(FieldPosition fieldPosition) {
    return this.fieldAuthorization.requiresAccessControl(fieldPosition);
  }

  private boolean isFieldAccessAllowed(FieldPosition fieldPosition, Map<String, Object> fieldArguments) {

    FieldAuthorizationEnvironment<AuthDataT> fieldAuthorizationEnvironment = FieldAuthorizationEnvironment.<AuthDataT>builder()
            .fieldPosition(fieldPosition)
            .fieldArguments(fieldArguments)
            .authData(authData)
            .build();

    return this.fieldAuthorization.isAccessAllowed(fieldAuthorizationEnvironment);
  }

}
