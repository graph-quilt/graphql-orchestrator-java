package com.intuit.graphql.orchestrator.authorization

import com.intuit.graphql.orchestrator.SelectionSetUtil
import com.intuit.graphql.orchestrator.batch.AuthDownstreamQueryModifier
import com.intuit.graphql.orchestrator.common.ArgumentValueResolver
import com.intuit.graphql.orchestrator.metadata.RenamedMetadata
import com.intuit.graphql.orchestrator.schema.ServiceMetadata
import com.intuit.graphql.orchestrator.utils.SelectionCollector
import graphql.GraphQLContext
import graphql.GraphqlErrorException
import graphql.language.*
import graphql.parser.Parser
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldsContainer
import helpers.SchemaTestUtil
import org.apache.commons.collections4.CollectionUtils
import spock.lang.Specification

class AuthDownstreamQueryRedactorVisitorSpec extends Specification {

    def testGraphQLSchema = SchemaTestUtil.createGraphQLSchema("""
        type Query {
            a: A
            a2: String
        }            
        type A {
            b1: B1
            b2: B2
        }
        type B1 {
            c1: C1
        }
        type C1 {
            s1: String
        }
        type B2 {
            i1: Int
        }
    """)

    def testQuery = """
        { 
            a { 
                b1 {                                     
                    ... on B1 {
                        c1 { 
                            s1 
                        }
                    }   
                } 
                b2 { i1 } 
            } 
            a2
        }
    """

    def testDeferInlineFragmentQuery = """
        { 
            a { 
                ... on B1 @defer {
                    c1 { 
                        s1 
                    }
                }   
                b2 { i1 } 
            } 
        }
    """

    def testIfDeferInlineFragmentQuery = """
        { 
            a { 
                ... on B1 @defer(if:false) {
                    c1 { 
                        s1 
                    }
                }   
                b2 { i1 } 
            } 
        }
    """

    def testDeferSpreadFragmentQuery = """
        { 
            a { 
                ... DeferredFrag @defer  
                b2 { i1 } 
            } 
        }
        fragment DeferredFrag on B1 {
            c1
        } 
    """

    def testIfDeferSpreadFragmentQuery = """
        { 
            a { 
                ... DeferredFrag @defer(if: false)  
                b2 { i1 } 
            } 
        }
        fragment DeferredFrag on B1 {
            c1
        } 
    """

    static final Object TEST_AUTH_DATA = "TestAuthDataCanBeAnyObject"

