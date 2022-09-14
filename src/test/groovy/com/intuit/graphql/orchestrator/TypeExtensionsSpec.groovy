package com.intuit.graphql.orchestrator

import com.google.common.collect.ImmutableMap
import graphql.ExecutionInput
import graphql.GraphQLContext
import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.language.OperationDefinition.Operation
import lombok.extern.slf4j.Slf4j
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

/**
 * Covers test for ObjectTypeExtension, InterfaceTypeExtension, UnionTypeExtension, EnumTypeExtension,
 * InputObjectTypeExtension TODO ScalarTypeExtension.
 */
@Slf4j
class TypeExtensionsSpec extends Specification {

    def "can Query With Result Type Extended Interface"() {
        given:
        // person is of type PersonInterface which is extended to add field address
        TestCase testCase = TestCase.newTestCase()
                .service(new ExtendedPersonTestService())
                .query('''
                    {
                        person {
                            __typename id name phoneNumber hobby 
                            address {
                                city
                            }
                        }
                    }
                ''')
                .build()

        when:
        testCase.run()
        testCase.assertHashNoErrors()
        testCase.assertHasData()

        then:
        Map<String, Object> person = (Map<String, Object>) testCase.getDataField("person")
        person.keySet().size() == 6
        person.get("__typename") == "Teacher"
        person.get("id") == "teacher-1"
        person.get("name") == "Mr. Teacher"
        person.get("phoneNumber") == "415 1234567"
        person.get("hobby") == "BIKING"
        person.get("address") != null
    }

    def "can Query With Result Type Extended Union And Object"() {
        given:
        // firstEmployedPerson is of type EmployedPersonsUnion
        // EmployedPersonsUnion initially created with one possibler type Teacher then extended to add Actor
        // The query returns an data of type Teacher
        TestCase testCase = TestCase.newTestCase()
                .service(new ExtendedPersonTestService())
                .query('''
                    {
                        firstEmployedPerson {
                            ... on Teacher { 
                                id name phoneNumber hobby school subjects 
                            }
                            ... on Actor {
                                id name phoneNumber hobby movies 
                            }
                        }
                    }
                ''')
                .build()

        when:
        testCase.run()
        testCase.assertHashNoErrors()
        testCase.assertHasData()

        then:
        Map<String, Object> teacher = (Map<String, Object>) testCase.getDataField("firstEmployedPerson")
        teacher.keySet().size() == 6
        teacher.get("id") == "teacher-1"
        teacher.get("name") == "Mr. Teacher"
        teacher.get("phoneNumber") == "415 1234567"
        teacher.get("hobby") == "BIKING"
        teacher.get("school") != null
        teacher.get("subjects") != null // from another extend Teacher
        ((List) teacher.get("subjects")).size() == 2
        ((List) teacher.get("subjects")).containsAll("Accounting", "Chemistry")
    }

    def "can Query With Result Type Extended Union"() {
        when:
        // secondEmployedPerson is of type EmployedPersonsUnion.
        // EmployedPersonsUnion initially created with one possibler type Teacher then extended to add Actor
        // The query returns an data of type Actor
        TestCase testCase = TestCase.newTestCase()
                .service(new ExtendedPersonTestService())
                .query('''
                    {
                        secondEmployedPerson {
                            ... on Teacher { id name phoneNumber hobby school }
                            ... on Actor { id name phoneNumber hobby movies }
                        }
                    }
                ''')
                .build()

        testCase.run()
        testCase.assertHashNoErrors()
        testCase.assertHasData()

        then:
        Map<String, Object> actor = (Map<String, Object>) testCase.getDataField("secondEmployedPerson")
        actor.keySet().size() == 5
        actor.get("id") == "actor-1"
        actor.get("name") == "Mr. Actor"
        actor.get("phoneNumber") == "415 1111111"
        actor.get("hobby") == "PHOTOGRAPHY"
        actor.get("movies") != null // from extend PersonInterface
    }

    def "can Add With Extend Input Object Type"() {
        given:
        // InputTeacher is extended.
        // The query returns an the data submitted
        TestCase testCase = TestCase.newTestCase()
                .service(new ExtendedPersonTestService())
                .query('''
                    mutation AddNewTeacher($newteacher: InputTeacher!) {
                        addTeacher(teacher: $newteacher) {
                            id 
                            name 
                            phoneNumber 
                            address { 
                                street 
                                city 
                                country
                            }
                            hobby 
                            school 
                            subjects
                        }
                    }
                ''')
                .variables(ImmutableMap.of("newteacher", getNewTeacher()))
                .build()

        when:
        testCase.run()
        testCase.assertHashNoErrors()
        testCase.assertHasData()

        then:
        Map<String, Object> actor = (Map<String, Object>) testCase.getDataField("addTeacher")
        actor.keySet().size() == 7
        actor.get("id") == "teacher-2"
        actor.get("name") == "Mr. ComSci Teacher"
        actor.get("phoneNumber") == "415 7654321"
        actor.get("hobby") == "BIKING"
        actor.get("school") == "University Of San Carlos"
        actor.get("subjects") != null
        ((List) actor.get("subjects")).size() == 2
        ((List) actor.get("subjects")).containsAll("Operating Systems", "Algorithm Analysis and Design")
    }

