package com.intuit.graphql.orchestrator.fieldresolver

import com.google.common.collect.Sets
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import spock.lang.Specification

class ValueTemplateSpec extends Specification {

    private Map<String, Object> testDataSource
    private FieldResolverContext fieldResolverContextMock

    void setup() {
        fieldResolverContextMock = Mock(FieldResolverContext.class)

        testDataSource = new HashMap<>()
    }

    void compile_SimpleVariable_success() {
        given:
        String valueTemplateStr = '$someVar'
        ValueTemplate subjectUnderTest = new ValueTemplate(fieldResolverContextMock, valueTemplateStr)

        Set<String> requiredFields = Sets.newHashSet("someVar")
        fieldResolverContextMock.getRequiredFields() >> requiredFields

        testDataSource.put("someVar", "TEST_VALUE")

        when:
        String actual = subjectUnderTest.compile(testDataSource)

        then:
        actual == "TEST_VALUE"
    }

    void compile_jsonStringWithVariable_success() {
        given:
        String valueTemplateStr = '{ id : "$someVar" }'
        ValueTemplate subjectUnderTest = new ValueTemplate(fieldResolverContextMock, valueTemplateStr)
        testDataSource.put("someVar", "TEST_VALUE")

        Set<String> requiredFields = Sets.newHashSet("someVar")
        fieldResolverContextMock.getRequiredFields() >> requiredFields

        when:
        String actual = subjectUnderTest.compile(testDataSource)

        then:
        actual == '{ id : "TEST_VALUE" }'
    }

    void compile_jsonStringWithNullVariable_success() {
        given:
        String valueTemplateStr = '{ id : "$someVar" }'
        ValueTemplate subjectUnderTest = new ValueTemplate(fieldResolverContextMock, valueTemplateStr)
        testDataSource.put("someVar", null)

        Set<String> requiredFields = Sets.newHashSet("someVar")
        fieldResolverContextMock.getRequiredFields() >> requiredFields

        when:
        String actual = subjectUnderTest.compile(testDataSource)

        then:
        actual == '{ id : null }'
    }

    void compile_jsonStringWithMultipleVariables_success() {
        given:
        String valueTemplateStr = '{ id : "$petId" name : "$petName" }'
        ValueTemplate subjectUnderTest = new ValueTemplate(fieldResolverContextMock, valueTemplateStr)
        testDataSource.put("petId", "pet-901")
        testDataSource.put("petName", null)

        Set<String> requiredFields = Sets.newHashSet("petId", "petName")
        fieldResolverContextMock.getRequiredFields() >> requiredFields

        when:
        String actual = subjectUnderTest.compile(testDataSource)

        then:
        actual == '{ id : "pet-901" name : null }'
    }

    void compile_jsonStringWithMultipleVariablesNotString_success() {
        given:
        String valueTemplateStr = '{ includeName : "$includeName" name : "$childCount" }'
        ValueTemplate subjectUnderTest = new ValueTemplate(fieldResolverContextMock, valueTemplateStr)
        testDataSource.put("includeName", true)
        testDataSource.put("childCount", 5)

        Set<String> requiredFields = Sets.newHashSet("includeName", "childCount")
        fieldResolverContextMock.getRequiredFields() >> requiredFields

        when:
        String actual = subjectUnderTest.compile(testDataSource)

        then:
        actual == '{ includeName : true name : 5 }'
    }
}
