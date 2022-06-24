package com.intuit.graphql.orchestrator.resolverdirective

import helpers.BaseIntegrationTestSpecification

import static com.intuit.graphql.orchestrator.utils.XtextUtils.getAllTypes
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getOperationType
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.STANDARD_SCALARS

import com.intuit.graphql.graphQL.ObjectTypeDefinition
import com.intuit.graphql.graphQL.TypeDefinition
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.schema.transform.ResolverArgumentListTypeNotSupported
import com.intuit.graphql.orchestrator.xtext.FieldContext
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder
import java.util.stream.Stream
import org.eclipse.xtext.resource.XtextResourceSet

class ResolverDirectiveTypeResolverSpec extends BaseIntegrationTestSpecification {

    private ResolverDirectiveTypeResolver resolver

    public UnifiedXtextGraph source

    void setup() {
        this.resolver = new ResolverDirectiveTypeResolver()

        final String schemaString = '''
            schema { query: Query } 
            type Query { a: A list: [A] premature_leaf: Int some_enum: Enum } 
            type A { b: Int } enum Enum { A }
        '''

        final XtextResourceSet schemaResource = XtextResourceSetBuilder.newBuilder()
                .file("schema", schemaString)
                .build()

        final ObjectTypeDefinition queryOperation = getOperationType(Operation.QUERY, schemaResource)

        Map<String, TypeDefinition> types =
                Stream.concat(getAllTypes(schemaResource), STANDARD_SCALARS.stream())
                .inject([:]) {map, it -> map << [(it.getName()): it]}

        source = UnifiedXtextGraph.newBuilder()
                .query(queryOperation)
                .types(types)
                .build()
    }

    void resolvesRootTypeOfField() {
        given:
        String field = "a.b"

        final TypeDefinition result = resolver.resolveField(field, source, "someArg", Mock(FieldContext.class))

        expect:
        result.getName() == "Int"
    }

    void fieldDoesNotExist() {
        given:
        String field = "c.d"

        when:
        resolver.resolveField(field, source, "someArg", Mock(FieldContext.class))

        then:
        thrown(ResolverArgumentFieldRootObjectDoesNotExist)
    }

    void listsNotSupported() {
        given:
        final String field = "list"

        when:
        resolver.resolveField(field, source, "someArg", Mock(FieldContext.class))

        then:
        thrown(ResolverArgumentListTypeNotSupported)
    }

    void prematureLeafTypeScalar() {
        given:
        final String field = "premature_leaf.nested"

        when:
        resolver.resolveField(field, source, "someArg", Mock(FieldContext.class))

        then:
        thrown(ResolverArgumentPrematureLeafType)
    }

    void prematureLeafTypeEnum() {
        given:
        final String field = "some_enum.nested"

        when:
        resolver.resolveField(field, source, "someArg", Mock(FieldContext.class))

        then:
        thrown(ResolverArgumentPrematureLeafType)
    }
}