    Field mockField = Mock()
    FieldAuthorizationEnvironment queryA = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("Query","a"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a"])
            .build()

    FieldAuthorizationEnvironment queryA2 = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("Query","a2"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a2"])
            .build()

    FieldAuthorizationEnvironment aB1 = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("A","b1"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a", "b1"])
            .build()

    FieldAuthorizationEnvironment b1C1 = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("B1","c1"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a", "b1", "c1"])
            .build()

    FieldAuthorizationEnvironment c1S1 = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("C1","s1"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a", "b1", "c1", "s1"])
            .build()

    FieldAuthorizationEnvironment aB2 = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("A","b2"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a", "b2"])
            .build()

    FieldAuthorizationEnvironment b2i1 = FieldAuthorizationEnvironment
            .builder()
            .field(mockField)
            .fieldCoordinates(FieldCoordinates.coordinates("B2","i1"))
            .authData(TEST_AUTH_DATA)
            .argumentValues(Collections.emptyMap())
            .path(["a","b2", "i1"])
            .build()

    GraphQLContext mockGraphQLContext = Mock()
    FieldAuthorization mockFieldAuthorization = Mock()
    ArgumentValueResolver argumentValueResolver = Mock()
    RenamedMetadata mockRenamedMetadata = Mock()
    ServiceMetadata mockServiceMetadata = Mock()

    Map<String, FragmentDefinition> fragmentsByName = new HashMap<>()

    AstTransformer astTransformer = new AstTransformer()

    def setup() {
        argumentValueResolver.resolve(_, _, _) >> Collections.emptyMap()
    }

    def "redact query, results to empty selection set"() {
        given:

        Document document = new Parser().parseDocument(testQuery)
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

        GraphqlErrorException testGraphqlErrorException = GraphqlErrorException.newErrorException()
            .message("testError")
            .build()

        AuthDownstreamQueryModifier specUnderTest = AuthDownstreamQueryModifier.builder()
                .rootParentType((GraphQLFieldsContainer) rootFieldParentType)
                .fieldAuthorization(mockFieldAuthorization)
                .graphQLContext(mockGraphQLContext)
                .queryVariables(Collections.emptyMap())
                .graphQLSchema(testGraphQLSchema)
                .selectionCollector(new SelectionCollector(fragmentsByName))
                .serviceMetadata(mockServiceMetadata)
                .authData(TEST_AUTH_DATA)
                .build()

        when:
        Field transformedField =  (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        transformedField.getName() == "a"
        CollectionUtils.size(specUnderTest.getEmptySelectionSets()) == 2

        1 * mockFieldAuthorization.authorize(queryA) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        1 * mockFieldAuthorization.authorize(aB1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        1 * mockFieldAuthorization.authorize(b1C1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        1 * mockFieldAuthorization.authorize(c1S1) >> FieldAuthorizationResult
                .createDeniedResult(testGraphqlErrorException)
        1 * mockFieldAuthorization.authorize(aB2) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        1 * mockFieldAuthorization.authorize(b2i1) >> FieldAuthorizationResult
                .createDeniedResult(testGraphqlErrorException)

        4 * mockRenamedMetadata.getOriginalFieldNamesByRenamedName() >>  Collections.emptyMap()
        4 * mockServiceMetadata.getRenamedMetadata() >>  mockRenamedMetadata
    }

    def "root node access is denied"() {
        given:

        Document document = new Parser().parseDocument(testQuery)
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a2"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

        GraphqlErrorException testGraphqlErrorException = GraphqlErrorException.newErrorException()
                .message("testError")
                .build()

        AuthDownstreamQueryModifier specUnderTest = AuthDownstreamQueryModifier.builder()
                .rootParentType((GraphQLFieldsContainer) rootFieldParentType)
                .fieldAuthorization(mockFieldAuthorization)
                .graphQLContext(mockGraphQLContext)
                .queryVariables(Collections.emptyMap())
                .graphQLSchema(testGraphQLSchema)
                .selectionCollector(new SelectionCollector(fragmentsByName))
                .serviceMetadata(mockServiceMetadata)
                .authData(TEST_AUTH_DATA)
                .build()

        when:
        Field transformedField =  (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        transformedField == null
        CollectionUtils.size(specUnderTest.getEmptySelectionSets()) == 0

        1 * mockFieldAuthorization.authorize(queryA2) >>  FieldAuthorizationResult
                .createDeniedResult(testGraphqlErrorException)
    }

    def "deferred inline fragments are removed"() {
        given:

        Document document = new Parser().parseDocument(testDeferInlineFragmentQuery)
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

        AuthDownstreamQueryModifier specUnderTest = AuthDownstreamQueryModifier.builder()
                .rootParentType((GraphQLFieldsContainer) rootFieldParentType)
                .fieldAuthorization(mockFieldAuthorization)
                .graphQLContext(mockGraphQLContext)
                .queryVariables(Collections.emptyMap())
                .graphQLSchema(testGraphQLSchema)
                .selectionCollector(new SelectionCollector(fragmentsByName))
                .serviceMetadata(mockServiceMetadata)
                .authData(TEST_AUTH_DATA)
                .build()

        when:
        mockGraphQLContext.getOrDefault("useDefer", false) >> true
        Field transformedField =  (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        rootField.getName() == "a"
        rootField.selectionSet.selections.size() == 2
        ((InlineFragment)(rootField.selectionSet.selections.get(0))).getTypeCondition().name == "B1"
        ((Field)(rootField.selectionSet.selections.get(1))).getName() == "b2"

        transformedField.getName() == "a"
        transformedField.selectionSet.selections.size() == 1
        ((Field)(transformedField.selectionSet.selections.get(0))).getName() == "b2"

        mockFieldAuthorization.authorize(queryA) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(aB1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(b1C1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(c1S1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(aB2) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(b2i1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT

        mockRenamedMetadata.getOriginalFieldNamesByRenamedName() >>  Collections.emptyMap()
        mockServiceMetadata.getRenamedMetadata() >>  mockRenamedMetadata
    }

    def "deferred inline fragments with if argument as false are kept"() {
        given:

        Document document = new Parser().parseDocument(testIfDeferInlineFragmentQuery)
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")

        AuthDownstreamQueryModifier specUnderTest = AuthDownstreamQueryModifier.builder()
                .rootParentType((GraphQLFieldsContainer) rootFieldParentType)
                .fieldAuthorization(mockFieldAuthorization)
                .graphQLContext(mockGraphQLContext)
                .queryVariables(Collections.emptyMap())
                .graphQLSchema(testGraphQLSchema)
                .selectionCollector(new SelectionCollector(fragmentsByName))
                .serviceMetadata(mockServiceMetadata)
                .authData(TEST_AUTH_DATA)
                .build()

        when:
        mockGraphQLContext.getOrDefault("useDefer", false) >> true
        Field transformedField =  (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        rootField.getName() == "a"
        rootField.selectionSet.selections.size() == 2
        ((InlineFragment)(rootField.selectionSet.selections.get(0))).getTypeCondition().name == "B1"
        ((Field)(rootField.selectionSet.selections.get(1))).getName() == "b2"

        transformedField.getName() == "a"
        transformedField.selectionSet.selections.size() == 2
        ((InlineFragment)(transformedField.selectionSet.selections.get(0))).getTypeCondition().name == "B1"
        ((Field)(transformedField.selectionSet.selections.get(1))).getName() == "b2"

        mockFieldAuthorization.authorize(queryA) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(aB1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(b1C1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(c1S1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(aB2) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(b2i1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT

        mockRenamedMetadata.getOriginalFieldNamesByRenamedName() >>  Collections.emptyMap()
        mockServiceMetadata.getRenamedMetadata() >>  mockRenamedMetadata
    }

    def "deferred fragment spreads are removed"() {
        given:

        Document document = new Parser().parseDocument(testDeferSpreadFragmentQuery)
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")
        FragmentDefinition mockedFragmentDef = Mock()
        HashMap<String, FragmentDefinition> fragsByName = ["DeferredFrag": mockedFragmentDef]
        Field mockCField = Mock()
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(mockCField).build()

        AuthDownstreamQueryModifier specUnderTest = AuthDownstreamQueryModifier.builder()
                .rootParentType((GraphQLFieldsContainer) rootFieldParentType)
                .fieldAuthorization(mockFieldAuthorization)
                .graphQLContext(mockGraphQLContext)
                .queryVariables(Collections.emptyMap())
                .graphQLSchema(testGraphQLSchema)
                .selectionCollector(new SelectionCollector(fragsByName))
                .serviceMetadata(mockServiceMetadata)
                .authData(TEST_AUTH_DATA)
                .build()

        when:
        mockGraphQLContext.getOrDefault("useDefer", false) >> true
        mockedFragmentDef.getSelectionSet() >> selectionSet
        Field transformedField =  (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        rootField.getName() == "a"
        rootField.selectionSet.selections.size() == 2
        ((FragmentSpread)(rootField.selectionSet.selections.get(0))).getName() == "DeferredFrag"
        ((Field)(rootField.selectionSet.selections.get(1))).getName() == "b2"

        transformedField.getName() == "a"
        transformedField.selectionSet.selections.size() == 1
        ((Field)(transformedField.selectionSet.selections.get(0))).getName() == "b2"

        specUnderTest.fragmentSpreadsRemoved.size() == 1
        specUnderTest.fragmentSpreadsRemoved.get(0) == "DeferredFrag"

        mockFieldAuthorization.authorize(queryA) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(aB1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(b1C1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(c1S1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(aB2) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(b2i1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT

        mockRenamedMetadata.getOriginalFieldNamesByRenamedName() >>  Collections.emptyMap()
        mockServiceMetadata.getRenamedMetadata() >>  mockRenamedMetadata
    }

    def "deferred fragment spreads with if argument as false are kept"() {
        given:

        Document document = new Parser().parseDocument(testIfDeferSpreadFragmentQuery)
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")
        FragmentDefinition mockedFragmentDef = Mock()
        HashMap<String, FragmentDefinition> fragsByName = ["DeferredFrag": mockedFragmentDef]
        Field mockCField = Mock()
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(mockCField).build()

        AuthDownstreamQueryModifier specUnderTest = AuthDownstreamQueryModifier.builder()
                .rootParentType((GraphQLFieldsContainer) rootFieldParentType)
                .fieldAuthorization(mockFieldAuthorization)
                .graphQLContext(mockGraphQLContext)
                .queryVariables(Collections.emptyMap())
                .graphQLSchema(testGraphQLSchema)
                .selectionCollector(new SelectionCollector(fragsByName))
                .serviceMetadata(mockServiceMetadata)
                .authData(TEST_AUTH_DATA)
                .build()

        when:
        mockGraphQLContext.getOrDefault("useDefer", false) >> true
        mockedFragmentDef.getSelectionSet() >> selectionSet
        Field transformedField =  (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        rootField.getName() == "a"
        rootField.selectionSet.selections.size() == 2
        ((FragmentSpread)(rootField.selectionSet.selections.get(0))).getName() == "DeferredFrag"
        ((Field)(rootField.selectionSet.selections.get(1))).getName() == "b2"

        transformedField.getName() == "a"
        transformedField.selectionSet.selections.size() == 2
        ((FragmentSpread)(transformedField.selectionSet.selections.get(0))).getName() == "DeferredFrag"
        ((Field)(transformedField.selectionSet.selections.get(1))).getName() == "b2"

        mockFieldAuthorization.authorize(queryA) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(aB1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(b1C1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(c1S1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(aB2) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(b2i1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT

        mockRenamedMetadata.getOriginalFieldNamesByRenamedName() >>  Collections.emptyMap()
        mockServiceMetadata.getRenamedMetadata() >>  mockRenamedMetadata
    }

    def "Exception when defer directive is not in supported location"() {
        given:

        Document document = new Parser().parseDocument(testIfDeferSpreadFragmentQuery)
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        Field rootField = SelectionSetUtil.getFieldByPath(Arrays.asList("a"), operationDefinition.getSelectionSet())
        GraphQLFieldsContainer rootFieldParentType = (GraphQLFieldsContainer) testGraphQLSchema.getType("Query")
        FragmentDefinition mockedFragmentDef = Mock()
        HashMap<String, FragmentDefinition> fragsByName = ["DeferredFrag": mockedFragmentDef]
        Field mockCField = Mock()
        SelectionSet selectionSet = SelectionSet.newSelectionSet().selection(mockCField).build()

        AuthDownstreamQueryModifier specUnderTest = AuthDownstreamQueryModifier.builder()
                .rootParentType((GraphQLFieldsContainer) rootFieldParentType)
                .fieldAuthorization(mockFieldAuthorization)
                .graphQLContext(mockGraphQLContext)
                .queryVariables(Collections.emptyMap())
                .graphQLSchema(testGraphQLSchema)
                .selectionCollector(new SelectionCollector(fragsByName))
                .serviceMetadata(mockServiceMetadata)
                .authData(TEST_AUTH_DATA)
                .build()

        when:
        mockGraphQLContext.getOrDefault("useDefer", false) >> true
        mockedFragmentDef.getSelectionSet() >> selectionSet
        Field transformedField =  (Field) astTransformer.transform(rootField, specUnderTest)

        then:
        rootField.getName() == "a"
        rootField.selectionSet.selections.size() == 2
        ((FragmentSpread)(rootField.selectionSet.selections.get(0))).getName() == "DeferredFrag"
        ((Field)(rootField.selectionSet.selections.get(1))).getName() == "b2"

        transformedField.getName() == "a"
        transformedField.selectionSet.selections.size() == 2
        ((FragmentSpread)(transformedField.selectionSet.selections.get(0))).getName() == "DeferredFrag"
        ((Field)(transformedField.selectionSet.selections.get(1))).getName() == "b2"

        mockFieldAuthorization.authorize(queryA) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(aB1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(b1C1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(c1S1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(aB2) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT
        mockFieldAuthorization.authorize(b2i1) >> FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT

        mockRenamedMetadata.getOriginalFieldNamesByRenamedName() >>  Collections.emptyMap()
        mockServiceMetadata.getRenamedMetadata() >>  mockRenamedMetadata
    }
}
