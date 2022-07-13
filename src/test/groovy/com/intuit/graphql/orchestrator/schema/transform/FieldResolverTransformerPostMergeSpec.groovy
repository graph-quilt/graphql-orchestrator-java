package com.intuit.graphql.orchestrator.schema.transform

import com.intuit.graphql.graphQL.*
import com.intuit.graphql.orchestrator.fieldresolver.FieldResolverException
import com.intuit.graphql.orchestrator.resolverdirective.ExternalTypeNotfoundException
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentNotAFieldOfParentException
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition
import com.intuit.graphql.orchestrator.schema.TypeMetadata
import com.intuit.graphql.orchestrator.stitching.StitchingException
import com.intuit.graphql.orchestrator.xtext.FieldContext
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph
import spock.lang.Specification;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.*
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.*
import static java.util.Collections.singletonList

class FieldResolverTransformerPostMergeSpec extends Specification {

    private static final String EXTENDED_OBJECT_TYPENAME = "AObjectType"
    private static final String EXTERNAL_OBJECT_TYPENAME = "BObjectType"
    private static final String EXTERNAL_INTERFACE_TYPENAME = "BInterfaceType"
    private static final String EXTERNAL_UNION_TYPENAME = "BUnionType"
    private static final String EXTERNAL_ENUM_TYPENAME = "BEnumType"

    private static ObjectTypeDefinition externalObjectTypeDefinition
    private static InterfaceTypeDefinition externalInterfaceTypeDefinition
    private static UnionTypeDefinition externalUnionTypeDefinition
    private static EnumTypeDefinition externalEnumTypeDefinition

    private final Transformer<UnifiedXtextGraph, UnifiedXtextGraph> transformer = new FieldResolverTransformerPostMerge()

    def setup() {
        List<FieldDefinition> fieldDefinitions = singletonList(buildFieldDefinition("fieldA"))
        externalObjectTypeDefinition = buildObjectTypeDefinition(EXTERNAL_OBJECT_TYPENAME,
                fieldDefinitions)
        externalInterfaceTypeDefinition = buildInterfaceTypeDefinition(EXTERNAL_INTERFACE_TYPENAME,
                fieldDefinitions)

        ObjectType objectType = GraphQLFactoryDelegate.createObjectType()
        objectType.setType(externalObjectTypeDefinition)

        List<NamedType> unionMemberNamedTypes = singletonList(objectType)

        externalUnionTypeDefinition = buildUnionTypeDefinition(EXTERNAL_UNION_TYPENAME, unionMemberNamedTypes)
        externalEnumTypeDefinition = buildEnumTypeDefinition(EXTERNAL_ENUM_TYPENAME, "ENUM_VAL1")
    }

    def "transform Schema Without Resolver Directive On Field Definition No Processing Occur"() {
        given:
        String schema = '''
            type Query { 
               basicField(arg: Int) : String
               fieldWithArgumentResolver(arg: Int @resolver(field: "a.b.c")): Int 
            }
            directive @resolver(field: String) on ARGUMENT_DEFINITION
        '''

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        assert !unifiedXtextGraph.isHasFieldResolverDefinition()

        UnifiedXtextGraph unifiedXtextGraphSpy = Spy(unifiedXtextGraph)

        when:
        UnifiedXtextGraph transformedSource = transformer.transform(unifiedXtextGraphSpy)

        then:
        0 * unifiedXtextGraphSpy.getType(_ as NamedType)

        transformedSource.getCodeRegistry().size() == 2
    }

    def "transform Resolved Field Is Scalar Then Processing Succeeds And No Type Replacement Occur"() {
        given:
        String schema = """
            type Query {
              a : AObjectType
              b1(id: String): String
            }
            type AObjectType {
              af1 : String 
            } 
            extend type AObjectType { 
              a : String @resolver(field: "b1" arguments: [{name : "id", value: "\$af1"}]) @deprecated(rea: "Use `newField`.")
            }
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        UnifiedXtextGraph unifiedXtextGraphSpy = Spy(unifiedXtextGraph)

        when:
        UnifiedXtextGraph transformedSource = transformer.transform(unifiedXtextGraphSpy)

        then:
        0 * unifiedXtextGraphSpy.getType(_ as NamedType)

        FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        fieldResolverContext.getTargetFieldContext().getFieldName() == "b1"
        fieldResolverContext.getTargetFieldContext().getParentType() == "Query"

        ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition()
        PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType()
        targetFieldArgumentType.getType() == "String"

        transformedSource.getCodeRegistry().size() == 3

        TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType")
        typeMetadata.getFieldResolverContext("a") != null
    }

    def "transform Resolved Field Is Object Type Then Processing Succeeds"() {
        given:
        String schema = """
            type Query { 
              a : AObjectType 
              b1(id: String): BObjectType 
            }
            type AObjectType { 
              af1 : String 
            } 
            extend type AObjectType { 
              a : BObjectType @resolver(field: "b1" arguments: [{name : "id", value: "\$af1"}]) 
            } 
            type BObjectType 
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        unifiedXtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition)

