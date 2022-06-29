package com.intuit.graphql.orchestrator.utils

import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.Type
import graphql.language.TypeName
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import spock.lang.Specification

class GraphQLUtilSpec extends Specification {

    void canCreateTypeFromGraphQLObjectType() {
        given:
        GraphQLObjectType graphQLObjectType = GraphQLObjectType.newObject().name("object").build()
        Type type = GraphQLUtil.createTypeBasedOnGraphQLType(graphQLObjectType)

        expect:
        type in TypeName
        ((TypeName) type).getName() == "object"
    }

    void canCreateTypeFromNonNullType() {
        given:
        GraphQLObjectType graphQLObjectType = GraphQLObjectType.newObject().name("object").build()
        GraphQLNonNull nonNull = GraphQLNonNull.nonNull(graphQLObjectType)

        Type type = GraphQLUtil.createTypeBasedOnGraphQLType(nonNull)
        Type nonNullType = ((NonNullType) type).getType()

        expect:
        type in NonNullType
        ((TypeName) nonNullType).getName() == "object"
    }

    void canCreateTypeFromListType() {
        given:
        GraphQLObjectType graphQLObjectType = GraphQLObjectType.newObject().name("object").build()
        GraphQLNonNull nonNull = GraphQLNonNull.nonNull(graphQLObjectType)
        GraphQLList graphQLList = GraphQLList.list(nonNull)

        Type type = GraphQLUtil.createTypeBasedOnGraphQLType(graphQLList)
        Type listType = ((ListType) type).getType()
        Type nonNullType = ((NonNullType) listType).getType()

        expect:
        type in ListType
        listType in NonNullType
        ((TypeName) nonNullType).getName() == "object"
    }
}