package com.intuit.graphql.orchestrator.schema.transform

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.graphQL.ObjectTypeDefinition
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.exceptions.InvalidRenameException
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.xtext.XtextGraph
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder
import lombok.extern.slf4j.Slf4j
import spock.lang.Specification

@Slf4j
class RenameTransformerSpec extends Specification {

    void "test Renamed Fields Get Renamed"() {
        given:
        String schema = '''
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE 
            schema { query: Query }
            type Query { a: MyType1 @rename(to: "renamedA") } 
            type MyType1 { test: String }
        '''

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1")
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        XtextGraph domainGraph = new RenameTransformer().transform(xtextGraph)
        XtextGraph domainGraphTypes = new AllTypesTransformer().transform(domainGraph)

        then:
        ObjectTypeDefinition query = xtextGraph.getOperationMap().get(Operation.QUERY)

        domainGraphTypes.getTypes().containsKey("MyType1")
        query.getFieldDefinition().get(0).getName() == "renamedA"
    }

    def "test Renamed Types Get Renamed"() {
        given:
        String schema = '''
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE 
            schema { query: Query } 
            type Query { a: MyType1 } 
            type MyType1 @rename(to: "RenamedType") { test: String }
        '''

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1")
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        XtextGraph domainGraph = new RenameTransformer().transform(xtextGraph)
        XtextGraph domainGraphTypes = new AllTypesTransformer().transform(domainGraph)

        ObjectTypeDefinition query = xtextGraph.getOperationMap().get(Operation.QUERY)

        then:
        domainGraphTypes.getTypes().containsKey("RenamedType")
    }

    def "test Renamed Type And Field Get Renamed"() {
        given:
        String schema = '''
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE 
            schema { query: Query } 
            type Query { a: MyType1 @rename(to: "renamedA") } 
            type MyType1 @rename(to: "RenamedType") { test: String }
        '''

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1")
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        XtextGraph domainGraph = new RenameTransformer().transform(xtextGraph)
        XtextGraph domainGraphTypes = new AllTypesTransformer().transform(domainGraph)

        ObjectTypeDefinition query = xtextGraph.getOperationMap().get(Operation.QUERY)

        then:
        domainGraphTypes.getTypes().containsKey("RenamedType")
        query.getFieldDefinition().get(0).getName() == "renamedA"
    }

    def "test Exception Caught From Renaming Extension"() {
        given:
        String schema = '''
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE 
                schema { query: Query } 
                type Query { a: MyType1 } 
                type MyType1 { test: String }
                extend type MyType1 @rename(to: "RenamedType") { 
                    id: ID 
                }
        '''

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1")
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        new RenameTransformer().transform(xtextGraph)

        then:
        def exception = thrown(Exception)
        exception in InvalidRenameException
        exception.getMessage() == "Invalid rename directive for MyType1: Type Extensions cannot be renamed MyType1"
    }

    def "test Exception Caught From Renaming Federation Extension"() {
        given:
        String schema = '''
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE 
            schema { query: Query } 
            type Query { a: MyType1 } 
            type MyType1 @extends @rename(to: "RenamedType") { test: String } 
        '''

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1")
                        .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        new RenameTransformer().transform(xtextGraph)

        then:
        def exception = thrown(Exception)
        exception in InvalidRenameException
        exception.getMessage() == "Invalid rename directive for MyType1: Type Extensions cannot be renamed MyType1"
    }

    def "test Exception Caught From Blank Rename"() {
        given:
        String schema = '''
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE 
            schema { query: Query } 
            type Query { a: MyType1 } 
            type MyType1 @rename(to: " ") { test: String } 
        '''

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1")
                        .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        new RenameTransformer().transform(xtextGraph)

        then:
        def exception = thrown(Exception)
        exception in InvalidRenameException
        exception.getMessage() == "Invalid rename directive for MyType1: to argument is empty"
    }

    def "test Exception Caught From Rename With Whitespace"() {
        given:
        String schema = '''
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE 
            schema { query: Query } 
            type Query { a: MyType1 } 
            type MyType1 @rename(to: "Test Rename") { test: String } 
        '''

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1")
                        .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        new RenameTransformer().transform(xtextGraph)

        then:
        def exception = thrown(Exception)
        exception in InvalidRenameException
        exception.getMessage() == "Invalid rename directive for MyType1: to argument (Test Rename) cannot contain whitespace"
    }

    def "test Exception Caught From Rename With Non Alphanumeric Chars"() {
        given:
        String schema = '''
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE 
            schema { query: Query } 
            type Query { a: MyType1 } 
            type MyType1 @rename(to: "Test Rename!2ef24$") { test: String } 
        '''

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1")
                        .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        new RenameTransformer().transform(xtextGraph)

        then:
        def exception = thrown(Exception)
        exception in InvalidRenameException
        exception.getMessage() == 'Invalid rename directive for MyType1: to argument (Test Rename!2ef24$) cannot contain whitespace'
    }

    def "test Exception Caught From Multi Of Same Type Renames"() {
        given:
        String schema = '''
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE 
            schema { query: Query } 
            type Query { a: MyType1 b: MyType2 } 
            type MyType1 @rename(to: "Multi") { test: String } 
            type MyType2 @rename(to: "Multi") { test: String } 
        '''

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1")
                        .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        new RenameTransformer().transform(xtextGraph)

        then:
        def exception = thrown(Exception)
        exception in InvalidRenameException
        exception.getMessage() == "Invalid rename directive for MyType2: Multiple definitions are renamed with the same name"
    }

    def "test Exception Caught From Multi Of Same Field Renames"() {
        given:
        String schema = '''
            directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE 
            schema { query: Query } 
            type Query { a: MyType1 } 
            type MyType1 { test: String @rename(to: "Multi") test2: String @rename(to: "Multi") } 
        '''

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1")
                        .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build())

        when:
        new RenameTransformer().transform(xtextGraph)

        then:
        def exception = thrown(Exception)
        exception in InvalidRenameException
        exception.getMessage() == "Invalid rename directive for test2: Multiple definitions are renamed with the same name"
    }
}
