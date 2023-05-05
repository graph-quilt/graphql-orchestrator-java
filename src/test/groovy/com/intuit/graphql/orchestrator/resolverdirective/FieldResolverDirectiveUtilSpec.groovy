package com.intuit.graphql.orchestrator.resolverdirective

import com.intuit.graphql.graphQL.*
import com.intuit.graphql.orchestrator.xtext.XtextGraph
import org.eclipse.emf.ecore.EObject
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.*
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.FIELD_REFERENCE_PREFIX
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.isReferenceToFieldInParentType
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createInterfaceTypeDefinition

class FieldResolverDirectiveUtilSpec extends Specification {

    private String TEST_FIELD_REFERENCE_NAME = "testFieldReferenceName"
    private String TEST_PARENT_TYPE_NAME = "testParentTypeName"
    private String TEST_SERVICE_NAME = "testServiceName"

    def "is Reference To Field In Parent Type Success"() {
        given:
        String fieldName = "someFieldName"
        FieldDefinition fieldDefinition = buildFieldDefinition(fieldName)
        String fieldReferenceWithNoCorrectPrefix = String.join("", FIELD_REFERENCE_PREFIX, fieldName)
        ObjectTypeDefinition typeDefinition = buildObjectTypeDefinition("TypeName", Collections.singletonList(fieldDefinition))

        when:
        boolean actualResult = isReferenceToFieldInParentType(fieldReferenceWithNoCorrectPrefix, typeDefinition)

        then:
        actualResult
    }

    def "is Reference To Field In Parent Type Fails Not Present In Parent Type"() {
        given:
        String fieldName = "someFieldName"
        FieldDefinition fieldDefinition = buildFieldDefinition("someOTHERFieldName")
        String fieldReferenceWithNoCorrectPrefix = String.join("", FIELD_REFERENCE_PREFIX, fieldName)
        ObjectTypeDefinition typeDefinition = buildObjectTypeDefinition("TypeName", Collections.singletonList(fieldDefinition))

        when:
        boolean actualResult = isReferenceToFieldInParentType(fieldReferenceWithNoCorrectPrefix, typeDefinition)

        then:
        !actualResult
    }

    def "is Reference To Field In Parent Type Fails Invalid Argument"() {
        given:
        String fieldName = "someFieldName"
        FieldDefinition fieldDefinition = buildFieldDefinition(fieldName)
        ObjectTypeDefinition typeDefinition = buildObjectTypeDefinition("TypeName", Collections.singletonList(fieldDefinition))

        when:
        isReferenceToFieldInParentType(fieldName, typeDefinition)

        then:
        thrown(NotAValidFieldReference)
    }

    def "create Field Resolver Contexts Throws Exception For Multiple Resolver Directives"() {
        given:
        XtextGraph mockXtextGraph = Mock(XtextGraph.class)

        Directive resolverDirective1 = buildDirective(buildDirectiveDefinition("resolver"), Collections.emptyList())
        Directive resolverDirective2 = buildDirective(buildDirectiveDefinition("resolver"), Collections.emptyList())

        List<Directive> directives = Arrays.asList(resolverDirective1, resolverDirective2)
        FieldDefinition fieldDefinitionWithResolver1 = buildFieldDefinition("testField1", directives)

        // leaving this line here as it seems there's a bug in object creation.  This resets the list of
        // directives in fieldDefinitionWithResolver1.
        // FieldDefinition fieldDefinitionWithResolver2 = buildFieldDefinition("testField2", directives)

        List<FieldDefinition> fieldDefinitions = new ArrayList<>()
        fieldDefinitions.add(fieldDefinitionWithResolver1)

        when:
        ObjectTypeDefinition objectTypeDefinition = buildObjectTypeDefinition("TestType", fieldDefinitions)

        FieldResolverDirectiveUtil
                .createFieldResolverContexts(objectTypeDefinition, mockXtextGraph)

        then:
        thrown(MultipleResolverDirectiveDefinition)
    }

    def "can Contain Field Resolver Directive Returns True For Object Type Extension Definition"() {
        given:
        EObject eContainer = Mock(ObjectTypeExtensionDefinition.class)

        when:
        boolean actual = FieldResolverDirectiveUtil.canContainFieldResolverDirective(eContainer)

        then:
        actual
    }

    def "can Contain Field Resolver Directive Returns True For Object Type Definition"() {
        given:
        EObject eContainer = Mock(ObjectTypeDefinition.class)

        when:
        boolean actual = FieldResolverDirectiveUtil.canContainFieldResolverDirective(eContainer)

        then:
        actual
    }

    def "can Contain Field Resolver Directive Returns False For Interface Type Definition"() {
        given:
        EObject eContainer = Mock(InterfaceTypeDefinition.class)

        when:
        boolean actual = FieldResolverDirectiveUtil.canContainFieldResolverDirective(eContainer)

        then:
        !actual
    }

    def "get Resolver Directive Parent Type Name Invalid Parent Type"() {
        given:
        EObject eContainer = createInterfaceTypeDefinition()

        DirectiveDefinition mockDirectiveDefinition = Mock(DirectiveDefinition.class)
        mockDirectiveDefinition.getName() >> "mockDirective"

        Directive mockDirective = Mock(Directive.class)
        mockDirective.eContainer() >> eContainer
        mockDirective.getDefinition() >> mockDirectiveDefinition

        when:
        FieldResolverDirectiveUtil.getResolverDirectiveParentTypeName(mockDirective)

        then:
        def exception = thrown(UnexpectedResolverDirectiveParentType)
        exception.getMessage().startsWith("Expecting parent to be an instance of FieldDefinition.  directive=mockDirective, pareTypeInstance=InterfaceTypeDefinition")
    }

    def "ifInvalidFieldReferenceThrowException throws FieldNotFoundInParentException when parentSource doesn't contain fieldReferenceName"() {
        when:
        ResolverDirectiveDefinition resolverDirectiveDefinitionMock = Mock(ResolverDirectiveDefinition.class)
        HashMap<String, Object> testParentSourceMap =  new HashMap<>()
        FieldResolverDirectiveUtil.ifInvalidFieldReferenceThrowException(TEST_FIELD_REFERENCE_NAME, TEST_PARENT_TYPE_NAME, resolverDirectiveDefinitionMock, TEST_SERVICE_NAME, testParentSourceMap)
        then:
        def exception = thrown(FieldNotFoundInParentException.class)
        exception.getMessage().startsWith("Field not found in parent's resolved value.")
    }

    def "ifInvalidFieldReferenceThrowException throws FieldValueIsNullInParentException when parentSource contains fieldReferenceName value as null"() {
        when:
        ResolverDirectiveDefinition resolverDirectiveDefinitionMock = Mock(ResolverDirectiveDefinition.class)
        HashMap<String, Object> testParentSourceMap =  new HashMap<>()
        testParentSourceMap.put(TEST_FIELD_REFERENCE_NAME, null)
        FieldResolverDirectiveUtil.ifInvalidFieldReferenceThrowException(TEST_FIELD_REFERENCE_NAME, TEST_PARENT_TYPE_NAME, resolverDirectiveDefinitionMock, TEST_SERVICE_NAME, testParentSourceMap)
        then:
        def exception = thrown(FieldValueIsNullInParentException.class)
        exception.getMessage().startsWith("Field value not found in parent's resolved value.")
    }
}
