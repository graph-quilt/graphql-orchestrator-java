package com.intuit.graphql.orchestrator.datafetcher;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.schema.GraphQLFieldsContainer;
import graphql.util.TraversalControl;
import graphql.util.TreeTransformerUtil;
import java.util.List;

/**
 * This class is responsible for appending an argument list to a given field in a query.
 */
public class ArgumentAppenderVisitor extends QueryVisitorStub {

  private String expectedContainerTypeName;
  private String expectedFieldName;
  private List<Argument> arguments;

  public ArgumentAppenderVisitor(final String expectedContainerTypeName, final String expectedFieldName,
      final List<Argument> arguments) {
    this.expectedContainerTypeName = expectedContainerTypeName;
    this.expectedFieldName = expectedFieldName;
    this.arguments = arguments;
  }

  @Override
  public TraversalControl visitFieldWithControl(final QueryVisitorFieldEnvironment env) {
    if (!env.isTypeNameIntrospectionField() && isExpectedField(env.getFieldsContainer(), env.getField())) {
      Field newFieldWithArguments = env.getField().transform(builder -> builder.arguments(arguments));

      TreeTransformerUtil.changeNode(env.getTraverserContext(), newFieldWithArguments);

      return TraversalControl.QUIT;
    }

    return TraversalControl.CONTINUE;
  }

  private boolean isExpectedField(GraphQLFieldsContainer containerType, Field field) {
    return this.expectedFieldName.equals(field.getName()) && expectedContainerTypeName
        .equals(containerType.getName());
  }
}
