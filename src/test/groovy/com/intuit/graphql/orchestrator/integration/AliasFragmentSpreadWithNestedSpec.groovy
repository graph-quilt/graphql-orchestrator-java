package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import graphql.ExecutionInput
import graphql.ExecutionResult
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class AliasFragmentSpreadWithNestedSpec extends BaseIntegrationTestSpecification {

    def schema1 = """
            schema { query: Query } 
            type Query { a: A } 
            type A { b: B  e:E }  
            type E { f1: String }
            type B {c(a:String): C} 
            type C {d: D}
            type D { f1: String f2: String}
        """

    def schema2 = """
            schema { query: Query } 
            type Query { a: A } 
            type A { b: B }  
            type B {c(a:String): C}             
            type C {f: F}
            type F { f1: String f2: String}
    """
    def mockServiceResponse = [
            data: [
                    a: [b: [
                            foo: [d: [f1: "foo1", f2: "foo2"]],
                            bar: [d: [f1: "bar1", f2: "bar2"]]
                    ]]
            ]
    ]
    @Subject
    def specUnderTest

    def "Alias used at the type-merge level with arguments"() {
        given:

        SimpleMockServiceProvider service1 = createSimpleMockService("SVC_b", schema1, mockServiceResponse)
        def service2 = createSimpleMockService("SVC_c", schema2, mockServiceResponse)
        specUnderTest = createGraphQLOrchestrator([service1, service2])

        def graphqlQuery = """
            query {
              a { b {
               foo: c(a:"foo") { d { f1 } }
               bar: c(a:"bar") { d { f2 } }
              } }
            }
        """

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        //query QUERY {
        //      a {b {foo:c(a:"foo") {d {f1}}}}
        //      a {b {bar:c(a:"bar") {d {f2}}}}
        // }
        compareQueryToExecutionInput(null,
                                    "queryQUERY{a{b{foo:c(a:\"foo\"){d{f1}}bar:c(a:\"bar\"){d{f2}}}}}", service1)
        compareQueryToExecutionInput(null,null, service2)

        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.a?.b?.foo?.d?.f1 instanceof String && data.a?.b?.foo?.d?.f1 == "foo1"
        data.a?.b?.bar?.d?.f2 instanceof String && data.a?.b?.bar?.d?.f2 == "bar2"

    }

    def "Fragments used at the type-merge level with arguments"() {
        given:
        SimpleMockServiceProvider service1 = createSimpleMockService("SVC_b", schema1, mockServiceResponse)
        def service2 = createSimpleMockService("SVC_c", schema2, mockServiceResponse)
        specUnderTest = createGraphQLOrchestrator([service1, service2])

        def graphqlQuery = """
            query {
              a { b {
               foo: c(a:"foo") { ... fr }
               bar: c(a:"bar") { ... fr }
              } }
            }
            fragment fr on C {  d { f1 f2 } } 
        """

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        //    fragment fr on C {d {f1 f2}}
        //
        //    query QUERY {
        //        a {b {foo:c(a:"foo") {...fr}}}
        //        a {b {bar:c(a:"bar") {...fr}}}
        //    }
        compareQueryToExecutionInput(null,
                "fragmentfronC{d{f1f2}}queryQUERY{a{b{foo:c(a:\"foo\"){...fr}bar:c(a:\"bar\"){...fr}}}}", service1)
        compareQueryToExecutionInput(null,null, service2)
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.a?.b?.foo?.d?.f1 instanceof String && data.a?.b?.foo?.d?.f1 == "foo1"
        data.a?.b?.foo?.d?.f2 instanceof String && data.a?.b?.foo?.d?.f2 == "foo2"
        data.a?.b?.bar?.d?.f1 instanceof String && data.a?.b?.bar?.d?.f1 == "bar1"
        data.a?.b?.bar?.d?.f2 instanceof String && data.a?.b?.bar?.d?.f2 == "bar2"
    }


    def "Basic Nested"() {
        given:
        def mockServiceResponseBasic1 = [
                data: [
                        a: [b: [
                                c: [
                                        d: [f1: "foo1", f2: "foo2"]
                                ]
                        ], e: [f1: "eoo"]]
                ]
        ]
        def mockServiceResponseBasic2 = [
                data: [
                        a: [b: [
                                c: [
                                        f: [f1: "bar1", f2: "bar2"]
                                ]
                        ]]
                ]
        ]
        SimpleMockServiceProvider service1 = createSimpleMockService("SVC_b", schema1, mockServiceResponseBasic1)
        def service2 = createSimpleMockService("SVC_c", schema2, mockServiceResponseBasic2)
        specUnderTest = createGraphQLOrchestrator([service1, service2])

        def graphqlQuery = """
            query {
              a { 
                b {
                    c(a:"foo") { 
                      d { f1 f2 } 
                      f { f1 f2 }
                    }
                  } 
                e { f1 } 
              }
            }
        """

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        //    fragment fr on C {d {f1 f2}}
        //
        //    query QUERY {
        //        a {b {foo:c(a:"foo") {...fr}}}
        //        a {b {bar:c(a:"bar") {...fr}}}
        //    }
        compareQueryToExecutionInput(null,
                "queryQUERY{a{b{c(a:\"foo\"){d{f1f2}}}e{f1}}}", service1)
        compareQueryToExecutionInput(null,"queryQUERY{a{b{c(a:\"foo\"){f{f1f2}}}}}", service2)
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.a?.b?.c?.d?.f1 instanceof String && data.a?.b?.c?.d?.f1 == "foo1"
        data.a?.b?.c?.d?.f2 instanceof String && data.a?.b?.c?.d?.f2 == "foo2"
        data.a?.b?.c?.f?.f1 instanceof String && data.a?.b?.c?.f?.f1 == "bar1"
        data.a?.b?.c?.f?.f2 instanceof String && data.a?.b?.c?.f?.f2 == "bar2"
        data.a?.e?.f1 instanceof String && data.a?.e?.f1 == "eoo"
    }
    
}
