package com.intuit.graphql.orchestrator.batch;

import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorFragmentSpreadEnvironment;
import graphql.analysis.QueryVisitorInlineFragmentEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.Value;
import graphql.language.VariableReference;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class provides assistance in extracting all VariableReference names used in GraphQL nodes.
 */
public class VariableDefinitionFilter {

  /**
   * Traverses a GraphQL Node and returns all VariableReference names used in all nodes in the graph.
   *
   * @param graphQLSchema   The GraphQL Schema, to be referenced in the {@code QueryTraverser}
   * @param rootType        GraphQL Type of argument {@code rootNode}
   * @param fragmentsByName Operation fragment definitions by name, to be referenced in the {@code QueryTraverser}
   * @param variables       Operation variables, to be referenced in the {@code QueryTraverser}
   * @param rootNode        The root node of your graph (traversal starts here)
   * @return A Set of all variable reference names used in the provided {@code rootNode}. Note that the variable
   * reference indicator prefix '$' will be <b>excluded</b> in the result.
   */
  public Set<String> getVariableReferencesFromNode(GraphQLSchema graphQLSchema, GraphQLObjectType rootType,
                                                   Map<String, FragmentDefinition> fragmentsByName, Map<String, Object> variables, Node<?> rootNode) {
    final VariableReferenceVisitor variableReferenceVisitor = new VariableReferenceVisitor();

    //need to utilize a better pattern for creating mockable QueryTraverser/QueryTransformer
    QueryTraverser queryTraverser = QueryTraverser.newQueryTraverser()
            .schema(graphQLSchema)
            .rootParentType(rootType) //need to support also for subscription
            .fragmentsByName(fragmentsByName)
            .variables(variables)
            .root(rootNode)
            .build();

    queryTraverser.visitPreOrder(variableReferenceVisitor);

    List<OperationDefinition> operationDefinitions = new ArrayList<>();

    if (rootNode instanceof Document) {
      Document document = (Document) rootNode;
      operationDefinitions.addAll(document.getDefinitionsOfType(OperationDefinition.class));
    } else if (rootNode instanceof OperationDefinition) {
      operationDefinitions.add((OperationDefinition) rootNode);
    }

    Set<VariableReference> additionalReferences = operationDirectiveVariableReferences(operationDefinitions);

    return Stream.concat(variableReferenceVisitor.getVariableReferences().stream(), additionalReferences.stream())
            .map(VariableReference::getName).collect(Collectors.toSet());
  }

  private Set<VariableReference> operationDirectiveVariableReferences(List<OperationDefinition> operationDefinitions) {
    final List<Value> values = operationDefinitions.stream()
            .flatMap(operationDefinition -> operationDefinition.getDirectives().stream())
            .flatMap(directive -> directive.getArguments().stream())
            .map(Argument::getValue)
            .collect(Collectors.toList());

    VariableReferenceExtractor extractor = new VariableReferenceExtractor();
    extractor.captureVariableReferences(values);

    return extractor.getVariableReferences();
  }


  /**
   * This visitor accepts all nodes in a graph and inspects arguments for VariableReference instances.
   *
   * The location of arguments can be in the following:
   *
   * <ul>
   *   <li>Field arguments</li>
   *   <li>Field directive arguments</li>
   *   <li>Fragment Spread directive arguments</li>
   *   <li>Inline Fragment directive arguments</li>
   *   <li>Fragment Definition directive arguments</li>
   *   <li>Argument Values</li>
   * </ul>
   */
  @SuppressWarnings("rawtypes")
  @Getter
  static class VariableReferenceVisitor extends QueryVisitorStub {

    private final VariableReferenceExtractor variableReferenceExtractor = new VariableReferenceExtractor();

    public Set<VariableReference> getVariableReferences() {
      return variableReferenceExtractor.getVariableReferences();
    }

    @Override
    public void visitField(final QueryVisitorFieldEnvironment env) {
      final Field field = env.getField();

      if (field.getArguments().isEmpty() && field.getDirectives().isEmpty()) {
        return;
      }

      final Stream<Argument> directiveArgumentStream = field.getDirectives().stream()
              .flatMap(directive -> directive.getArguments().stream());

      final Stream<Argument> fieldArgumentStream = field.getArguments().stream();

      captureVariableReferences(Stream.concat(fieldArgumentStream, directiveArgumentStream));
    }

    @Override
    public void visitInlineFragment(final QueryVisitorInlineFragmentEnvironment env) {
      InlineFragment inlineFragment = env.getInlineFragment();

      if (inlineFragment.getDirectives().isEmpty()) {
        return;
      }

      Stream<Argument> arguments = env.getInlineFragment().getDirectives().stream()
              .flatMap(directive -> directive.getArguments().stream());

      captureVariableReferences(arguments);
    }

    @Override
    public void visitFragmentSpread(final QueryVisitorFragmentSpreadEnvironment env) {
      FragmentDefinition fragmentDefinition = env.getFragmentDefinition();
      FragmentSpread fragmentSpread = env.getFragmentSpread();

      if (fragmentDefinition.getDirectives().isEmpty() && fragmentSpread.getDirectives().isEmpty()) {
        return;
      }

      final Stream<Argument> allArguments = Stream.concat(
              fragmentDefinition.getDirectives().stream(),
              fragmentSpread.getDirectives().stream()
      ).flatMap(directive -> directive.getArguments().stream());

      captureVariableReferences(allArguments);
    }

    private void captureVariableReferences(Stream<Argument> arguments) {
      final List<Value> values = arguments.map(Argument::getValue)
              .collect(Collectors.toList());

      variableReferenceExtractor.captureVariableReferences(values);
    }
  }
}
