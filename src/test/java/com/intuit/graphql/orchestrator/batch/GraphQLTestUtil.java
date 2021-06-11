package com.intuit.graphql.orchestrator.batch;

import static graphql.execution.ExecutionPath.rootPath;
import static graphql.execution.ExecutionStepInfo.newExecutionStepInfo;
import static graphql.execution.MergedField.newMergedField;
import static graphql.schema.GraphQLObjectType.newObject;

import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitor;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorFragmentSpreadEnvironment;
import graphql.analysis.QueryVisitorInlineFragmentEnvironment;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.schema.GraphQLSchema;
import graphql.util.TraverserContext.Phase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GraphQLTestUtil {

  public static class PassthroughQueryModifier extends QueryOperationModifier {

    @Override
    public OperationDefinition modifyQuery(final GraphQLSchema graphQLSchema, final OperationDefinition operationDefinition,
        final Map<String, FragmentDefinition> fragmentsByName, final Map<String, Object> variables) {
      return operationDefinition;
    }
  }

  private static class FieldNameCollector implements QueryVisitor {

    private String fieldPrefix = "";

    private List<String> fieldNames = new ArrayList<>();


    @Override
    public void visitField(final QueryVisitorFieldEnvironment env) {
      fieldNames.add(fieldPrefix + env.getField().getName());
    }

    @Override
    public void visitInlineFragment(final QueryVisitorInlineFragmentEnvironment env) {
      if (env.getTraverserContext().getPhase() == Phase.ENTER) {
        fieldPrefix += "inline:";
      } else if (env.getTraverserContext().getPhase() == Phase.LEAVE) {
        fieldPrefix = fieldPrefix.substring(0, fieldPrefix.lastIndexOf("inline:"));
      }
    }

    @Override
    public void visitFragmentSpread(final QueryVisitorFragmentSpreadEnvironment env) {
      if (env.getTraverserContext().getPhase() == Phase.ENTER) {
        fieldPrefix += "spread:";
      } else if (env.getTraverserContext().getPhase() == Phase.LEAVE) {
        fieldPrefix = fieldPrefix.substring(0, fieldPrefix.lastIndexOf("spread:"));
      }
    }

    public List<String> getFieldNames() {
      return fieldNames;
    }
  }

  public static List<String> printPreOrder(Node n, GraphQLSchema s,
      Map<String, FragmentDefinition> fragmentDefinitions) {
    QueryTraverser queryTraverser = QueryTraverser.newQueryTraverser()
        .fragmentsByName(fragmentDefinitions)
        .root(n)
        .rootParentType(s.getQueryType())
        .schema(s)
        .variables(new HashMap<>()).build();

    FieldNameCollector fieldNameCollector = new FieldNameCollector();

    queryTraverser.visitPreOrder(fieldNameCollector);

    return fieldNameCollector.getFieldNames();
  }

  public static ExecutionStepInfo buildCompleteExecutionStepInfo(Document document, String... path) {

    OperationDefinition query = document.getDefinitionsOfType(OperationDefinition.class).get(0);
    Map<String, FragmentDefinition> fragmentDefinitions = document.getDefinitionsOfType(FragmentDefinition.class)
        .stream().collect(Collectors.toMap(FragmentDefinition::getName, Function.identity()));

    ExecutionStepInfo parent = newExecutionStepInfo()
        .type(newObject().name("FakeType").build())
        .path(rootPath())
        .build();

    Queue<String> paths = new LinkedList<>(Arrays.asList(path));

    Stack<ExecutionStepInfo> hierarchy = new Stack<>();

    hierarchy.push(parent);

    for (final Selection selection : query.getSelectionSet().getSelections()) {
      final Field field = (Field) selection;
      if (field.getName().equals(paths.peek())) {
        paths.poll();
        buildExecutionStepInfo(field, parent, fragmentDefinitions, "", hierarchy, paths);
      }
    }

    return hierarchy.pop();
  }

  private static void buildExecutionStepInfo(Field f, ExecutionStepInfo parent,
      Map<String, FragmentDefinition> fragmentDefinitions, String accumulatedPath,
      Stack<ExecutionStepInfo> hierarchy, Queue<String> rest) {
    accumulatedPath += "/" + f.getName();
    ExecutionStepInfo result = newExecutionStepInfo()
        .parentInfo(parent)
        .field(newMergedField().addField(f).build())
        .type(newObject().name("FakeType").build())
        .path(ExecutionPath.parse(accumulatedPath))
        .build();

    hierarchy.push(result);

    if (!rest.isEmpty()) {
      for (final Selection selection : f.getSelectionSet().getSelections()) {
        if (selection instanceof Field) {
          Field field = (Field) selection;
          if (field.getName().equals(rest.peek())) {
            rest.poll();
            buildExecutionStepInfo(field, result, fragmentDefinitions, accumulatedPath, hierarchy, rest);
          }
        } else if (selection instanceof FragmentSpread) {
          final FragmentSpread fragmentSpread = (FragmentSpread) selection;
          FragmentDefinition fragmentDefinition = fragmentDefinitions.get(fragmentSpread.getName());

          for (final Selection fragmentSelection : fragmentDefinition.getSelectionSet().getSelections()) {
            final Field fragmentField = (Field) fragmentSelection;

            if (fragmentField.getName().equals(rest.peek())) {
              rest.poll();
              buildExecutionStepInfo(fragmentField, result, fragmentDefinitions, accumulatedPath, hierarchy, rest);
            }
          }
        } else if (selection instanceof InlineFragment) {
          final InlineFragment inlineFragment = (InlineFragment) selection;
          //single inline fragment only
          for (final Selection inlineFragmentSelection : inlineFragment.getSelectionSet().getSelections()) {
            final Field inlineFragmentField = (Field) inlineFragmentSelection;
            if (inlineFragmentField.getName().equals(rest.peek())) {
              rest.poll();
              buildExecutionStepInfo(inlineFragmentField, result, fragmentDefinitions, accumulatedPath, hierarchy,
                  rest);
            }
          }
        }
      }
    }

  }

}
