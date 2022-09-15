package com.intuit.graphql.orchestrator.fieldresolver;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.getNameFromFieldReference;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.ifInvalidFieldReferenceThrowException;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.isReferenceToFieldInParentType;
import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.AST_TRANSFORMER;
import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.getFieldType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isPrimitiveType;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static graphql.schema.InputValueWithState.newExternalValue;

import com.intuit.graphql.orchestrator.batch.DownstreamQueryModifier;
import com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import graphql.Scalars;
import graphql.execution.ValuesResolver;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.SelectionSet;
import graphql.language.Value;
import graphql.parser.Parser;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
public class FieldResolverBatchSelectionSetSupplier implements Supplier<SelectionSet> {

    private final String[] resolverSelectedFields;
    private final List<DataFetchingEnvironment> dataFetchingEnvironments;
    private final FieldResolverContext fieldResolverContext;
    private final ServiceMetadata serviceMetadata;

    @Override
    public SelectionSet get() {
        return createBatchSelectionSet();
    }

    private SelectionSet createBatchSelectionSet() {
        ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();
        SelectionSet.Builder parentSelectionSetBuilder = SelectionSet.newSelectionSet();

        for (int batchCounter = 0; batchCounter < dataFetchingEnvironments.size(); batchCounter++) {

            DataFetchingEnvironment dataFetchingEnvironment = dataFetchingEnvironments.get(batchCounter);

            List<Argument> queryFieldArguments = createFieldArguments(resolverDirectiveDefinition, dataFetchingEnvironment);
            Field rootField = createSelectionSetFor(dataFetchingEnvironment, batchCounter, queryFieldArguments);
            GraphQLSchema graphQLSchema = dataFetchingEnvironment.getGraphQLSchema();
            GraphQLObjectType rootFieldParentType = graphQLSchema.getQueryType();
            GraphQLType rootFieldType = getFieldType(rootField, rootFieldParentType).get();
            rootField = removeFieldsWithExternalTypes(rootField, rootFieldType, dataFetchingEnvironment
                .getFragmentsByName(), graphQLSchema);
            parentSelectionSetBuilder.selection(rootField);
        }

        return parentSelectionSetBuilder.build();
    }

    private Field removeFieldsWithExternalTypes(final Field field,
        GraphQLType parentType, Map<String, FragmentDefinition> fragmentsByName, GraphQLSchema graphQLSchema) {
        // call serviceMetadata.hasFieldResolverDirective() before calling this method
        return (Field) AST_TRANSFORMER.transform(field,
            new DownstreamQueryModifier(unwrapAll(parentType), serviceMetadata, fragmentsByName, graphQLSchema));
    }

    private  List<Argument> createFieldArguments(ResolverDirectiveDefinition resolverDirectiveDefinition, DataFetchingEnvironment dataFetchingEnvironment) {
        List<ResolverArgumentDefinition> resolverArgumentDefinitions = resolverDirectiveDefinition.getArguments();
        Map<String, Object> parentSource = dataFetchingEnvironment.getSource();
        GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) dataFetchingEnvironment.getParentType();

        Objects.requireNonNull(resolverArgumentDefinitions);
        Objects.requireNonNull(parentSource);
        Objects.requireNonNull(resolverDirectiveDefinition);

        List<Argument> arguments = new ArrayList<>();
        resolverArgumentDefinitions.forEach(resolverArg -> {
            Value<?> value = resolveArgumentValue(resolverArg, parentSource, parentType);
            Argument argument = Argument.newArgument(resolverArg.getName(), value).build();
            arguments.add(argument);
        });
        return arguments;
    }

    private Value<?> resolveArgumentValue(ResolverArgumentDefinition resolverArgumentDefinition,
                                          Map<String, Object> parentSource,
                                          GraphQLFieldsContainer parentType) {

        Objects.requireNonNull(resolverArgumentDefinition.getNamedType(), "namedType for ResolverArgumentDefinition is null");

        String resolverArgValue = resolverArgumentDefinition.getValue();

        if (isPrimitiveType(resolverArgumentDefinition.getNamedType())) {
            if (isReferenceToFieldInParentType(resolverArgValue, parentType)) {
                String fieldReferenceName = getNameFromFieldReference(resolverArgValue);

                ifInvalidFieldReferenceThrowException(fieldReferenceName, parentType.getName(),
                        fieldResolverContext.getResolverDirectiveDefinition(), fieldResolverContext.getServiceNamespace(), parentSource);

                Object valueFromSource = parentSource.get(fieldReferenceName);
                GraphQLType fieldReferenceType = GraphQLTypeUtil
                    .unwrapAll(parentType.getFieldDefinition(fieldReferenceName).getType());
                if (fieldReferenceType == Scalars.GraphQLID) {
                    fieldReferenceType = Scalars.GraphQLString;
                }
                return ValuesResolver.valueToLiteral(newExternalValue(valueFromSource), fieldReferenceType);

            } else {
                String typename = com.intuit.graphql.utils.XtextTypeUtils.typeName(resolverArgumentDefinition.getNamedType());
                String stringLiteralAstValue = compileTemplate(fieldResolverContext, resolverArgumentDefinition.getValue(), parentSource);
                if (StringUtils.equals(typename, Scalars.GraphQLString.getName()) ||
                    StringUtils.equals(typename, Scalars.GraphQLID.getName())) {
                    stringLiteralAstValue = String.format("\"%s\"", stringLiteralAstValue);
                }
                return Parser.parseValue(stringLiteralAstValue);

            }
        } else {
            String stringLiteralAstValue = compileTemplate(fieldResolverContext, resolverArgumentDefinition.getValue(), parentSource);
            return Parser.parseValue(stringLiteralAstValue);
        }
    }

    private Field createSelectionSetFor(final DataFetchingEnvironment dataFetchingEnvironment,
                                        final int dfeBatchPosition,
                                        final List<Argument> queryFieldArguments) {

        final int lastIndex = resolverSelectedFields.length - 1;

        final String leafFieldName = resolverSelectedFields[lastIndex];

        Field.Builder fieldBuilder = Field.newField(leafFieldName);
        fieldBuilder.selectionSet(dataFetchingEnvironment.getField().getSelectionSet());
        if (CollectionUtils.isNotEmpty(queryFieldArguments)) {
            fieldBuilder.arguments(queryFieldArguments);
        }

        String aliasName = FieldResolverDirectiveUtil.createAlias(leafFieldName,dfeBatchPosition);
        fieldBuilder.alias(aliasName);
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

        return currField;
    }

    private String compileTemplate(FieldResolverContext fieldResolverContext, String stringTemplate, Map<String, Object> dataSource) {
        ValueTemplate valueTemplate = new ValueTemplate(fieldResolverContext, stringTemplate);
        return valueTemplate.compile(dataSource);
    }

}
