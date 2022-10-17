package com.intuit.graphql.orchestrator.schema.transform

import com.intuit.graphql.graphQL.TypeDefinition
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext
import com.intuit.graphql.orchestrator.xtext.FieldContext
import com.intuit.graphql.orchestrator.xtext.XtextGraph
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder
import org.eclipse.xtext.resource.XtextResourceSet
import spock.lang.Specification

import java.util.stream.Stream

import static com.intuit.graphql.orchestrator.utils.XtextUtils.getAllTypes
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.STANDARD_SCALARS

class UnionAndInterfaceTransformerSpec extends Specification {

    UnionAndInterfaceTransformer unionAndInterfaceTransformer

    XtextGraph source

    def setup() {
        String schema = '''
            type Query { field(arg: Int @resolver(field: "a.b.c")): Int }
            directive @resolver(field: String) on ARGUMENT_DEFINITION
        '''

        XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", schema)

        Map<String, TypeDefinition> types =
                Stream.concat(getAllTypes(set), STANDARD_SCALARS.stream())
                        .inject([:]) { map, it -> map << [(it.getName()): it] }

        final DataFetcherContext originalDataFetcherContext = DataFetcherContext
                .newBuilder()
                .namespace("test_namespace")
                .build()

        source = XtextGraph.newBuilder()
                .types(types)
                .xtextResourceSet(set)
                .dataFetcherContext(new FieldContext("Query", "field"), originalDataFetcherContext)
                .hasInterfaceOrUnion(true)
                .build()

        unionAndInterfaceTransformer = new UnionAndInterfaceTransformer()
    }

    def "resolver Argument Transforms Graph"() {
        when:
        def final transformedSource = unionAndInterfaceTransformer.transform(source)

        then:
        transformedSource.hasInterfaceOrUnion
    }

}
