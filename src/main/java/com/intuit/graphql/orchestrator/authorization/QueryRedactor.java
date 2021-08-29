package com.intuit.graphql.orchestrator.authorization;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.hasResolverDirective;

import com.intuit.graphql.orchestrator.common.FieldPosition;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.language.Field;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLTypeUtil;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
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
    Field field = queryVisitorFieldEnvironment.getField();
    Map<String, Object> fieldArguments = queryVisitorFieldEnvironment.getArguments();
    FieldPath fieldPath = createFieldPath(queryVisitorFieldEnvironment);
    queryVisitorFieldEnvironment.getTraverserContext().setVar(FieldPath.class, fieldPath);

    if (hasResolverDirective(queryVisitorFieldEnvironment.getFieldDefinition())) {
      TreeTransformerUtil.deleteNode(queryVisitorFieldEnvironment.getTraverserContext());
    }

    FieldPosition fieldPosition = createFieldPosition(queryVisitorFieldEnvironment, fieldPath);
    if (requiresFieldAuthorization(fieldPosition) && !isFieldAccessAllowed(fieldPosition, fieldArguments)) {
      TreeTransformerUtil.deleteNode(queryVisitorFieldEnvironment.getTraverserContext());
      this.fieldAccessDeclined = true;
      return TraversalControl.QUIT;
    }

    return TraversalControl.CONTINUE;
  }

  private FieldPosition createFieldPosition(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment,
      FieldPath fieldPath) {
    Field field = queryVisitorFieldEnvironment.getField();
    GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) GraphQLTypeUtil
        .unwrapAll(queryVisitorFieldEnvironment.getParentType());
    return new FieldPosition(parentType.getName(), field.getName(), fieldPath);
  }

  private FieldPath createFieldPath(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
    Field field = queryVisitorFieldEnvironment.getField();
    FieldPath fieldPath;
    if (isRoot(queryVisitorFieldEnvironment)) {
      fieldPath = new FieldPath(field.getName());
    } else {
      FieldPath parentFieldPath = getParentFieldPath(queryVisitorFieldEnvironment);
      fieldPath = parentFieldPath.createChildPath(field.getName());
    }
    return fieldPath;
  }

  private FieldPath getParentFieldPath(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
    return queryVisitorFieldEnvironment.getParentEnvironment().getTraverserContext().getVar(FieldPath.class);
  }

  private boolean isRoot(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
    TraverserContext<?> currentTraverserCtx = queryVisitorFieldEnvironment.getTraverserContext();
    TraverserContext<?> parentTraverserCtx = currentTraverserCtx.getParentContext();
    return parentTraverserCtx.isRootContext();
  }

  private boolean requiresFieldAuthorization(FieldPosition fieldPosition) {
    return this.fieldAuthorization.requiresAccessControl(fieldPosition);
  }

  private boolean isFieldAccessAllowed(FieldPosition fieldPosition, Map<String, Object> fieldArguments) {

    FieldAuthorizationRequest<AuthDataT> fieldAuthorizationRequest = FieldAuthorizationRequest.<AuthDataT>builder()
            .fieldPosition(fieldPosition)
            .fieldArguments(fieldArguments)
            .authData(authData)
            .build();

    return this.fieldAuthorization.isAccessAllowed(fieldAuthorizationRequest);
  }

}
