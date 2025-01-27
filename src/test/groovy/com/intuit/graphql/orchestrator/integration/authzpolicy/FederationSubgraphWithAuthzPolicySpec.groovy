package com.intuit.graphql.orchestrator.integration.authzpolicy

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class FederationSubgraphWithAuthzPolicySpec extends BaseIntegrationTestSpecification {

    @Subject
    def specUnderTest

    def "Two Federation Schemas with @authzPolicy"(){
        given:
        def schema1 = """
                    type Query {
                        getProvider1Val: sharedValueType @authzPolicy(ruleInput: [{key:"foo",value:"a"}])
                    }
                    type sharedValueType {
                        id: ID!
                        name: String
                    }
                """

        def schema2 = """
                    type Query {
                        getProvider2Val: sharedValueType @authzPolicy(ruleInput: [{key:"foo",value:"b"}])
                    }
                    type sharedValueType {
                        id: ID!
                        name: String
                        test: String
                    }
                """

        def valueProvider1 = TestServiceProvider.newBuilder()
                .namespace("A")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(ImmutableMap.of("schema1", schema1))
                .build()

        def valueProvider2 = TestServiceProvider.newBuilder()
                .namespace("B")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(ImmutableMap.of("schema2", schema2))
                .build()

        when:
        specUnderTest = createGraphQLOrchestrator([valueProvider1, valueProvider2])

        then:
        final GraphQLObjectType queryType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY)
        GraphQLFieldDefinition getProvider1ValFieldDef = queryType.getFieldDefinition("getProvider1Val")
        GraphQLFieldDefinition getProvider2ValFieldDef = queryType.getFieldDefinition("getProvider2Val")
        getProvider1ValFieldDef.type.name == "sharedValueType"
        getProvider2ValFieldDef.type.name == "sharedValueType"

        GraphQLDirective directive1 = getProvider1ValFieldDef.getDirective("authzPolicy")
        directive1.getName() == "authzPolicy"
        GraphQLDirective directive2 = getProvider2ValFieldDef.getDirective("authzPolicy")
        directive2.getName() == "authzPolicy"

    }

    def "Two Federation Schemas with one using @authzPolicy"() {
        given:
        def schema1 = """
                    type Query {
                        getProvider1Val: sharedValueType @authzPolicy(ruleInput: [{key:"foo",value:"a"}])
                    }
                    type sharedValueType {
                        id: ID!
                        name: String
                    }
                """

        def schema2 = """
                    type Query {
                        getProvider2Val: sharedValueType
                    }
                    type sharedValueType {
                        id: ID!
                        name: String
                        test: String
                    }
                """

        def valueProvider1 = TestServiceProvider.newBuilder()
                .namespace("A")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(ImmutableMap.of("schema1", schema1))
                .build()

        def valueProvider2 = TestServiceProvider.newBuilder()
                .namespace("B")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(ImmutableMap.of("schema2", schema2))
                .build()

        when:
        specUnderTest = createGraphQLOrchestrator([valueProvider1, valueProvider2])

        then:
        final GraphQLObjectType queryType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY)
        GraphQLFieldDefinition getProvider1ValFieldDef = queryType.getFieldDefinition("getProvider1Val")
        GraphQLFieldDefinition getProvider2ValFieldDef = queryType.getFieldDefinition("getProvider2Val")
        getProvider1ValFieldDef.type.name == "sharedValueType"
        getProvider2ValFieldDef.type.name == "sharedValueType"

        GraphQLDirective directive1 = getProvider1ValFieldDef.getDirective("authzPolicy")
        directive1.getName() == "authzPolicy"
        GraphQLDirective directive2 = getProvider2ValFieldDef.getDirective("authzPolicy")
        directive2 == null

    }

}