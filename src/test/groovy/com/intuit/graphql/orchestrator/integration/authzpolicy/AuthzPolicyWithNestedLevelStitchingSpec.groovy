package com.intuit.graphql.orchestrator.integration.authzpolicy

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.datafetcher.ServiceDataFetcher
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.stitching.StitchingException
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLCodeRegistry
import graphql.schema.GraphQLSchema
import graphql.schema.StaticDataFetcher
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

import static com.intuit.graphql.orchestrator.TestHelper.getResourceAsString
import static graphql.Scalars.GraphQLInt

class AuthzPolicyWithNestedLevelStitchingSpec extends BaseIntegrationTestSpecification {

    def v4osService, turboService

    def mockServiceResponse = new HashMap()

    def conflictingSchema = """
        type Query {
            root: RootType 
            topLevelField: NestedType @authzPolicy(ruleInput: [{key:"foo",value:"a"}])
        }

        type RootType {
            nestedField: NestedType
        }
        
        type NestedType {
            fieldA: String @authzPolicy(ruleInput: [{key:"foo",value:"b"}])
            fieldB: String @authzPolicy(ruleInput: [{key:"foo",value:"b"}])
        }
    """

    def conflictingSchema2 = """
        type Query {
            root: RootType
        }

        type RootType {
            nestedField: NestedType
        }
        
        type NestedType {
            fieldC: String @authzPolicy(ruleInput: [{key:"foo",value:"b"}])
            fieldD: String @authzPolicy(ruleInput: [{key:"foo",value:"c"}])
        }
    """
    ServiceProvider topLevelDeprecatedService1
    ServiceProvider topLevelDeprecatedService2


    @Subject
    def specUnderTest

    def "Test Nested Stitching"() {
        given:
        v4osService = createSimpleMockService("V4OS", getResourceAsString("nested/v4os/schema.graphqls"), mockServiceResponse)
        turboService = createSimpleMockService("TURBO", getResourceAsString("nested/turbo/schema.graphqls"), mockServiceResponse)

        when:
        specUnderTest = createGraphQLOrchestrator([v4osService, turboService])

        then:
        GraphQLSchema result = specUnderTest.schema

        def consumer = result?.getQueryType()?.getFieldDefinition("consumer")
        consumer != null

        def consumerType =  result?.getQueryType()?.getFieldDefinition("consumer")?.type
        consumerType != null
        consumerType.name == "ConsumerType"
        consumerType.description == "[V4OS,TURBO]"

        def financialProfile = consumerType?.getFieldDefinition("financialProfile")
        financialProfile != null
        financialProfile.type?.name == "FinancialProfileType"

        def turboExperiences = consumerType?.getFieldDefinition("turboExperiences")
        turboExperiences != null
        turboExperiences.type?.name == "ExperienceType"

        def financeField =  consumerType?.getFieldDefinition("finance")
        financeField != null
        financeField.type?.name == "FinanceType"
        financeField.type?.getFieldDefinition("fieldFinance")?.type == GraphQLInt
        financeField.type?.getFieldDefinition("fieldTurbo")?.type == GraphQLInt

        //DataFetchers
        final GraphQLCodeRegistry codeRegistry = specUnderTest.runtimeGraph.getCodeRegistry().build()
        codeRegistry?.getDataFetcher(FieldCoordinates.coordinates("Query", "consumer"), consumer) instanceof StaticDataFetcher
        codeRegistry?.getDataFetcher(FieldCoordinates.coordinates("ConsumerType", "finance"), financeField) instanceof StaticDataFetcher
        codeRegistry?.getDataFetcher(FieldCoordinates.coordinates("ConsumerType", "financialProfile"), financeField) instanceof ServiceDataFetcher
        codeRegistry?.getDataFetcher(FieldCoordinates.coordinates("ConsumerType", "turboExperiences"), turboExperiences) instanceof ServiceDataFetcher
    }

