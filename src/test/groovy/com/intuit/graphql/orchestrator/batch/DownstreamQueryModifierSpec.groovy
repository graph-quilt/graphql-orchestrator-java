package com.intuit.graphql.orchestrator.batch

import com.google.common.collect.ImmutableSet
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata
import com.intuit.graphql.orchestrator.federation.metadata.FederationMetadata.EntityMetadata
import com.intuit.graphql.orchestrator.federation.metadata.KeyDirectiveMetadata
import com.intuit.graphql.orchestrator.metadata.RenamedMetadata
import com.intuit.graphql.orchestrator.resolverdirective.DownstreamQueryModifierTestHelper
import com.intuit.graphql.orchestrator.resolverdirective.DownstreamQueryModifierTestHelper.TestService
import com.intuit.graphql.orchestrator.schema.ServiceMetadataImpl
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import graphql.language.AstTransformer
import graphql.language.Field
import graphql.language.FragmentDefinition
import graphql.language.InlineFragment
import graphql.language.Selection
import graphql.language.SelectionSet
import graphql.language.TypeName
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.resolverdirective.DownstreamQueryModifierTestHelper.aSchema
import static com.intuit.graphql.orchestrator.resolverdirective.DownstreamQueryModifierTestHelper.bSchema
import static com.intuit.graphql.orchestrator.resolverdirective.DownstreamQueryModifierTestHelper.cSchema
import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.unwrapAll
import static graphql.schema.FieldCoordinates.coordinates

class DownstreamQueryModifierSpec extends Specification {

    private ServiceMetadataImpl serviceMetadataMock

    private Field af1, af2
    private Field b1, b2, b3, b4, b5

    //renamed fields
    private Field renamedAf3, renamedId1, resolvedC
    private SelectionSet selectionSet
    private SelectionSet reverseSelectionSet
    private SelectionSet renamedFieldSelectionSet
    private SelectionSet renamedResolverSelectionSet

    private DownstreamQueryModifier subjectUnderTest

    private AstTransformer astTransformer = new AstTransformer()

    def setup() {
        serviceMetadataMock = Mock(ServiceMetadataImpl)

        ServiceProvider serviceA = new TestService("serviceA", aSchema, null)
        ServiceProvider serviceB = new TestService("serviceB", bSchema, null)
        ServiceProvider serviceC = new TestService("serviceC", cSchema, null)

        DownstreamQueryModifierTestHelper fieldResolverTestHelper = new DownstreamQueryModifierTestHelper(serviceA, serviceB, serviceC)
        GraphQLSchema graphQLSchema = fieldResolverTestHelper.getGraphQLSchema()
        GraphQLFieldsContainer aType = (GraphQLFieldsContainer) unwrapAll(
                graphQLSchema.getType("AObjectType"))

        GraphQLScalarType a3Type = (GraphQLScalarType) unwrapAll(graphQLSchema.getType("String"))

        af1 = Field.newField("af1").build()
        af2 = Field.newField("af2").build()
        b1 = Field.newField("b1").build()
        b2 = Field.newField("b2").build()
        b3 = Field.newField("b3").build()
        b4 = Field.newField("b4").build()
        b5 = Field.newField("b5").build()

        renamedAf3 = Field.newField("renamedAf3").build()
        renamedId1  = Field.newField("renamedId1").build()
        resolvedC = Field.newField("resolvedC").build()
        selectionSet = SelectionSet.newSelectionSet().selection(af1).selection(af2)
                .selection(b1).selection(b2).selection(b3).selection(b4).selection(b5).build()
        reverseSelectionSet = SelectionSet.newSelectionSet()
                .selection(af2).selection(b5).selection(b4).selection(b3)
                .selection(b2).selection(b1).selection(af1).build()
        renamedFieldSelectionSet = SelectionSet.newSelectionSet().selection(renamedId1).build()
        renamedResolverSelectionSet = SelectionSet.newSelectionSet().selection(resolvedC).build()

        RenamedMetadata renameMetadata = new RenamedMetadata(null)
        renameMetadata.getOriginalFieldNamesByRenamedName().put("renamedAf3", "getA3")
        renameMetadata.getOriginalFieldNamesByRenamedName().put("resolvedC", "c1")
        renameMetadata.getOriginalTypeNamesByRenamedName().put("renamedCType", "CObjectType")
        renameMetadata.getOriginalFieldNamesByRenamedName().put("AObjectType-renamedId1", "id2")
        renameMetadata.getOriginalFieldNamesByRenamedName().put("renamedCType-renamedId2", "id")

        serviceMetadataMock.getRenamedMetadata() >> renameMetadata
        serviceMetadataMock.isOwnedByEntityExtension(_) >> false
        serviceMetadataMock.shouldModifyDownStreamQuery() >> true

        subjectUnderTest = new DownstreamQueryModifier(aType, serviceMetadataMock, Collections.emptyMap(), graphQLSchema)
    }

