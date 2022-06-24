package com.intuit.graphql.orchestrator.schema.transform

import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDirectiveValidator

import static com.intuit.graphql.orchestrator.utils.XtextUtils.getAllTypes
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getOperationType
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.STANDARD_SCALARS

import com.intuit.graphql.graphQL.FieldDefinition
import com.intuit.graphql.graphQL.ObjectTypeDefinition
import com.intuit.graphql.graphQL.TypeDefinition
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext
import com.intuit.graphql.orchestrator.xtext.FieldContext
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder
import helpers.BaseIntegrationTestSpecification
import org.eclipse.xtext.resource.XtextResourceSet

import java.util.stream.Stream

class ResolverArgumentTransformerSpec extends BaseIntegrationTestSpecification {

    UnifiedXtextGraph source

    ResolverArgumentTransformer transformer

    ObjectTypeDefinition queryType

    ResolverArgumentDirectiveValidator validator

    void setup() {
        validator = Mock(ResolverArgumentDirectiveValidator.class)

        //assume all validations pass unless otherwise stated.
        validator.validateField(_ as FieldDefinition, _ as UnifiedXtextGraph, _ as FieldContext) >> null

        String schema = '''
            type Query { field(arg: Int @resolver(field: "a.b.c")): Int }
            directive @resolver(field: String) on ARGUMENT_DEFINITION
        '''

        XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", schema)

        queryType = getOperationType(Operation.QUERY, set)

        Map<String, TypeDefinition> types =
        Stream.concat(getAllTypes(set), STANDARD_SCALARS.stream())
                .inject([:]) {map, it -> map << [(it.getName()): it]}

        final DataFetcherContext originalDataFetcherContext = DataFetcherContext
                .newBuilder()
                .namespace("test_namespace")
                .build()

        source = UnifiedXtextGraph.newBuilder()
                .query(queryType)
                .types(types)
                .dataFetcherContext(new FieldContext( "Query", "field"), originalDataFetcherContext)
                .build()

        transformer = new ResolverArgumentTransformer()
        transformer.validator = validator
    }

    void "resolver Argument Transforms Graph"() {
        when:
        def final transformedSource = transformer.transform(source)
        def final resultFieldDefinition = queryType.getFieldDefinition().get(0)

        then:
        transformedSource.getResolverArgumentFields().size() == 1
        resultFieldDefinition.getArgumentsDefinition().getInputValueDefinition().size() == 0
    }
}
