package com.intuit.graphql.orchestrator.schema.transform

import com.intuit.graphql.orchestrator.resolverdirective.ArgumentDefinitionNotAllowed
import com.intuit.graphql.orchestrator.resolverdirective.NotAValidLocationForFieldResolverDirective
import com.intuit.graphql.orchestrator.xtext.XtextGraph
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPreMergeTestHelper.*

class FieldResolverTransformerPreMergeSpec extends Specification {

    private final Transformer<XtextGraph, XtextGraph> transformer = new FieldResolverTransformerPreMerge()

    def "transform With No Field Resolver Success No Transformation"() {
        given:
        String schema = '''
            type Query { 
                basicField(arg: Int) : String
                fieldWithArgumentResolver(arg: Int @resolver(field: "a.b.c")): Int
            }
            directive @resolver(field: String) on ARGUMENT_DEFINITION
        '''
        XtextGraph xtextGraph = createTestXtextGraph(schema)
        assert !xtextGraph.isHasFieldResolverDefinition()

        when:
        final XtextGraph transformedSource = transformer.transform(xtextGraph)

        then:
        transformedSource.getCodeRegistry().size() == 0
        !transformedSource.isHasFieldResolverDefinition()
    }

    def "transform With Field Resolver Success"() {
        given:
        XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_IS_OBJECT_TYPE)
        assert !xtextGraph.isHasFieldResolverDefinition()

        when:
        final XtextGraph transformedSource = transformer.transform(xtextGraph)

        then:
        transformedSource.isHasFieldResolverDefinition()
        transformedSource.getFieldResolverContexts().size() == 1

        FieldResolverContext actualFieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        actualFieldResolverContext.getParentTypename() == "AObjectType"
        actualFieldResolverContext.getFieldName() == "b1"
    }

    def "transform With Field Resolver Parent Not Null Success"() {
        given:
        XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_TYPE_IS_NOT_NULL)
        assert !xtextGraph.isHasFieldResolverDefinition()

        when:
        final XtextGraph transformedSource = transformer.transform(xtextGraph)

        then:
        transformedSource.isHasFieldResolverDefinition()
        transformedSource.getFieldResolverContexts().size() == 1

        FieldResolverContext actualFieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        actualFieldResolverContext.getParentTypename() == "AObjectType"
        actualFieldResolverContext.getFieldName() == "b1"
    }

    def "transform With Field Resolver Parent Wrapped In Array Success"() {
        given:
        XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_TYPE_WRAPPED_IN_ARRAY)
        assert !xtextGraph.isHasFieldResolverDefinition()

        when:
        final XtextGraph transformedSource = transformer.transform(xtextGraph)

        then:
        transformedSource.isHasFieldResolverDefinition()
        transformedSource.getFieldResolverContexts().size() == 1

        FieldResolverContext actualFieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        actualFieldResolverContext.getParentTypename() == "AObjectType"
        actualFieldResolverContext.getFieldName() == "b1"
    }

    def "transform With Field Resolver Parent Not Null Wrapped In Array Success"() {
        given:
        XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_NOT_NULL_WRAPPED_IN_ARRAY)
        assert !xtextGraph.isHasFieldResolverDefinition()

        when:
        final XtextGraph transformedSource = transformer.transform(xtextGraph)

        then:
        transformedSource.isHasFieldResolverDefinition()
        transformedSource.getFieldResolverContexts().size() == 1

        FieldResolverContext actualFieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        actualFieldResolverContext.getParentTypename() == "AObjectType"
        actualFieldResolverContext.getFieldName() == "b1"
    }

    def "transform With Two Field Resolvers Yields Two Field Resolver Context"() {
        given:
        XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_WITH_TWO__FIELD_RESOLVERS)
        assert !xtextGraph.isHasFieldResolverDefinition()

        when:
        final XtextGraph transformedSource = transformer.transform(xtextGraph)

        then:
        transformedSource.isHasFieldResolverDefinition()
        transformedSource.getFieldResolverContexts().size() == 2
    }

    def "transform Two Types With Field With Resolver Yields Two Field Resolver Contexts"() {
        given:
        XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_AND_C_WITH_WITH_FIELD_RESOLVER)
        assert !xtextGraph.isHasFieldResolverDefinition()

        when:
        final XtextGraph transformedSource = transformer.transform(xtextGraph)

        then:
        transformedSource.isHasFieldResolverDefinition()
        transformedSource.getFieldResolverContexts().size() == 2
    }

    def "transform With Field Resolver Parent Is An Interface Throws Exception"() {
        given:
        XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_A_IS_INTERFACE)
        assert !xtextGraph.isHasFieldResolverDefinition()

        when:
        transformer.transform(xtextGraph)

        then:
        thrown(NotAValidLocationForFieldResolverDirective)
    }

    def "transform With Field Definition Resolve Has Argument Throws Exception"() {
        given:
        XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_FIELD_WITH_RESOLVER_HAS_ARGUMENT)
        assert !xtextGraph.isHasFieldResolverDefinition()

        when:
        transformer.transform(xtextGraph)

        then:
        thrown(ArgumentDefinitionNotAllowed)
    }
}
