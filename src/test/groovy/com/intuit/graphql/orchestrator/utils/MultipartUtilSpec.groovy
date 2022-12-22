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
        //non deferred fields are in the first query
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    fieldA\n" +
                "    fieldB\n" +
                "  }\n" +
                "}\n"
        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    fieldC\n" +
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
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    aliasA: fieldA\n" +
                "  }\n" +
                "}\n"
        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    aliasB: fieldB\n" +
                "  }\n" +
                "}\n"
    }

    def "can split execution input with arguments"() {
        given:
        String query = "query { getFoo(id:\"inputA\") { fieldA fieldB@defer } }"

        when:
        ExecutionInput input = ExecutionInput.newExecutionInput(query).build()
        List<ExecutionInput> splitSet = MultipartUtil.splitMultipartExecutionInput(input)

        then:
        splitSet.size() == 2

        splitSet.get(0).query == "query {\n" +
                "  getFoo(id: \"inputA\") {\n" +
                "    fieldA\n" +
                "  }\n" +
                "}\n"

        splitSet.get(1).query == "query {\n" +
                "  getFoo(id: \"inputA\") {\n" +
                "    fieldB\n" +
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
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    fieldA\n" +
                "  }\n" +
                "}\n"
        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    fieldB\n" +
                "  }\n" +
                "}\n"
        splitSet.get(2).query == "query {\n" +
                "  queryA {\n" +
                "    fieldC\n" +
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
        splitSet.get(0).query == "query {\n" +
                "  queryA {\n" +
                "    fieldA\n" +
                "    objectField {\n" +
                "      fieldB\n" +
                "    }\n" +
                "  }\n" +
                "}\n"

        splitSet.get(1).query == "query {\n" +
                "  queryA {\n" +
                "    objectField {\n" +
                "      fieldC\n" +
                "    }\n" +
                "  }\n" +
                "}\n"
    }

    //todo
    def "prunes selections sets without fields after removing deferred fields"() {}

    //todo
    def "can split EI with variables" () {}

    //todo
    def "can split EI with inline fragment" () {}

    //todo
    def "can split EI with fragment spread"() {}

}
