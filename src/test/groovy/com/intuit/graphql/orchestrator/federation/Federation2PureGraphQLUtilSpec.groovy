package com.intuit.graphql.orchestrator.federation

import com.intuit.graphql.graphQL.Directive
import com.intuit.graphql.graphQL.DirectiveDefinition
import com.intuit.graphql.graphQL.FieldDefinition
import com.intuit.graphql.graphQL.ObjectTypeDefinition
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.*

class Federation2PureGraphQLUtilSpec extends Specification {

    private Directive keyDirective1

    private Directive keyDirective2

    private Directive externalDirective

    private Directive requiresDirective

    private Directive nonFedDirective1

    private Directive nonFedDirective2

    def setup() {
        DirectiveDefinition keyDirectiveDefinition = buildDirectiveDefinition("key")
        DirectiveDefinition externalDirectiveDefinition = buildDirectiveDefinition("external")
        DirectiveDefinition requiresDirectiveDefinition = buildDirectiveDefinition("requires")
        DirectiveDefinition nonFedDirectiveDefinition1 = buildDirectiveDefinition("nonFed")
        DirectiveDefinition nonFedDirectiveDefinition2 = buildDirectiveDefinition("nonFed")

        keyDirective1 = buildDirective(keyDirectiveDefinition, Collections.emptyList())
        keyDirective2 = buildDirective(keyDirectiveDefinition, Collections.emptyList())
        externalDirective = buildDirective(externalDirectiveDefinition, Collections.emptyList())
        requiresDirective = buildDirective(requiresDirectiveDefinition, Collections.emptyList())
        nonFedDirective1 = buildDirective(nonFedDirectiveDefinition1, Collections.emptyList())
        nonFedDirective2 = buildDirective(nonFedDirectiveDefinition2, Collections.emptyList())
    }

    def "make As Pure GraphQL, object Type Definition All Fed Directive, removes All Directive"() {
        given:
        FieldDefinition fieldWithExternalDirective = buildFieldDefinition("fieldWithExternalDirective")
        fieldWithExternalDirective.getDirectives().add(externalDirective)

        FieldDefinition fieldWithRequiresDirective = buildFieldDefinition("fieldWithRequiresDirective")
        fieldWithRequiresDirective.getDirectives().add(requiresDirective)

        ObjectTypeDefinition baseObjectType = buildObjectTypeDefinition("EntityType")
        baseObjectType.getDirectives().add(keyDirective1)
        baseObjectType.getDirectives().add(keyDirective2)
        baseObjectType.getFieldDefinition().add(fieldWithExternalDirective)
        baseObjectType.getFieldDefinition().add(fieldWithRequiresDirective)

        when:
        Federation2PureGraphQLUtil.makeAsPureGraphQL(baseObjectType)

        then:
        baseObjectType.getDirectives().size() == 0
        fieldWithExternalDirective.getDirectives().size() == 0
        fieldWithRequiresDirective.getDirectives().size() == 0
    }

    def "make As Pure GraphQL, object Type Definition With Non Fed Directive, retains Non Fed Directive"() {
        given:
        FieldDefinition fieldWithExternalDirective = buildFieldDefinition("fieldWithExternalDirective")
        fieldWithExternalDirective.getDirectives().add(externalDirective)
        fieldWithExternalDirective.getDirectives().add(nonFedDirective1)

        ObjectTypeDefinition baseObjectType = buildObjectTypeDefinition("EntityType")
        baseObjectType.getDirectives().add(keyDirective1)
        baseObjectType.getDirectives().add(nonFedDirective2)

        baseObjectType.getFieldDefinition().add(fieldWithExternalDirective)

        when:
        Federation2PureGraphQLUtil.makeAsPureGraphQL(baseObjectType)

        then:
        baseObjectType.getDirectives().size() == 1
        fieldWithExternalDirective.getDirectives().size() == 1
    }
}
