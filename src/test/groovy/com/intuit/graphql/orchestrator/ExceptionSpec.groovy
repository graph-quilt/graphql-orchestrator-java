package com.intuit.graphql.orchestrator

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.graphQL.FieldDefinition
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.schema.RuntimeGraph
import com.intuit.graphql.orchestrator.schema.fold.FieldMergeValidations
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException
import com.intuit.graphql.orchestrator.stitching.XtextStitcher
import com.intuit.graphql.orchestrator.xtext.XtextGraph
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder
import graphql.schema.GraphQLObjectType
import spock.lang.Specification

class ExceptionSpec extends Specification {

    void "Nested Type Primitive And Object Type - Type Conflict Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc: String }
            type BB { cc: String }
        '''
        String schema2 = '''
            schema { query: Query }
            type Query { a: A } 
            type A { bbc: String }
            type BB { cc: String }
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        def exception = thrown(FieldMergeException)
        exception.message.startsWith(
                "Nested fields (parentType:A, field:bbc) are not eligible to merge")
    }

    def "Nested Type UnEqual Arguments Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A }
            type A {  bbc (arg1: Arg1, arg2: Arg2): BB }
            type BB { cc: String }
            type Arg1 { pp: String }
            type Arg2 { pp: String }
        '''
        String schema2 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: Arg1): BB }
            type BB { dd: String }
            type Arg1 { pp: String }
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        def exception = thrown(FieldMergeException)
        exception.message.startsWith(
                "Nested fields (parentType:A, field:bbc) are not eligible to merge")
    }

    def "Nested Type Missing Arguments Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: Arg1, arg2: String): BB }
            type BB { cc: String }
            input Arg1 { pp: String }
        '''
        String schema2 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: Arg1, arg3: String): BB }
            type BB { dd: String }
            input Arg1 { pp: String }
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        def exception = thrown(FieldMergeException)
        exception.message.startsWith(
                "Nested fields (parentType:A, field:bbc) are not eligible to merge")
    }

    def "Nested Type Mismatched Arguments Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: Arg1, arg2: Arg2): BB }
            type BB {cc: String}
            input Arg1 { pp: String }
            input Arg2 { pp: String }
        '''
        String schema2 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: Arg1, arg2: Arg3): BB }
            type BB { dd: String }
            input Arg1 { pp: String }
            input Arg3 { pp: String }
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        def exception = thrown(FieldMergeException)
        exception.message.startsWith(
                "Nested fields (parentType:A, field:bbc) are not eligible to merge")
    }

    def "Nested Wraped Type Mismatched Arguments Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: [[Arg1]]!, arg2: Arg2): BB }
            type BB { cc: String }
            input Arg1 { pp: String }
            input Arg2 { pp: String }
        '''
        String schema2 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: [Arg1]!, arg2: Arg2): BB }
            type BB { dd: String }
            input Arg1 { pp: String }
            input Arg2 { pp: String }
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder().namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        def exception = thrown(FieldMergeException)
        exception.message.startsWith(
                "Nested fields (parentType:A, field:bbc) are not eligible to merge")
    }

    def "Nested Type Matched Arguments No Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: Arg1, arg2: Arg2): BB }
            type BB { cc: String }
            input Arg1 { pp: String }
            input Arg2 { pp: String }
        '''
        String schema2 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: Arg1, arg2: Arg2): BB }
            type BB { dd: String }
            input Arg1 { pp: String }
            input Arg2 { pp: String }
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        noExceptionThrown()
    }

    def "Nested Type Matched Arguments Multilevel No Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: [Arg1], arg2: Arg2): BB }
            type BB { cc: String }
            input Arg1 { pp: QQ }
            input Arg2 { pp: Arg2 }
            input QQ { mm: String }
        '''
        String schema2 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: [Arg1], arg2: Arg2): BB }
            type BB { dd: String }
            input Arg1 { pp: QR }
            input Arg2 { pp: Arg2 }
            input QR { mm: String }
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        RuntimeGraph runtimeGraph = XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        GraphQLObjectType aType = (GraphQLObjectType) runtimeGraph.getGraphQLtypes().get("A")
        def bbcFieldDef = aType.getFieldDefinition("bbc")
        bbcFieldDef.getArgument("arg1") != null
        bbcFieldDef.getArgument("arg2") != null

        GraphQLObjectType bbcType = (GraphQLObjectType) bbcFieldDef.getType()
        bbcType.getFieldDefinition("cc") != null
        bbcType.getFieldDefinition("dd") != null
    }

    def "Nested Type MisMatched Arguments Multilevel Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: [Arg1!]!, arg2: Arg2!): BB }
            type BB { cc: String }
            input Arg1 { pp: QQ }
            input Arg2 { pp: Arg2 }
            input QQ { mm: String }
        '''
        String schema2 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc (arg1: [Arg1]!, arg2: Arg2!): BB }
            type BB { dd: String }
            input Arg1 { pp: QQ }
            input Arg2 { pp: Arg2 }
            input QQ { mn: String }
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        def exception = thrown(FieldMergeException)
        exception.message.contains(
                "Nested fields (parentType:A, field:bbc) are not eligible to merge")
    }

    def "Nested Type UnEqual Directives Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A } 
            type A { bbc : BB @addExternalFields(source: "profiles") @excludeField(name: "photo") }
            type BB { cc: String }
            directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE 
            directive @excludeField(name: String!) on FIELD_DEFINITION | ENUM_VALUE
        '''

        String schema2 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc : BB @addExternalFields(source: "profiles") }
            type BB { dd: String }
            directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE 
            directive @someTest on FIELD_DEFINITION
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        def exception = thrown(FieldMergeException)
        exception.message.startsWith(
                "Nested fields (parentType:A, field:bbc) are not eligible to merge")
        exception.message.contains("Unequal directives")
    }

    def "Nested Mismatched Directives Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc : BB @addExternalFields(source: "profiles") @excludeField(name: "photo") }
            type BB {cc: String}
            directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE
            directive @excludeField(name: String!) on FIELD_DEFINITION | ENUM_VALUE
        '''

        String schema2 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc : BB @addExternalFields(source: "profiles") @ignoreField(name: "photo") }
            type BB { dd: String }
            directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE
            directive @someTest on FIELD_DEFINITION
            directive @ignoreField(name: String!) on FIELD_DEFINITION | ENUM_VALUE
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        def exception = thrown(FieldMergeException)
        exception.message.startsWith(
                "Nested fields (parentType:A, field:bbc) are not eligible to merge")
        exception.message.contains("Missing directive")
    }

    def "Nested UnEqual Directive Locations Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc : BB @addExternalFields(source: "profiles") @excludeField(name: "photo") }
            type BB { cc: String }
            directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE
            directive @excludeField(name: String!) on FIELD_DEFINITION
        '''

        String schema2 = '''
            schema { query: Query }
            type Query { a: A }
            type A {  bbc : BB @addExternalFields(source: "profiles") @excludeField(name: "photo") }
            type BB {dd: String}
            directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE
            directive @excludeField(name: String!) on FIELD_DEFINITION | ENUM_VALUE
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        def exception = thrown(FieldMergeException)
        exception.message.startsWith(
                "Nested fields (parentType:A, field:bbc) are not eligible to merge")
        exception.message.contains("Unequal directive locations")
    }

    def "Nested Mismatched Directive Locations Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc : BB @addExternalFields(source: "profiles") @excludeField(name: "photo") }
            type BB { cc: String }
            directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE
            directive @excludeField(name: String!) on FIELD_DEFINITION | FIELD
        '''

        String schema2 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc : BB @addExternalFields(source: "profiles") @excludeField(name: "photo") }
            type BB { dd: String }
            directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE
            directive @excludeField(name: String!) on FIELD_DEFINITION | ENUM_VALUE
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        def exception = thrown(FieldMergeException)
        exception.message.startsWith(
                "Nested fields (parentType:A, field:bbc) are not eligible to merge")
        exception.message.contains("Missing directive location")
    }

    def "Nested Matched Directives No Exception Test"() {
        given:
        String schema1 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc : BB @addExternalFields(source: "profiles") @excludeField(name: "photo") }
            type BB { cc: String }
            directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE
            directive @excludeField(name: String!) on FIELD_DEFINITION | ENUM_VALUE
        '''

        String schema2 = '''
            schema { query: Query }
            type Query { a: A }
            type A { bbc : BB @addExternalFields(source: "profiles") @excludeField(name: "photo") }
            type BB { dd: String }
            directive @addExternalFields(source: String!) on FIELD_DEFINITION | ENUM_VALUE
            directive @someTest on FIELD_DEFINITION
            directive @excludeField(name: String!) on FIELD_DEFINITION | ENUM_VALUE
        '''

        XtextGraph xtextGraph1 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema1)).build())

        XtextGraph xtextGraph2 = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_B").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema2)).build())

        List<ServiceProvider> providerList = Arrays
                .asList(xtextGraph1.getServiceProvider(), xtextGraph2.getServiceProvider())

        when:
        RuntimeGraph runtimeGraph = XtextStitcher.newBuilder().build().stitch(providerList)

        then:
        noExceptionThrown()

        GraphQLObjectType aType = (GraphQLObjectType) runtimeGraph.getGraphQLtypes().get("A")
        def bbcFieldDef = aType.getFieldDefinition("bbc")
        bbcFieldDef.getDirective("addExternalFields") != null
        bbcFieldDef.getDirective("excludeField") != null

        GraphQLObjectType bbcType = (GraphQLObjectType) bbcFieldDef.getType()
        bbcType.getFieldDefinition("cc") != null
        bbcType.getFieldDefinition("dd") != null
    }

    def "Is A GraphQL Object Type Validation Exception Test"() {
        given:
        String schema = '''
            schema { query: Query }
            type Query { a: String }
        '''

        XtextGraph xtextGraph = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        FieldDefinition fieldDefinition = xtextGraph.getOperationType(Operation.QUERY).getFieldDefinition().get(0)
        FieldMergeValidations.checkMergeEligibility("Query", fieldDefinition, fieldDefinition)

        then:
        def exception = thrown(FieldMergeException)
        exception.message.endsWith("is not an ObjectType")
    }

    def "Has The Same Type Name Validation Exception Test"() {
        given:
        String schema = '''
            schema { query: Query }
            type Query { a: String, b: Int }
        '''

        XtextGraph xtextGraph = XtextGraphBuilder.build(
                TestServiceProvider.newBuilder()
                        .namespace("SVC_A").serviceType(ServiceType.REST)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        FieldMergeValidations
                .checkMergeEligibility("Query", xtextGraph.getOperationType(Operation.QUERY).getFieldDefinition().get(0),
                        xtextGraph.getOperationType(Operation.QUERY).getFieldDefinition().get(1))

        then:
        thrown(FieldMergeException)
    }
}
