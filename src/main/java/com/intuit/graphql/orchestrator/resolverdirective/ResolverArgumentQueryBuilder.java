package com.intuit.graphql.orchestrator.resolverdirective;

import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.unwrapAll;
import static graphql.schema.GraphQLTypeUtil.isLeaf;

import graphql.language.OperationDefinition;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLType;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * This class is responsible for parsing a resolver directive "field" argument and producing a OperationDefinition that
 * can be used to call a resolver service.
 */
public class ResolverArgumentQueryBuilder {

  /**
   * Parses a resolver directive field and produces an OperationDefinition using the GraphQL-java {@code Parser} class
   * for validation and object generation.
   *
   * This method attaches the entire {@code nestedType} underneath the last node found in {@code resolverField}.
   *
   * @param resolverField dot-separated string that represents the path to the {@code nestedType} from the Query root
   * @param nestedType    the input type that exists under the leaf node found in {@code resolverField}
   * @return an OperationDefinition that can be used to issue queries to a resolver service
   */
  public OperationDefinition buildQuery(String resolverField, GraphQLInputType nestedType) {
    int parens = 0;

    StringBuilder query = new StringBuilder("{");
    for (char c : resolverField.toCharArray()) {
      if (c == '.') {
        parens += 1;
        query.append('{');
      } else {
        query.append(c);
      }
    }

    if (!isLeaf(nestedType)) {
      query.append("{");
      applyInputObjectToQuery(query, nestedType);
      query.append("}");
    }

    query.append(StringUtils.repeat('}', parens + 1)); // + 1 to count for first open bracket

    try {
      return new Parser().parseDocument(query.toString()).getDefinitionsOfType(OperationDefinition.class).get(0);
    } catch (InvalidSyntaxException e) {
      throw new NotAValidFieldReference(resolverField);
    }
  }

  private void applyInputObjectToQuery(StringBuilder sb, GraphQLInputType type) {
    GraphQLType unwrappedType = unwrapAll(type);
    final List<GraphQLInputObjectField> fieldDefinitions = unwrappedType.getChildren().stream()
        .filter(t -> t instanceof GraphQLInputObjectField)
        .map(t -> (GraphQLInputObjectField) t)
        .collect(Collectors.toList());

    fieldDefinitions.forEach(fieldDefinition -> {
      sb.append(fieldDefinition.getName());

      if (!isLeaf(fieldDefinition.getType())) {
        sb.append("{");
        applyInputObjectToQuery(sb, fieldDefinition.getType());
        sb.append("}");
      } else {
        sb.append(" "); //okay to carry over since it will be parsed and validated
      }
    });
  }
}
