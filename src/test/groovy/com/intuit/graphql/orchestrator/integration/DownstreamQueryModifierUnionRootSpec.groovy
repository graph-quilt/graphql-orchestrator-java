package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.authorization.DefaultFieldAuthorization
import com.intuit.graphql.orchestrator.batch.DownstreamQueryModifier
import com.intuit.graphql.orchestrator.metadata.RenamedMetadata
import com.intuit.graphql.orchestrator.schema.ServiceMetadata
import com.intuit.graphql.orchestrator.utils.SelectionCollector
import graphql.GraphQLContext
import graphql.Scalars
import graphql.language.*
import graphql.schema.*
import spock.lang.Specification

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition
import static graphql.schema.GraphQLObjectType.newObject
import static graphql.schema.GraphQLUnionType.newUnionType
import static java.util.Collections.emptyMap

class DownstreamQueryModifierUnionRootSpec extends Specification {

    private static AstTransformer AST_TRANSFORMER = new AstTransformer()

    private static GraphQLFieldDefinition ID_FIELD_DEFINITION = newFieldDefinition()
            .name("id")
            .type(Scalars.GraphQLString)
            .build()

    private static GraphQLFieldDefinition NAME_FIELD_DEFINITION = newFieldDefinition()
            .name("name")
            .type(Scalars.GraphQLString)
            .build()

    private static GraphQLFieldDefinition REVIEWS_FIELD_DEFINITION = newFieldDefinition()
            .name("reviews")
            .type(Scalars.GraphQLString)
            .withDirective(GraphQLDirective.newDirective().name("resolver"))
            .build()

    private static GraphQLObjectType PRODUCT_A_TYPE = newObject()
            .name("ProductA")
            .field(ID_FIELD_DEFINITION)
            .field(NAME_FIELD_DEFINITION)
            .field(REVIEWS_FIELD_DEFINITION)
            .build()

    private static GraphQLObjectType PRODUCT_B_TYPE = newObject()
            .name("ProductB")
            .field(ID_FIELD_DEFINITION)
            .field(NAME_FIELD_DEFINITION)
            .build()

    private ServiceMetadata serviceMetadataMock
    private GraphQLSchema graphQLSchemaMock
    private RenamedMetadata renamedMetadataMock
    private GraphQLContext graphqlContextMock

    private DownstreamQueryModifier queryModifierWithUnionRootType

    void setup() {

        renamedMetadataMock = Mock(RenamedMetadata.class)
        renamedMetadataMock.getOriginalFieldNamesByRenamedName() >> emptyMap()

        serviceMetadataMock = Mock(ServiceMetadata.class)
        serviceMetadataMock.shouldModifyDownStreamQuery() >> true
        serviceMetadataMock.getRenamedMetadata() >> renamedMetadataMock

        graphQLSchemaMock = Mock(GraphQLSchema.class)
        graphQLSchemaMock.getType("ProductA") >> PRODUCT_A_TYPE
        graphQLSchemaMock.getType("ProductB") >> PRODUCT_B_TYPE

        GraphQLUnionType rootType = newUnionType()
                .name("ProductsUnionType")
                .possibleType(PRODUCT_A_TYPE)
                .possibleType(PRODUCT_B_TYPE)
                .build()

        Map<String, FragmentDefinition> emptyFragmentsByName = emptyMap()
//        queryModifierWithUnionRootType = new DownstreamQueryModifier(rootType, serviceMetadataMock,
//                emptyFragmentsByName, graphQLSchemaMock)
        graphqlContextMock = Mock(GraphQLContext)
        queryModifierWithUnionRootType = DownstreamQueryModifier.builder()
          .rootType(rootType)
          .serviceMetadata(serviceMetadataMock)
          .selectionCollector(new SelectionCollector(emptyFragmentsByName))
          .fieldAuthorization(new DefaultFieldAuthorization())
          .graphQLContext(graphqlContextMock)
          .queryVariables(emptyMap())
          .graphQLSchema(graphQLSchemaMock)
          .build();
    }


    def "an external field is removed from a Field of type union"() {

        given: "FIELDS USED IN INLINE_FRAGMENTS"
        Field idField = Field.newField("id").build()
        Field nameField = Field.newField("name").build()
        Field reviewsFieldFromOtherService = Field.newField("reviews")
                .selectionSet(SelectionSet.newSelectionSet()
                        .selection(Field.newField("score").build())
                        .build())
                .build()

        and: "THE SELECTED INLINE FRAGMENTS"
        SelectionSet withInlineFragmentSelectionSet = SelectionSet.newSelectionSet()
                .selection(InlineFragment.newInlineFragment()
                        .typeCondition(TypeName.newTypeName("ProductA").build())
                        .selectionSet(SelectionSet.newSelectionSet()
                                .selection(idField)
                                .selection(nameField)
                                .selection(reviewsFieldFromOtherService)
                                .build())
                        .build())
                .selection(InlineFragment.newInlineFragment()
                        .typeCondition(TypeName.newTypeName("ProductB").build())
                        .selectionSet(SelectionSet.newSelectionSet()
                                .selection(idField)
                                .selection(nameField)
                                .selection(reviewsFieldFromOtherService)
                                .build())
                        .build())
                .build()

        and: "SELECTED ROOT FIELD THAT HAS TYPE UNION... see reference schema below of this class"
        Field testField = Field.newField("product")
                .selectionSet(withInlineFragmentSelectionSet)
                .build()

        when: "MODIFYING THE SELECTION SET"
        Field actualField = (Field) AST_TRANSFORMER.transform(testField, queryModifierWithUnionRootType)

        then: "REVIEWS FIELD IS REMOVED SINCE IT IS AN EXTERNAL FIELD"
        InlineFragment productAInlineFragment = (InlineFragment) actualField.getSelectionSet().getSelections().get(0)
        productAInlineFragment.getSelectionSet().selections.size() == 2 // instead of 3
        productAInlineFragment.getSelectionSet().selections.get(0) == idField
        productAInlineFragment.getSelectionSet().selections.get(1) == nameField

        InlineFragment productBInlineFragment = (InlineFragment) actualField.getSelectionSet().getSelections().get(1)
        productBInlineFragment.getSelectionSet().selections.get(0) == idField
        productBInlineFragment.getSelectionSet().selections.get(1) == nameField
    }

}


// SCHEMA REFERENCE FOR THIS TEST
/*

type Query {
    product: ProductsUnionType
}

union ProductsUnionType = ProductA|ProductB

type ProductA {
    id: String
    name: String
    reviews: Review @resolver(field: "" arguments: [{name : "workflowId", value: "$workflowId"} {name : "firmId", value: "$firmId"} {name : "name", value: "$name"}])
}

type ProductB {
    id: String
    name: String
}

 */