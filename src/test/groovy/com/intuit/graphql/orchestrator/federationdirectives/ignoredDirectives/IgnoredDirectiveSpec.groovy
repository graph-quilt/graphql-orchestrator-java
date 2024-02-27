package com.intuit.graphql.orchestrator.federationdirectives.ignoredDirectives

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.schema.RuntimeGraph
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher
import graphql.schema.GraphQLFieldDefinition
import helpers.BaseIntegrationTestSpecification

class IgnoredDirectiveSpec  extends BaseIntegrationTestSpecification {
    def "Successful stitching fields with tags"() {
        given:
        String SCHEMA_WITH_TAGS = '''
        type Query {
            multipleTagField(id: String): String @tag(name: "tag1") @tag(name: "tag2")
            singleTagField: String @tag(name: "soloTag")                 
            noTagField: String                 
        }
        '''

        ServiceProvider tagProvider = createQueryMatchingService("TAG_SERVICE_PROVIDER",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, SCHEMA_WITH_TAGS, null)
        List<ServiceProvider> services = List.of(tagProvider)

        when:
        RuntimeGraph actualRuntimeGraph = SchemaStitcher.newBuilder()
                .services(services).build().stitchGraph()

        then:
        actualRuntimeGraph != null
        actualRuntimeGraph.addtionalDirectives.stream().anyMatch { it -> it.name == "tag" }
        GraphQLFieldDefinition multipleTagGqlDef = actualRuntimeGraph.executableSchema.queryType.getFieldDefinition("multipleTagField")
        multipleTagGqlDef.directives.size() == 2
        multipleTagGqlDef.getDirectives().get(0).getArgument("name").argumentValue.getValue() == "tag1"
        multipleTagGqlDef.getDirectives().get(1).getArgument("name").argumentValue.getValue() == "tag2"

        GraphQLFieldDefinition singleTagGqlDef = actualRuntimeGraph.executableSchema.queryType.getFieldDefinition("singleTagField")
        singleTagGqlDef.directives.size() == 1
        singleTagGqlDef.getDirectives().get(0).getArgument("name").argumentValue.getValue() == "soloTag"

        actualRuntimeGraph.executableSchema.queryType.getFieldDefinition("noTagField").directives.size() == 0
    }
}