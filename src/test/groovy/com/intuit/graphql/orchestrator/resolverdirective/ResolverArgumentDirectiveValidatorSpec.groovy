package com.intuit.graphql.orchestrator.resolverdirective

import spock.lang.Specification

import static com.intuit.graphql.orchestrator.utils.XtextUtils.getAllTypes
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getObjectType
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getOperationType
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getType
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.STANDARD_SCALARS
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newIntType
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newStringType

import com.intuit.graphql.graphQL.FieldDefinition
import com.intuit.graphql.graphQL.ObjectTypeDefinition
import com.intuit.graphql.graphQL.TypeDefinition
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.xtext.FieldContext
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder
import java.util.stream.Stream
import org.eclipse.xtext.resource.XtextResourceSet

class ResolverArgumentDirectiveValidatorSpec extends Specification {

    public ResolverDirectiveTypeResolver resolver

    private UnifiedXtextGraph source

    private ResolverArgumentDirectiveValidator validator

    private TypeDefinition enumType
    private TypeDefinition typeA

    def setup() {
        String schema = '''
            schema { query: Query }
            type Query { a: A } 
            type A { b: B } 
            type B { c: Int schema_enum: SchemaEnum } 
            input AInput { b: BInput } 
            input BInput { c: Int } 
            input AInputWrong { does_not_exist: Int } 
            enum SchemaEnum { a b c } 
            enum InputEnum { a b c } 
        '''

        resolver = Mock(ResolverDirectiveTypeResolver.class)

        XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", schema)

        ObjectTypeDefinition query = getOperationType(Operation.QUERY, set)

        Map<String, TypeDefinition> types =
                Stream.concat(getAllTypes(set), STANDARD_SCALARS.stream())
                        .inject([:]) {map, it -> map << [(it.getName()): it]}

        source = UnifiedXtextGraph.newBuilder()
                .query(query)
                .types(types)
                .build()

        validator = new ResolverArgumentDirectiveValidator()
        validator.typeResolver = resolver

        enumType = getType("SchemaEnum", set)
        typeA = getType("A", set)
    }

    def "validation Passes On Scalars"() {
        given:
        resolver.resolveField("a.b.c", _ as UnifiedXtextGraph, _ as String, _ as FieldContext) >> newIntType()

        String resolverSchema = '''
            type Query { other_a(arg: Int @resolver(field: "a.b.c")): Int } 
            directive @resolver(field: String) on ARGUMENT_DEFINITION
            '''

        final XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", resolverSchema)

        when:
        FieldDefinition fieldWithResolverDirective = getObjectType("Query", set).getFieldDefinition().get(0)

        validator.validateField(fieldWithResolverDirective, source, Mock(FieldContext.class))

        then:
        noExceptionThrown()
    }

    def "validation Passes On Enums"() {
        given:
        resolver.resolveField("a.b.schema_enum", _ as UnifiedXtextGraph, _ as String, _ as FieldContext) >> enumType

        String resolverSchema = '''
            type Query { other_a(arg: SchemaEnum @resolver(field: "a.b.schema_enum")): Int }
            enum SchemaEnum { a b c }
            directive @resolver(field: String) on ARGUMENT_DEFINITION
        '''

        final XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", resolverSchema)

        when:
        FieldDefinition fieldWithResolverDirective = getObjectType("Query", set).getFieldDefinition().get(0)
        validator.validateField(fieldWithResolverDirective, source, Mock(FieldContext.class))

        then:
        noExceptionThrown()
    }

    def "validation Passes On Objects"() {
        given:
        resolver.resolveField("a", _ as UnifiedXtextGraph, _ as String, _ as FieldContext) >> typeA

        String resolverSchema = '''
            type Query { other_a(arg: AInput @resolver(field: "a")): Int }
            input AInput { b: BInput }
            input BInput { c: Int } 
            directive @resolver(field: String) on ARGUMENT_DEFINITION
        '''

        final XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", resolverSchema)

        when:
        FieldDefinition fieldWithResolverDirective = getObjectType("Query", set).getFieldDefinition().get(0)
        validator.validateField(fieldWithResolverDirective, source, Mock(FieldContext))

        then:
        noExceptionThrown()
    }

    def "field Does Not Exist In Schema"() {
        given:
        resolver.resolveField("a", _ as UnifiedXtextGraph, _ as String, _ as FieldContext) >> typeA

        String resolverSchema = '''
            type Query { other_a(arg: AInputWrong @resolver(field: "a")): Int } 
            input AInputWrong { does_not_exist: Int } 
            directive @resolver(field: String) on ARGUMENT_DEFINITION
        '''

        final XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", resolverSchema)

        when:
        FieldDefinition fieldWithResolverDirective = getObjectType("Query", set).getFieldDefinition().get(0)
        validator.validateField(fieldWithResolverDirective, source, Mock(FieldContext.class))

        then:
        thrown(ResolverArgumentFieldNotInSchema)
    }

    def "not Same Leaf Name"() {
        given:
        resolver.resolveField("a.b.c", _ as UnifiedXtextGraph, _ as String, _ as FieldContext) >> newStringType()

        String resolverSchema = '''
            type Query { other_a(arg: Int @resolver(field: "a.b.c")): Int }
            directive @resolver(field: String) on ARGUMENT_DEFINITION
        '''

        final XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", resolverSchema)

        when:
        FieldDefinition fieldWithResolverDirective = getObjectType("Query", set).getFieldDefinition().get(0)
        validator.validateField(fieldWithResolverDirective, source, Mock(FieldContext.class))

        then:
        thrown(ResolverArgumentLeafTypeNotSame)
    }

    def "type Mismatch"() {
        given:
        resolver.resolveField("a", _ as UnifiedXtextGraph, _ as String, _ as FieldContext) >> newStringType()

        String resolverSchema = '''
            type Query { other_a(arg: AInput @resolver(field: "a")): Int } 
            input AInput { b: Int } 
            directive @resolver(field: String) on ARGUMENT_DEFINITION
        '''

        final XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", resolverSchema)

        when:
        FieldDefinition fieldWithResolverDirective = getObjectType("Query", set).getFieldDefinition().get(0)
        validator.validateField(fieldWithResolverDirective, source, Mock(FieldContext.class))

        then:
        thrown(ResolverArgumentTypeMismatch)
    }

}
