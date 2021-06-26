package com.intuit.graphql.orchestrator.resolverdirective;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.createFieldResolverOperationName;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.createResolverQueryFieldArguments;

import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import graphql.introspection.Introspection.DirectiveLocation;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldsContainer;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;

/**
 * This class is responsible for creating a query operation from field and arguments of
 * a Resolver Directive.
 *
 * It is assumed that the resolver directive has been validated during parsing of the SDL.
 */
public class ResolverDirectiveQueryBuilder {

  /**
   * Creates resolver query {@link OperationDefinition} for resolver directive on {@link DirectiveLocation#FIELD_DEFINITION}
   *
   * If an ancestor in the selection is an array, this will produce a batch query where each
   * data fetching environment field will by suffixed by an index staring from 0.
   *
   * The process starts with creating the leaf field of query.  If resolver has arguments, it will
   * becomes arguments of the leaf field.  Once the leaf field is created, it moves up to create
   * the parent field until the root field is created which completes the query.
   *
   * @param resolverSelectedFields an ordered array representing the field part of resolver directive
   *                            tokenized by dot character.  the left-most token is the first element
   *                            in array and the right-most is last element in array.
   * @param resolverDirectiveDefinition a reference to resolver directive definition
   * @param dataFetchingEnvironments 1 or more data fetching environment
   * @param fieldResolverContext meta data for the resolver field
   * @return OperationDefinition
   */
  public OperationDefinition buildFieldResolverQuery(String[] resolverSelectedFields,
      ResolverDirectiveDefinition resolverDirectiveDefinition,
      FieldResolverContext fieldResolverContext,
      final List<DataFetchingEnvironment> dataFetchingEnvironments) {

    SelectionSet.Builder parentSelectionSetBuilder = SelectionSet.newSelectionSet();

    for (int batchCounter = 0; batchCounter < dataFetchingEnvironments.size(); batchCounter++) {

      DataFetchingEnvironment dataFetchingEnvironment = dataFetchingEnvironments.get(batchCounter);

      final List<Argument> queryFieldArguments = createResolverQueryFieldArguments(
          resolverDirectiveDefinition.getArguments(),
          (GraphQLFieldsContainer) dataFetchingEnvironment.getParentType(),
          dataFetchingEnvironment.getSource(),
          resolverDirectiveDefinition,
          fieldResolverContext.getServiceNamespace()
      );

      final int lastIndex = resolverSelectedFields.length - 1;

      // Build the leaf field
      String leafFieldName = resolverSelectedFields[lastIndex];
      Field.Builder fieldBuilder = Field.newField(leafFieldName);
      fieldBuilder.selectionSet(dataFetchingEnvironment.getField().getSelectionSet());
      if (CollectionUtils.isNotEmpty(queryFieldArguments)) {
        fieldBuilder.arguments(queryFieldArguments);
      }
      fieldBuilder.alias(FieldResolverDirectiveUtil.createAlias(leafFieldName, batchCounter));
      Field leafField = fieldBuilder.build();

      // build selection set starting from leaf to root
      Field currField = leafField;
      SelectionSet currectSelectionSet;

      int currIdx = lastIndex;
      while( currIdx > 0) {
        currectSelectionSet = SelectionSet.newSelectionSet().selection(currField).build(); //**
        currField = Field.newField(resolverSelectedFields[currIdx - 1])
            .selectionSet(currectSelectionSet)
            .build();
        currIdx--;
      }
      parentSelectionSetBuilder.selection(currField);
    }

    // operation name is similar to these batched DFEs, get from any
    String originalOperationName = dataFetchingEnvironments.get(0).getOperationDefinition().getName();

    return OperationDefinition.newOperationDefinition()
        .name(createFieldResolverOperationName(originalOperationName))
        .selectionSet(parentSelectionSetBuilder.build())
        .operation(Operation.QUERY)
        .build();
  }

}
