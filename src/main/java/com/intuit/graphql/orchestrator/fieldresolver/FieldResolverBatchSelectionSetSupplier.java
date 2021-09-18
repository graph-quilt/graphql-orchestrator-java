package com.intuit.graphql.orchestrator.fieldresolver;

import com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import graphql.Scalars;
import graphql.language.*;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.commons.lang.StringUtils;

import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.*;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.isPrimitiveType;
import static graphql.language.AstValueHelper.astFromValue;

@AllArgsConstructor
public class FieldResolverBatchSelectionSetSupplier implements Supplier<SelectionSet> {

    private final String[] resolverSelectedFields;
    private final List<DataFetchingEnvironment> dataFetchingEnvironments;
    private final FieldResolverContext fieldResolverContext;

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

            Selection<?> selection = createSelectionSetFor(dataFetchingEnvironment, batchCounter, queryFieldArguments);
            parentSelectionSetBuilder.selection(selection);
        }

        return parentSelectionSetBuilder.build();
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
                return astFromValue(valueFromSource, fieldReferenceType);

            } else {
                String typename = com.intuit.graphql.utils.XtextTypeUtils.typeName(resolverArgumentDefinition.getNamedType());
                String stringLiteralAstValue = compileTemplate(resolverArgumentDefinition.getValue(), parentSource);
                if (StringUtils.equals(typename, Scalars.GraphQLString.getName()) ||
                    StringUtils.equals(typename, Scalars.GraphQLID.getName())) {
                    stringLiteralAstValue = String.format("\"%s\"", stringLiteralAstValue);
                }
                return AstValueHelper.valueFromAst(stringLiteralAstValue);
            }
        } else {
            String stringLiteralAstValue = compileTemplate(resolverArgumentDefinition.getValue(), parentSource);
            return AstValueHelper.valueFromAst(stringLiteralAstValue);
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

    private String compileTemplate(String stringTemplate, Map<String, Object> dataSource) {
        JsonTemplate jsonTemplate = new JsonTemplate(stringTemplate);
        return jsonTemplate.compile(dataSource);
    }

}
