package com.intuit.graphql.orchestrator.utils;

import static graphql.schema.GraphQLTypeUtil.isNotWrapped;
import static graphql.schema.GraphQLTypeUtil.unwrapOne;

import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.language.Field;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.parser.Parser;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class contains helper methods for GraphQL types. This class will often contain modifications of already built
 * methods in graphql-java's own GraphQlTypeUtil class.
 */
public class GraphQLUtil {

  public static final Parser parser = new Parser();

  private static final String ERR_CREATE_TYPE_UNEXPECTED_TYPE = "Failed to create Type due to "
      + "unexpected GraphQL Type %s";

  private GraphQLUtil() {
  }

  public static boolean isInterfaceOrUnionType(GraphQLType type) {
    final GraphQLType nestedType = unwrapAll(type);
    return nestedType instanceof GraphQLInterfaceType || nestedType instanceof GraphQLUnionType;
  }

  /**
   * Unwraps all layers of the type or just returns the type again if its not a wrapped type
   *
   * @param type the type to unwrapOne
   * @return the underlying type
   */
  public static GraphQLType unwrapAll(GraphQLType type) {
    /*
     * See graphql-java GraphQLTypeUtil#unwrapAll. The only difference here is the removal of the cast to
     * GraphQLUnmodifiedType.
     *
     * This avoids a Class cast exception produced when passing a GraphQLTypeReference into unwrapAll
     */
    while (true) {
      if (isNotWrapped(type)) {
        return type;
      }
      type = unwrapOne(type);
    }
  }

  public static List<GraphQLError> getErrors(DataFetcherResult<Map<String, Object>> result, Field f) {
    return result.getErrors().stream()
        .filter(e -> e.getPath() == null || e.getPath().isEmpty() || f.getName()
            .equals(String.valueOf(e.getPath().get(0))))
        .collect(Collectors.toList());
  }

  public static Type createTypeBasedOnGraphQLType(GraphQLType graphQLType) {

    if (graphQLType instanceof GraphQLNamedType) {
      return TypeName.newTypeName().name(((GraphQLNamedType) graphQLType).getName()).build();
    }
    if (graphQLType instanceof GraphQLNonNull) {
      return NonNullType.newNonNullType()
          .type(createTypeBasedOnGraphQLType(((GraphQLNonNull) graphQLType).getWrappedType())).build();
    }
    if (graphQLType instanceof GraphQLList) {
      return ListType.newListType().type(createTypeBasedOnGraphQLType(((GraphQLList) graphQLType).getWrappedType()))
          .build();
    }
    throw new CreateTypeException(
        String.format(ERR_CREATE_TYPE_UNEXPECTED_TYPE, GraphQLTypeUtil.simplePrint(graphQLType)));
  }

  public static Optional<GraphQLType> getFieldType(Field field, GraphQLFieldsContainer fieldsContainer) {
    GraphQLFieldDefinition fieldDefinition = fieldsContainer.getField(field.getName());
    if (fieldDefinition == null) {
      return Optional.empty();
    }
    return Optional.of(fieldDefinition.getType());
  }
}
