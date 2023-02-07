package com.intuit.graphql.orchestrator.utils


import graphql.ExecutionInput
import lombok.extern.slf4j.Slf4j
import spock.lang.Specification
/**
 * Covers test for ObjectTypeExtension, InterfaceTypeExtension, UnionTypeExtension, EnumTypeExtension,
 * InputObjectTypeExtension TODO ScalarTypeExtension.
 */
@Slf4j
class MultipartUtilSpec extends Specification {

    def "can split Execution input"() {
        given:
        String query = "query { queryA { fieldA fieldB fieldC @defer } }"

        when:
        ExecutionInput input = ExecutionInput.newExecutionInput(query).build()
        List<ExecutionInput> splitSet = MultipartUtil.splitMultipartExecutionInput(input)

        then:
        splitSet.size() == 2
        splitSet.get(0).query == query
        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    fieldC\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "can split EI with alias selections"(){
        given:
        String query = "query { queryA { aliasA: fieldA  aliasB: fieldB @defer } }"

        when:
        ExecutionInput input = ExecutionInput.newExecutionInput(query).build()
        List<ExecutionInput> splitSet = MultipartUtil.splitMultipartExecutionInput(input)

        then:
        splitSet.size() == 2
        splitSet.get(0).query == query
        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    aliasB: fieldB\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "can split execution input with arguments"() {
        given:
        String query = "query { getFoo(id: \"inputA\") { fieldA fieldB @defer } }"

        when:
        ExecutionInput input = ExecutionInput.newExecutionInput(query).build()
        List<ExecutionInput> splitSet = MultipartUtil.splitMultipartExecutionInput(input)

        then:
        splitSet.size() == 2

        splitSet.get(0).query == query

        splitSet.get(1).query == "query {\n" +
                "  getFoo(id: \"inputA\") {\n" +
                "    fieldB\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "can split EI with multiple defer on same level"() {
        given:
        String query = "query { queryA { fieldA fieldB @defer fieldC @defer } }"

        when:
        ExecutionInput input = ExecutionInput.newExecutionInput(query).build()
        List<ExecutionInput> splitSet = MultipartUtil.splitMultipartExecutionInput(input)

        then:
        splitSet.size() == 3
        splitSet.get(0).query == query
        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    fieldB\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
        splitSet.get(2).query == "query {\n" +
                "  queryA {\n" +
                "    fieldC\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"


    }

    def "can split EI with nested deferred selections"() {
        given:
        String query = "query { queryA { fieldA objectField { fieldB fieldC @defer } } }"

        when:
        ExecutionInput input = ExecutionInput.newExecutionInput(query).build()
        List<ExecutionInput> splitSet = MultipartUtil.splitMultipartExecutionInput(input)

        then:
        splitSet.size() == 2
        splitSet.get(0).query == query
        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    objectField {\n" +
                "      fieldC\n" +
                "      __typename\n" +
                "    }\n" +
                "    __typename\n" +
                "  }\n" +
                "}\n"
    }

    def "Does not split EI when if arg is false"() {
        given:
        String query = "query { queryA { fieldA fieldB fieldC @defer(if: false) } }"

        when:
        ExecutionInput input = ExecutionInput.newExecutionInput(query).build()
        List<ExecutionInput> splitSet = MultipartUtil.splitMultipartExecutionInput(input)

        then:
        splitSet.size() == 1
        splitSet.get(0).query == query
    }

    //todo
    def "prunes selections sets without fields after removing deferred fields"() {}

    //todo
    def "can split EI with variables" () {}

    //todo
    def "can split EI with inline fragment" () {}

    //todo
    def "can split EI with fragment spread"() {}


    // end to end tests


}