    /**
     * METHODS FOR CREATED INPUT DATA
     **/

    private Object getNewTeacher() {
        Map<String, Object> addressMap = new HashMap<>()
        addressMap.put("id", "address-2")
        addressMap.put("street", "Java Street")
        addressMap.put("city", "San Diego")
        addressMap.put("zip", "12345")
        addressMap.put("state", "CA")
        addressMap.put("country", "United States")

        Map<String, Object> teacher = new HashMap<>()
        teacher.put("id", "teacher-2")
        teacher.put("name", "Mr. ComSci Teacher")
        teacher.put("address", addressMap)
        teacher.put("phoneNumber", "415 7654321")
        teacher.put("hobby", "BIKING")
        teacher.put("school", "University Of San Carlos")
        teacher.put("subjects", Arrays.asList("Operating Systems", "Algorithm Analysis and Design"))

        return teacher
    }

    /**
     * INNER CLASS
     **/

    class ExtendedPersonTestService implements ServiceProvider {

        @Override
        String getNameSpace() {
            return "PERSON"
        }

        @Override
        Map<String, String> sdlFiles() {
            return TestHelper.getFileMapFromList(
                    "top_level/type-extensions/schema.graphqls",
                    "top_level/type-extensions/schema-actor.graphqls",
                    "top_level/type-extensions/schema-operations.graphqls",
                    "top_level/type-extensions/schema-person.graphqls",
                    "top_level/type-extensions/schema-person-extensions1.graphqls",
                    "top_level/type-extensions/schema-person-extensions2.graphqls",
                    "top_level/type-extensions/schema-teacher.graphqls",
                    "top_level/type-extensions/schema-teacher-extension.graphqls")
        }

        @Override
        CompletableFuture<Map<String, Object>> query(
                ExecutionInput executionInput, GraphQLContext context) {

            Document document = (Document) executionInput.getRoot()
            OperationDefinition opDep = (OperationDefinition) document.getDefinitions().get(0)

            if (opDep.getOperation().equals(Operation.QUERY)) {
                Map<String, Object> data = new HashMap<>()
                opDep.getSelectionSet().getSelections().stream().forEach({ selection ->
                    Field field = (Field) selection
                    String queryFieldName = field.getName()
                    if ("person".equals(queryFieldName)) {
                        data.put(queryFieldName, getTeacher())
                    }

                    if ("firstEmployedPerson".equals(queryFieldName)) {
                        data.put(queryFieldName, getTeacher())
                    }

                    if ("secondEmployedPerson".equals(queryFieldName)) {
                        data.put(queryFieldName, getActor())
                    }
                })
                return CompletableFuture.completedFuture(
                        ImmutableMap.of("data", data))
            }

            if (opDep.getOperation().equals(Operation.MUTATION)) {
                Map<String, Object> newteacher = (Map<String, Object>) executionInput.getVariables().get("newteacher")
                return CompletableFuture.completedFuture(
                        ImmutableMap.of("data", ImmutableMap.of("addTeacher", newteacher)))

            }

            return CompletableFuture.completedFuture(
                    ImmutableMap.of("data", null))
        }

        private Object getTeacher() {
            Map<String, Object> addressMap = new HashMap<>()
            addressMap.put("id", "address-1")
            addressMap.put("street", "Lombok Street")
            addressMap.put("city", "San Diego")
            addressMap.put("zip", "12345")
            addressMap.put("state", "CA")
            addressMap.put("country", "United States")

            Map<String, Object> person = new HashMap<>()
            person.put("__typename", "Teacher")
            person.put("id", "teacher-1")
            person.put("name", "Mr. Teacher")
            person.put("address", addressMap)
            person.put("phoneNumber", "415 1234567")
            person.put("hobby", "BIKING")
            person.put("school", "University Of San Diego")
            person.put("subjects", Arrays.asList("Accounting", "Chemistry"))

            return person
        }

        private Object getActor() {
            Map<String, Object> addressMap = new HashMap<>()
            addressMap.put("id", "address-1")
            addressMap.put("street", "Secret Street")
            addressMap.put("city", "Los Angeles")
            addressMap.put("zip", "12345")
            addressMap.put("state", "CA")
            addressMap.put("country", "United States")

            Map<String, Object> actor = new HashMap<>()
            actor.put("__typename", "Actor")
            actor.put("id", "actor-1")
            actor.put("name", "Mr. Actor")
            actor.put("address", addressMap)
            actor.put("phoneNumber", "415 1111111")
            actor.put("hobby", "PHOTOGRAPHY")
            actor.put("school", "University Of Los Angeles")
            actor.put("movies", Arrays.asList("Goodfellas", "Batman"))

            return actor
        }
    }

}