    def "can Remove Field"() {
        given:
        //AstTransformer astTransformer = new AstTransformer()

        when:
        // test 'a1 { af1 af2 b1 b2 b3 b4 b5 }' and remove b1..b5
        Field a1 = Field.newField("a1").selectionSet(selectionSet).build()
        Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest)

        Field f = (Field) newA1.getSelectionSet().getSelections().get(0)
        Field f2 = (Field) newA1.getSelectionSet().getSelections().get(1)

        then:
        // expect modified field to be 'a1 { af1 af2}'
        newA1.getSelectionSet().getSelections().size() == 2
        f.getName() == af1.getName()

        f2.getName() == af2.getName()

        // a should not be modified
        a1.getSelectionSet().getSelections().size() == 7
    }

    def "can Remove Field With Reverse Selection Set"() {
        given:
        // test 'a1 { af2 b5 b4 b3 b2 b1 af1 }' and remove b5..b1
        Field a1 = Field.newField("a1").selectionSet(reverseSelectionSet).build()

        when:
        Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest)

        Field f = (Field) newA1.getSelectionSet().getSelections().get(0)
        Field f2 = (Field) newA1.getSelectionSet().getSelections().get(1)

        then:
        // expect modified field to be 'a1 { af2   af1}'
        newA1.getSelectionSet().getSelections().size() == 2
        f.getName() == af2.getName()

        f2.getName() == af1.getName()

        // a should not be modified
        a1.getSelectionSet().getSelections().size() == 7
    }

    // TODO resolver defined in ObjectType
    def "can Remove Fields From Fragment Definition"() {
        given:
        //    fragment aFragment on A {
        //      af1
        //      b2  <- to be remove
        //      b1  <- to be remove
        //    }
        SelectionSet aFragmentSelectionSet = SelectionSet.newSelectionSet().selection(af1)
                .selection(b2).selection(b1).build()

        FragmentDefinition aFragment = FragmentDefinition.newFragmentDefinition()
                .name("AFragment")
                .selectionSet(aFragmentSelectionSet)
                .typeCondition(TypeName.newTypeName("AObjectType").build())
                .build()

        when:
        FragmentDefinition newAFragmentDefinition = (FragmentDefinition) astTransformer
                .transform(aFragment, subjectUnderTest)
        Field f = (Field) newAFragmentDefinition.getSelectionSet().getSelections().get(0)

        then:
        newAFragmentDefinition.getSelectionSet().getSelections().size() == 1
        f.getName() == af1.getName()
    }

    def "can Remove Fields From Inline Fragment Without Interface"() {
        given:
        InlineFragment inlineFragment = InlineFragment.newInlineFragment()
                .selectionSet(SelectionSet.newSelectionSet().selection(af1)
                        .selection(b5).selection(b4).selection(b3).build())
                .typeCondition(TypeName.newTypeName("AObjectType").build())
                .build()

        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(af2).selection(inlineFragment)
                .selection(b2).selection(b1).build()

        when:
        Field a1 = Field.newField("a1").selectionSet(selectionSet).build()
        Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest)

        then:
        Field f = (Field) newA1.getSelectionSet().getSelections().get(0)
        InlineFragment newInlineFragment = (InlineFragment) newA1.getSelectionSet().getSelections().get(1)

        newA1.getSelectionSet().getSelections().size() == 2
        f.getName() == af2.getName()

        newInlineFragment.getSelectionSet().getSelections().size() == 1

        // a should not be modified
        a1.getSelectionSet().getSelections().size() == 4
    }

    def "visit Selection Set add Required Fields"() {
        given:
        KeyDirectiveMetadata keyDirectiveDataMock = Mock(KeyDirectiveMetadata.class)
        keyDirectiveDataMock.getFieldSet() >> ImmutableSet.of(Field.newField("id").build())

        EntityMetadata entityMetadataMock = Mock(EntityMetadata.class)
        entityMetadataMock.getKeyDirectives() >> Collections.singletonList(keyDirectiveDataMock)

        FederationMetadata federationMetadataMock = Mock(FederationMetadata.class)
        federationMetadataMock.hasRequiresFieldSet(coordinates("AObjectType", "af1")) >> true
        federationMetadataMock.getRequireFields(coordinates("AObjectType", "af1")) >> ImmutableSet.of(Field.newField("reqdField").build())
        federationMetadataMock.getEntityMetadataByName("AObjectType") >> entityMetadataMock
        serviceMetadataMock.getFederationServiceMetadata() >> federationMetadataMock
        serviceMetadataMock.isEntity("AObjectType") >> true

        // af1 is external, should be removed
        FieldCoordinates testFieldCoordinate = coordinates("AObjectType", "af1")
        serviceMetadataMock.isOwnedByEntityExtension(testFieldCoordinate) >> true

        // test '{ af1 }' and add id as key field and regdFields requiredField
        selectionSet = SelectionSet.newSelectionSet().selection(af1).build()
        Field a1 = Field.newField("a1").selectionSet(selectionSet).build()

        when:
        Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest)

        then:
        List<Selection> selections = newA1.getSelectionSet().getSelections()

        selections.size() == 2
    }

    def "visit Selection Set req Field Not Selected add Required Fields"() {
        given:
        FieldResolverContext fieldResolverContextMock = Mock(FieldResolverContext.class)
        fieldResolverContextMock.getRequiredFields() >> ImmutableSet.of("reqdField")

        FieldCoordinates testFieldCoordinate = coordinates("AObjectType", "af1")
        serviceMetadataMock.getFieldResolverContext(testFieldCoordinate) >> fieldResolverContextMock

        // test '{ a1 { af1 } }', where af1 has @resolver and requires reqdField
        Field a1 = Field.newField("a1")
                .selectionSet(SelectionSet.newSelectionSet()
                        .selection(af1)
                        .build())
                .build()

        when:
        Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest)

        then:
        // test '{ a1 { af1 reqdField} }', expected
        @SuppressWarnings("rawtypes")
        List<Selection> actualSelections = newA1.getSelectionSet().getSelections()

        actualSelections.size() == 2
        toFieldNameSet(actualSelections).sort() == ["af1", "reqdField"]
    }

    def "can Rename Query Fields"() {
        when:
        // test 'renamedAf3' should be sent as a3
        Field newAf3 = (Field) astTransformer.transform(renamedAf3, subjectUnderTest)

        then:
        // expect modified field to be 'a3: renamedAf3'
        newAf3.getName() == "getA3"
        newAf3.getAlias() == "renamedAf3"
    }

    def "can Rename Type Fields"() {
        given:
        // a1 { renamedId1 } should be sent as a1 { id2: renamedId1 }
        Field a1 = Field.newField("a1").selectionSet(renamedFieldSelectionSet).build()

        when:
        Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest)
        Field selection = (Field) newA1.getSelectionSet().getSelections().get(0)

        then:
        // expect modified field to be 'a3: renamedAf3'
        newA1.getName() == "a1"
        newA1.getSelectionSet().getSelections().size() == 1

        selection.getName() == "id2"
        selection.getAlias() == "renamedId1"
    }

    def "validate User Inputted Alias Overrides Rename"() {
        given:
        // a1 { renamedId1 } should be sent as a1 { id2: customAlias }
        Field aliasField = Field.newField("renamedId1").alias("customAlias").build()
        SelectionSet aliasSelectionSet = SelectionSet.newSelectionSet().selection(aliasField).build()
        Field a1 = Field.newField("a1", aliasSelectionSet).build()

        when:
        Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest)
        Field selection = (Field) newA1.getSelectionSet().getSelections().get(0)

        then:
        newA1.getName() == "a1"
        newA1.getSelectionSet().getSelections().size() == 1

        selection.getName() == "id2"
        selection.getAlias() == "customAlias"
    }

    def "can Remove Fields From Renamed Resolvers"() {
        given:
        // test 'renamedAf3' should be sent as a3
        // a1 { renamedId1 } should be sent as a1 { id2: renamedId1 }
        Field a1 = Field.newField("a1").selectionSet(renamedResolverSelectionSet).build()

        when:
        Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest)

        then:
        // expect modified field to be 'a3: renamedAf3'
        newA1.getName() == "a1"
        a1.getSelectionSet().getSelections().size() == 1
        newA1.getSelectionSet().getSelections().size() == 0
    }

    def "visit Selection Set reqField Already Selected does Not Add RequiredField"() {
        given:
        FieldResolverContext fieldResolverContextMock = Mock(FieldResolverContext.class)
        fieldResolverContextMock.getRequiredFields() >> ImmutableSet.of("reqdField")

        FieldCoordinates testFieldCoordinate = coordinates("AObjectType", "af1")
        serviceMetadataMock.getFieldResolverContext(testFieldCoordinate) >> fieldResolverContextMock

        // test '{ a1 { af1 reqdField} }', where af1 has @resolver and requires reqdField
        Field a1 = Field.newField("a1")
                .selectionSet(SelectionSet.newSelectionSet()
                        .selection(af1)
                        .selection(Field.newField("reqdField").build())
                        .build())
                .build()

        when:
        Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest)

        then:
        // test '{ a1 { af1 reqdField} }', expected
        @SuppressWarnings("rawtypes")
        List<Selection> actualA1Selections = newA1.getSelectionSet().getSelections()

        actualA1Selections.size() == 2

        toFieldNameSet(actualA1Selections).sort() == ["af1", "reqdField" ]
    }

    def "visit Selection Set no Required Fields does Not Add Required Field"() {
        given:
        FieldResolverContext fieldResolverContextMock = Mock(FieldResolverContext.class)
        fieldResolverContextMock.getRequiredFields() >> Collections.emptySet()

        FieldCoordinates testFieldCoordinate = coordinates("AObjectType", "af1")
        serviceMetadataMock.getFieldResolverContext(testFieldCoordinate) >> fieldResolverContextMock

        // test '{ a1 { af1} }', where af1 has @resolver and requires reqdField
        Field a1 = Field.newField("a1")
                .selectionSet(SelectionSet.newSelectionSet()
                        .selection(af1)
                        .build())
                .build()

        when:
        Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest)

        then:
        // test '{ a1 { af1} }', expected
        @SuppressWarnings("rawtypes")
        List<Selection> actualA1Selections = newA1.getSelectionSet().getSelections()

        actualA1Selections.size() == 1

        toFieldNameSet(actualA1Selections) == [ "af1" ]
    }

    def "visit Selection Set no Field Resolvers does Not Add Required Field"() {
        given:
        FieldCoordinates testFieldCoordinate = coordinates("AObjectType", "af1")
        serviceMetadataMock.getFieldResolverContext(testFieldCoordinate) >> null

        // test '{ a1 { af1} }', where af1 has @resolver and requires reqdField
        Field a1 = Field.newField("a1")
                .selectionSet(SelectionSet.newSelectionSet()
                        .selection(af1)
                        .build())
                .build()

        when:
        Field newA1 = (Field) astTransformer.transform(a1, subjectUnderTest)

        then:
        // test '{ a1 { af1} }', expected
        @SuppressWarnings("rawtypes")
        List<Selection> actualA1Selections = newA1.getSelectionSet().getSelections()

        actualA1Selections.size() == 1

        toFieldNameSet(actualA1Selections) == [ "af1" ]
    }

    private List<String> toFieldNameSet(@SuppressWarnings("rawtypes") List<Selection> fields) {
        return fields.stream()
                .filter({ selection -> selection instanceof Field })
                .map({ selection -> (Field) selection })
                .collect( { it -> it.getName() })
    }

}