    def "Nested Type Description With Namespace And Empty Description"() {
        given:
        def bSchema = "schema { query: Query } type Query { a: A } \"\n" +\
         "         \"type A { b: B @adapter(service: 'foo') } type B {d: D}\"\n" +\
         "         \"type D { field: String}\"\n" +\
         "         \"directive @adapter(service:String!) on FIELD_DEFINITION"

        def bbSchema = "schema { query: Query } type Query { a: A } \"\n" +\
         "         \"type A {  bbc: BB }  type BB {cc: String}"

        def abcSchema = "schema { query: Query } type Query { a: A } \"\n" +\
        "         \"type A {  bbcd: C }  type C {cc: String}"

        def secondSchema = "schema { query: Query } type Query { a: A } " +\
         "type A {  bbbb: BAB }  type BAB {fieldBB: String}"

        def ambcSchema = "schema { query: Query } type Query { a: A } \"\n" +\
        "         \"type A {  bba: CDD }  type CDD {ccdd: String}"

        def ttbbSchema = "schema { query: Query } type Query { a: A } \"\n" +\
        "         \"type A {  bbab: BBD }  type BBD {cc: String}"

        def bService = createSimpleMockService("SVC_b", bSchema, mockServiceResponse)
        def bbService = createSimpleMockService("SVC_bb", bbSchema, mockServiceResponse)
        def abcService = createSimpleMockService("SVC_abc", abcSchema, mockServiceResponse)
        def secondService = createSimpleMockService("SVC_Second", secondSchema, mockServiceResponse)
        def ambcService = createSimpleMockService("AMBC", ambcSchema, mockServiceResponse)
        def ttbbService = createSimpleMockService("TTBB", ttbbSchema, mockServiceResponse)

        when:
        specUnderTest = createGraphQLOrchestrator([bService, bbService ,abcService,secondService, ambcService, ttbbService])


        then:
        def aType = specUnderTest.runtimeGraph.getOperation(Operation.QUERY)?.getFieldDefinition("a")?.type

        aType.description.contains("SVC_abc")
        aType.description.contains("SVC_bb")
        aType.description.contains("AMBC")
        aType.description.contains("TTBB")
        aType.description.contains("SVC_Second")
        aType.description.contains("SVC_b")
    }

    def "Nested Type Description With Namespace And Description"() {
        given:
        String schema1 = "schema { query: Query } type Query { a: A } " +\
        "type A { b: B @adapter(service: 'foo') } type B {d: D}" +\
        "type D { field: String}" +\
        "directive @adapter(service:String!) on FIELD_DEFINITION"

        String schema2 = "schema { query: Query } type Query { a: A } " +\
         "\"description for schema2\"type A {  bbc: BB }  type BB {cc: String}"

        String schema3 = "schema { query: Query } type Query { a: A } " +\
         "\"description for schema3\"type A {  bbcd: C }  type C {cc: String}"

        String schema4 = "schema { query: Query } type Query { a: A } " +\
         "type A {  bbbb: BAB }  type BAB {fieldBB: String}"

        def service1 = createSimpleMockService("SVC_b", schema1, mockServiceResponse)
        def service2 = createSimpleMockService("SVC_bb", schema2, mockServiceResponse)
        def service3 = createSimpleMockService("SVC_abc", schema3, mockServiceResponse)
        def service4 = createSimpleMockService("SVC_Second", schema4, mockServiceResponse)

        when:
        specUnderTest = createGraphQLOrchestrator([service1, service2, service3, service4])

        then:
        def aType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY)?.getFieldDefinition("a")?.type

        aType.description.contains("SVC_abc")
        aType.description.contains("SVC_bb")
        aType.description.contains("SVC_Second")
        aType.description.contains("SVC_b")
        aType.description.contains("description for schema3")
        aType.description.contains("description for schema2")
    }

    def "deprecated fields can not be referenced again"() {
        given:
        topLevelDeprecatedService1 = createSimpleMockService("test1", conflictingSchema, new HashMap<String, Object>())
        topLevelDeprecatedService2 = createSimpleMockService("test2", conflictingSchema2, new HashMap<String, Object>())

        when:
        specUnderTest = createGraphQLOrchestrator([topLevelDeprecatedService1, topLevelDeprecatedService2])

        then:
        def exception = thrown(StitchingException)
        exception.message == "FORBIDDEN: Subgraphs [test2,test1] are reusing type NestedType with different field definitions."
    }
}
