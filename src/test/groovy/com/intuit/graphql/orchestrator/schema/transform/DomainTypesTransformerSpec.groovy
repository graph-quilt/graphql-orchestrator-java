package com.intuit.graphql.orchestrator.schema.transform

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.graphQL.ObjectTypeDefinition
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.xtext.XtextGraph
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder
import com.intuit.graphql.utils.XtextTypeUtils
import helpers.BaseIntegrationTestSpecification

class DomainTypesTransformerSpec extends BaseIntegrationTestSpecification {

    void "test Domain Types Get Renamed"() {
        given:
        /* TEST SCOPE:
         *
         *      ObjectTypeDefinition, InterfaceTypeDefinition,
         *      UnionTypeDefinition, EnumTypeDefinition,
         *      InputObjectTypeDefinition
         */

        String schema = '''
            schema { query: Query } type Query { a: PageInfo m: MyType1 m2: MyInterface } 
            type PageInfo { id: String } 
            type MyType1 implements MyInterface { s: String } type MyType2 implements MyInterface { b: Boolean } 
            interface MyInterface { i : Int } union MyUnion = MyType1 | MyType2
        '''

        Set<String> domainTypes = new HashSet<>();
        domainTypes.add("MyType1");
        domainTypes.add("MyInterface");
        domainTypes.add("MyUnion");

        XtextGraph xtextGraph = XtextGraphBuilder
                .build(TestServiceProvider.newBuilder().namespace("SVC1")
                        .domainTypes(domainTypes)
                        .sdlFiles(ImmutableMap.of("schema.graphqls", schema))
                        .build());

        when:
        XtextGraph domainGraph = new DomainTypesTransformer().transform(xtextGraph);
        XtextGraph domainGraphTypes = new AllTypesTransformer().transform(domainGraph);

        ObjectTypeDefinition query = xtextGraph.getOperationMap().get(Operation.QUERY);

        then:
        domainGraphTypes.getTypes().containsKey("SVC1_MyType1")
        domainGraphTypes.getTypes().containsKey("SVC1_MyInterface")
        domainGraphTypes.getTypes().containsKey("MyType2")

        XtextTypeUtils.typeName(query.getFieldDefinition().get(1).getNamedType()) == "SVC1_MyType1"
        XtextTypeUtils.typeName(query.getFieldDefinition().get(2).getNamedType()) == "SVC1_MyInterface"
    }

}
