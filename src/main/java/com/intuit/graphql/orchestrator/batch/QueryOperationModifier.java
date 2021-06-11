package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.isInterfaceOrUnionType;
import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.unwrapAll;

import graphql.analysis.QueryTransformer;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLSchema;
import graphql.util.TreeTransformerUtil;
import java.util.Map;

public class QueryOperationModifier {

  private static final InterfaceModifierQueryVisitor interfaceModifierQueryVisitor = new InterfaceModifierQueryVisitor();

  public OperationDefinition modifyQuery(GraphQLSchema graphQLSchema, OperationDefinition operationDefinition,
      Map<String, FragmentDefinition> fragmentsByName, Map<String, Object> variables) {

    QueryTransformer queryTransformer = QueryTransformer.newQueryTransformer()
        .schema(graphQLSchema)
        .root(operationDefinition)
        .rootParentType(operationDefinition.getOperation() == Operation.QUERY ? graphQLSchema.getQueryType()
            : graphQLSchema.getMutationType())
        .fragmentsByName(fragmentsByName)
        .variables(variables).build();

    return (OperationDefinition) queryTransformer.transform(interfaceModifierQueryVisitor);
  }

  public static class InterfaceModifierQueryVisitor extends QueryVisitorStub {

    static final String TypenameFieldName = "__typename";

    @Override
    public void visitField(final QueryVisitorFieldEnvironment env) {
      if (isInterfaceOrUnionType(unwrapAll(env.getFieldDefinition().getType())) && isMissingTypenameSelection(
          env.getField())) {
        SelectionSet selectionSetWithTypename = env.getField().getSelectionSet().transform(
            builder -> builder.selection(new Field(TypenameFieldName))
        );
        Field fieldWithTypename = env.getField().transform(builder -> builder.selectionSet(selectionSetWithTypename));
        TreeTransformerUtil.changeNode(env.getTraverserContext(), fieldWithTypename);
      }
    }

    private boolean isMissingTypenameSelection(Field field) {
      return field.getSelectionSet().getSelectionsOfType(Field.class).stream()
          .noneMatch(f -> f.getName().equals(TypenameFieldName));
    }
  }
}