        ObjectTypeDefinition extendedType = (ObjectTypeDefinition) unifiedXtextGraph.getType(
                EXTENDED_OBJECT_TYPENAME)

        TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType)

        assert !placeHolderTypeDefinition.is(externalObjectTypeDefinition)

        when:
        UnifiedXtextGraph transformedSource = transformer.transform(unifiedXtextGraph)

        then:
        TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType)
        replacementTypeDefinition.is(externalObjectTypeDefinition)

        FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        fieldResolverContext.getTargetFieldContext().getFieldName() == "b1"
        fieldResolverContext.getTargetFieldContext().getParentType() == "Query"

        ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition()
        PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType()
        targetFieldArgumentType.getType() == "String"

        FieldContext expectedFieldContent = new FieldContext("AObjectType", "a")
        transformedSource.getCodeRegistry().get(expectedFieldContent) != null

        TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType")
        typeMetadata.getFieldResolverContext("a") != null
    }

    def "transform Resolved Field Object Type Wrapped Not Null Then Processing Succeeds"() {
        given:
        String schema = """
            type Query { 
              a : AObjectType 
              b1(id: String): BObjectType! 
            } 
            type AObjectType { 
              af1 : String 
            } 
            extend type AObjectType { 
              a : BObjectType! @resolver(field: "b1" arguments: [{name : "id", value: "\$af1"}])
            } 
            type BObjectType 
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        unifiedXtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition)

        ObjectTypeDefinition extendedType = (ObjectTypeDefinition) unifiedXtextGraph.getType(
                EXTENDED_OBJECT_TYPENAME)

        TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType)

        assert !placeHolderTypeDefinition.is(externalObjectTypeDefinition)

        when:
        UnifiedXtextGraph transformedSource = transformer.transform(unifiedXtextGraph)

        then:
        TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType)
        replacementTypeDefinition.is(externalObjectTypeDefinition)

        FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        fieldResolverContext.getTargetFieldContext().getFieldName() == "b1"
        fieldResolverContext.getTargetFieldContext().getParentType() == "Query"

        ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition()
        PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType()
        targetFieldArgumentType.getType() == "String"

        FieldContext expectedFieldContent = new FieldContext("AObjectType", "a")
        transformedSource.getCodeRegistry().get(expectedFieldContent) != null

        TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType")
        typeMetadata.getFieldResolverContext("a") != null
    }

    def "transform Resolved Field Is Object Type Array Then Processing Succeeds"() {
        given:
        String schema = """
            type Query { 
              a : AObjectType 
              b1(id: String): [BObjectType] 
            } 
            type AObjectType { 
              af1 : String 
            } 
            extend type AObjectType { 
              a : [BObjectType] @resolver(field: "b1" arguments: [{name : "id", value: "\$af1"}]) 
            }
            type BObjectType
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        unifiedXtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition)

        ObjectTypeDefinition extendedType = (ObjectTypeDefinition) unifiedXtextGraph.getType(
                EXTENDED_OBJECT_TYPENAME)

        TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType)

        assert !placeHolderTypeDefinition.is(externalObjectTypeDefinition)

        when:
        UnifiedXtextGraph transformedSource = transformer.transform(unifiedXtextGraph)

        then:
        TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType)
        replacementTypeDefinition.is(externalObjectTypeDefinition)

        FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        fieldResolverContext.getTargetFieldContext().getFieldName() == "b1"
        fieldResolverContext.getTargetFieldContext().getParentType() == "Query"

        ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition()
        PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType()
        targetFieldArgumentType.getType() == "String"

        FieldContext expectedFieldContent = new FieldContext("AObjectType", "a")
        transformedSource.getCodeRegistry().get(expectedFieldContent) != null

        TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType")
        typeMetadata.getFieldResolverContext("a") != null
    }

    def "transform Resolved Field Is Object Type NOt Null Array Then Processing Succeeds"() {
        given:
        String schema = """
            type Query { 
              a : AObjectType 
              b1(id: String): [BObjectType!] 
            } 
            type AObjectType { 
              af1 : String 
            } 
            extend type AObjectType { 
              a : [BObjectType!] @resolver(field: "b1" arguments: [{name : "id", value: "\$af1"}]) 
            }
            type BObjectType 
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        unifiedXtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition)

        ObjectTypeDefinition extendedType = (ObjectTypeDefinition) unifiedXtextGraph.getType(
                EXTENDED_OBJECT_TYPENAME)

        TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType)

        assert !placeHolderTypeDefinition.is(externalObjectTypeDefinition)

        when:
        UnifiedXtextGraph transformedSource = transformer.transform(unifiedXtextGraph)

        then:
        TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType)
        replacementTypeDefinition.is(externalObjectTypeDefinition)

        FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        fieldResolverContext.getTargetFieldContext().getFieldName() == "b1"
        fieldResolverContext.getTargetFieldContext().getParentType() == "Query"

        ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition()
        PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType()
        targetFieldArgumentType.getType() == "String"

        FieldContext expectedFieldContent = new FieldContext("AObjectType", "a")
        transformedSource.getCodeRegistry().get(expectedFieldContent) != null

        TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType")
        typeMetadata.getFieldResolverContext("a") != null
    }

    def "transform Resolved Field Is Interface Then Processing Succeeds"() {
        given:
        String schema = """
            type Query { 
              a : AObjectType 
              b1(id: String): BInterfaceType 
            } 
            type AObjectType { 
              af1 : String 
            } 
            extend type AObjectType { 
              a : BInterfaceType @resolver(field: "b1" arguments: [{name : "id", value: "\$af1"}]) 
            } 
            interface BInterfaceType 
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        unifiedXtextGraph.getTypes().put(EXTERNAL_INTERFACE_TYPENAME, externalInterfaceTypeDefinition)

        ObjectTypeDefinition extendedType = (ObjectTypeDefinition) unifiedXtextGraph.getType(
                EXTENDED_OBJECT_TYPENAME)

        TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
                EXTERNAL_INTERFACE_TYPENAME, extendedType)

        assert !placeHolderTypeDefinition.is(externalInterfaceTypeDefinition)

        when:
        UnifiedXtextGraph transformedSource = transformer.transform(unifiedXtextGraph)

        then:
        TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(
                EXTERNAL_INTERFACE_TYPENAME, extendedType)
        replacementTypeDefinition.is(externalInterfaceTypeDefinition)

        FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        fieldResolverContext.getTargetFieldContext().getFieldName() == "b1"
        fieldResolverContext.getTargetFieldContext().getParentType() == "Query"

        ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition()
        PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType()
        targetFieldArgumentType.getType() == "String"

        FieldContext expectedFieldContent = new FieldContext("AObjectType", "a")
        transformedSource.getCodeRegistry().get(expectedFieldContent) != null

        TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType")
        typeMetadata.getFieldResolverContext("a") != null
    }

    def "transform Resolved Field Is Union Then Processing Succeeds"() {
        given:
        String schema = """
            type Query { 
              a : AObjectType 
              b1(id: String): BUnionType 
            } 
            type AObjectType { 
              af1 : String 
            } 
            extend type AObjectType { 
              a : BUnionType @resolver(field: "b1" arguments: [{name : "id", value: "\$af1"}]) 
            }
            union BUnionType
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        unifiedXtextGraph.getTypes().put(EXTERNAL_UNION_TYPENAME, externalUnionTypeDefinition)

        ObjectTypeDefinition extendedType = (ObjectTypeDefinition) unifiedXtextGraph.getType(
                EXTENDED_OBJECT_TYPENAME)

        TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
                EXTERNAL_UNION_TYPENAME, extendedType)

        assert !placeHolderTypeDefinition.is(externalUnionTypeDefinition)

        when:
        UnifiedXtextGraph transformedSource = transformer.transform(unifiedXtextGraph)

        then:
        TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(
                EXTERNAL_UNION_TYPENAME, extendedType)
        replacementTypeDefinition.is(externalUnionTypeDefinition)

        FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        fieldResolverContext.getTargetFieldContext().getFieldName() == "b1"
        fieldResolverContext.getTargetFieldContext().getParentType() == "Query"

        ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition()
        PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType()
        targetFieldArgumentType.getType() == "String"

        FieldContext expectedFieldContent = new FieldContext("AObjectType", "a")
        transformedSource.getCodeRegistry().get(expectedFieldContent) != null

        TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType")
        typeMetadata.getFieldResolverContext("a") != null
    }

    def "transform Resolved Field Is Enum Then Processing Succeeds"() {
        given:
        String schema = """
            type Query { 
              a : AObjectType 
              b1(id: String): BEnumType 
            } 
            type AObjectType { 
              af1 : String 
            } 
            extend type AObjectType { 
              a : BEnumType @resolver(field: "b1" arguments: [{name : "id", value: "\$af1"}]) 
            }
            enum BEnumType { } 
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        unifiedXtextGraph.getTypes().put(EXTERNAL_ENUM_TYPENAME, externalEnumTypeDefinition)

        ObjectTypeDefinition extendedType = (ObjectTypeDefinition) unifiedXtextGraph.getType(
                EXTENDED_OBJECT_TYPENAME)

        TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
                EXTERNAL_ENUM_TYPENAME, extendedType)

        assert !placeHolderTypeDefinition.is(externalEnumTypeDefinition)

        when:
        UnifiedXtextGraph transformedSource = transformer.transform(unifiedXtextGraph)

        then:
        TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(
                EXTERNAL_ENUM_TYPENAME, extendedType)
        replacementTypeDefinition.is(externalEnumTypeDefinition)

        FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        fieldResolverContext.getTargetFieldContext().getFieldName() == "b1"
        fieldResolverContext.getTargetFieldContext().getParentType() == "Query"

        ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition()
        PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType()
        targetFieldArgumentType.getType() == "String"

        FieldContext expectedFieldContent = new FieldContext("AObjectType", "a")
        transformedSource.getCodeRegistry().get(expectedFieldContent) != null

        TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType")
        typeMetadata.getFieldResolverContext("a") != null
    }

    def "transform Resolved Field External Type Not Found Throws Exception"() {
        given:
        String schema = """
            type Query { 
              a : AObjectType 
            } 
            type AObjectType { 
              af1 : String 
            } 
            extend type AObjectType { 
              a : BObjectType! @resolver(field: "b1" arguments: [{name : "id", value: "\$af1"}]) 
            } 
            type BObjectType 
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)

        when:
        transformer.transform(unifiedXtextGraph)

        then:
        def exception = thrown(ExternalTypeNotfoundException)
        exception.getMessage() == "External type not found.  serviceName=TEST_SVC, parentTypeName=AObjectType, fieldName=a, placeHolderTypeDescription=[name:BObjectType, type:ObjectTypeDefinition, description:null]"
        //External type not found.  serviceName=TEST_SVC, parentTypeName=AObjectType, fieldName=a, placeHolderTypeDescription=[name:BObjectType, type:ObjectTypeDefinition, description:null]
    }

    def "transform Resolver Argument Not In Parent Type Throws Exception"() {
        given:
        String schema = """
            type Query { 
              a : AObjectType 
              b1(id: String): BEnumType 
            } 
            type AObjectType { } 
            extend type AObjectType { 
              a : BEnumType @resolver(field: "b1" arguments: [{name : "id", value: "\$af1"}]) 
            }
            enum BEnumType { } 
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        unifiedXtextGraph.getTypes().put(EXTERNAL_ENUM_TYPENAME, externalEnumTypeDefinition)

        ObjectTypeDefinition extendedType = (ObjectTypeDefinition) unifiedXtextGraph.getType(
                EXTENDED_OBJECT_TYPENAME)

        TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
                EXTERNAL_ENUM_TYPENAME, extendedType)

        assert !placeHolderTypeDefinition.is(externalEnumTypeDefinition)

        when:
        transformer.transform(unifiedXtextGraph)

        then:
        def exception = thrown(ResolverArgumentNotAFieldOfParentException)
        exception.getMessage().startsWith("'af1' is not a field of parent type. serviceName=TEST_SVC, parentTypeName=AObjectType, fieldName=a")
    }

    def "transform Resolver Argument Has Invalid Argument Value Throws Exception"() {
        given:
        String schema = """
            type Query {
              a : AObjectType 
              b1(id: String): BEnumType 
            } 
            type AObjectType { } 
            extend type AObjectType { 
              a : BEnumType @resolver(field: "b1" arguments: [{name : "id", value: "{invalid object}"}])
            }
            enum BEnumType { }
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        unifiedXtextGraph.getTypes().put(EXTERNAL_ENUM_TYPENAME, externalEnumTypeDefinition)

        ObjectTypeDefinition extendedType = (ObjectTypeDefinition) unifiedXtextGraph.getType(
                EXTENDED_OBJECT_TYPENAME)

        TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
                EXTERNAL_ENUM_TYPENAME, extendedType)

        assert !placeHolderTypeDefinition.is(externalEnumTypeDefinition)

        when:
        transformer.transform(unifiedXtextGraph)

        then:
        def exception = thrown(StitchingException)
        exception.getMessage() == "Invalid resolver argument value: ResolverArgumentDefinition(name=id, value={invalid object})"
    }

    def "transform Field Resolver Has Different Type Than Target Throws Exception"() {
        given:
        String schema = """
            type Query { 
              a : AObjectType 
              b1(id: String): [BObjectType] 
            } 
            type AObjectType { 
              af1 : String 
            } 
            extend type AObjectType { 
              a : BObjectType @resolver(field: "b1" arguments: [{name : "id", value: "\$af1"}]) 
            } 
            type BObjectType 
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        unifiedXtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition)

        ObjectTypeDefinition extendedType =
                (ObjectTypeDefinition) unifiedXtextGraph.getType(EXTENDED_OBJECT_TYPENAME)

        TypeDefinition placeHolderTypeDefinition =
                getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType)

        assert !placeHolderTypeDefinition.is(externalObjectTypeDefinition)

        when:
        transformer.transform(unifiedXtextGraph)

        then:
        def exception = thrown(FieldResolverException)
        exception.getMessage().startsWith('The type of field with @resolver is not compatible with target field type.  fieldName=a,  parentTypeName=AObjectType,  resolverDirectiveDefinition=ResolverDirectiveDefinition(field=b1, arguments=[ResolverArgumentDefinition(name=id, value=$af1)])')
    }

    def "transform Field Resolver And Target Field Has Same Array Type Processing Succeeds"() {
        given:
        String schema = """
            type Query { 
              a : AObjectType 
              b1(id: String): [BObjectType] 
            } 
            type AObjectType { 
              af1 : String 
            } 
            extend type AObjectType { 
              a : [BObjectType] @resolver(field: "b1" arguments: [{name : "id", value: "\$af1"}]) 
            }
            type BObjectType 
            $RESOLVER_DIRECTIVE_DEFINITION
        """

        UnifiedXtextGraph unifiedXtextGraph = createTestUnifiedXtextGraph(schema)
        unifiedXtextGraph.getTypes().put("BObjectType", externalObjectTypeDefinition)

        ObjectTypeDefinition extendedType =
                (ObjectTypeDefinition) unifiedXtextGraph.getType("AObjectType")

        TypeDefinition placeHolderTypeDefinition =
                getTypeFromFieldDefinitions("BObjectType", extendedType)

        assert !placeHolderTypeDefinition.is(externalObjectTypeDefinition)

        when:
        UnifiedXtextGraph transformedSource = transformer.transform(unifiedXtextGraph)

        then:
        TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(
                EXTERNAL_OBJECT_TYPENAME, extendedType)
        replacementTypeDefinition.is(externalObjectTypeDefinition)

        FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0)
        fieldResolverContext.getTargetFieldContext().getFieldName() == "b1"
        fieldResolverContext.getTargetFieldContext().getParentType() == "Query"

        ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition()
        PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType()
        targetFieldArgumentType.getType() == "String"

        FieldContext expectedFieldContent = new FieldContext("AObjectType", "a")
        transformedSource.getCodeRegistry().get(expectedFieldContent) != null

        TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType")
        typeMetadata.getFieldResolverContext("a") != null
    }
}
