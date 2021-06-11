package com.intuit.graphql.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * Covers test for ObjectTypeExtension, InterfaceTypeExtension, UnionTypeExtension, EnumTypeExtension,
 * InputObjectTypeExtension TODO ScalarTypeExtension.
 */
@Slf4j
public class TypeExtensionsTest {

  @Test
  public void canQueryWithResultTypeExtendedInterface() throws Exception {
    // person is of type PersonInterface which is extended to add field address
    TestCase testCase = TestCase.newTestCase()
        .service(new ExtendedPersonTestService())
        .query("{ person { __typename id name phoneNumber hobby address { city } }  }")
        .build();

    testCase.run();
    testCase.assertHashNoErrors();
    testCase.assertHasData();

    Map<String, Object> person = (Map<String, Object>) testCase.getDataField("person");
    assertThat(person.keySet()).hasSize(6);
    assertThat(person.get("__typename")).isEqualTo("Teacher");
    assertThat(person.get("id")).isEqualTo("teacher-1");
    assertThat(person.get("name")).isEqualTo("Mr. Teacher");
    assertThat(person.get("phoneNumber")).isEqualTo("415 1234567");
    assertThat(person.get("hobby")).isEqualTo("BIKING");
    assertThat(person.get("address")).isNotNull(); // from extend PersonInterface
  }

  @Test
  public void canQueryWithResultTypeExtendedUnionAndObject() throws Exception {
    // firstEmployedPerson is of type EmployedPersonsUnion
    // EmployedPersonsUnion initially created with one possibler type Teacher then extended to add Actor
    // THe query returns an data of type Teacher
    TestCase testCase = TestCase.newTestCase()
        .service(new ExtendedPersonTestService())
        .query("{"
            + "firstEmployedPerson { "
            + "... on Teacher { id name phoneNumber hobby school subjects } "
            + "... on Actor { id name phoneNumber hobby movies } "
            + "} }")
        .build();

    testCase.run();
    testCase.assertHashNoErrors();
    testCase.assertHasData();

    Map<String, Object> teacher = (Map<String, Object>) testCase.getDataField("firstEmployedPerson");
    assertThat(teacher.keySet()).hasSize(6);
    assertThat(teacher.get("id")).isEqualTo("teacher-1");
    assertThat(teacher.get("name")).isEqualTo("Mr. Teacher");
    assertThat(teacher.get("phoneNumber")).isEqualTo("415 1234567");
    assertThat(teacher.get("hobby")).isEqualTo("BIKING");
    assertThat(teacher.get("school")).isNotNull(); // from extend Teacher
    assertThat(teacher.get("subjects")).isNotNull(); // from another extend Teacher
    assertThat((List) teacher.get("subjects")).hasSize(2);
    assertThat((List) teacher.get("subjects")).containsExactly("Accounting", "Chemistry");
  }

  @Test
  public void canQueryWithResultTypeExtendedUnion() throws Exception {
    // secondEmployedPerson is of type EmployedPersonsUnion.
    // EmployedPersonsUnion initially created with one possibler type Teacher then extended to add Actor
    // THe query returns an data of type Actor
    TestCase testCase = TestCase.newTestCase()
        .service(new ExtendedPersonTestService())
        .query("{"
            + "secondEmployedPerson { "
            + "... on Teacher { id name phoneNumber hobby school } "
            + "... on Actor { id name phoneNumber hobby movies } "
            + "} }")
        .build();

    testCase.run();
    testCase.assertHashNoErrors();
    testCase.assertHasData();

    Map<String, Object> actor = (Map<String, Object>) testCase.getDataField("secondEmployedPerson");
    assertThat(actor.keySet()).hasSize(5);
    assertThat(actor.get("id")).isEqualTo("actor-1");
    assertThat(actor.get("name")).isEqualTo("Mr. Actor");
    assertThat(actor.get("phoneNumber")).isEqualTo("415 1111111");
    assertThat(actor.get("hobby")).isEqualTo("PHOTOGRAPHY");
    assertThat(actor.get("movies")).isNotNull(); // from extend PersonInterface
  }

  @Test
  public void canAddWithExtendInputObjectType() throws Exception {
    // InputTeacher is extended.
    // THe query returns an the data submitted
    TestCase testCase = TestCase.newTestCase()
        .service(new ExtendedPersonTestService())
        .query("mutation AddNewTeacher($newteacher: InputTeacher!) { "
            + "addTeacher(teacher: $newteacher) { id name phoneNumber address { street city country }  hobby school subjects } "
            + "}")
        .variables(ImmutableMap.of("newteacher", getNewTeacher()))
        .build();

    testCase.run();
    testCase.assertHashNoErrors();
    testCase.assertHasData();

    Map<String, Object> actor = (Map<String, Object>) testCase.getDataField("addTeacher");
    assertThat(actor.keySet()).hasSize(7);
    assertThat(actor.get("id")).isEqualTo("teacher-2");
    assertThat(actor.get("name")).isEqualTo("Mr. ComSci Teacher");
    assertThat(actor.get("phoneNumber")).isEqualTo("415 7654321");
    assertThat(actor.get("hobby")).isEqualTo("BIKING");
    assertThat(actor.get("school")).isEqualTo("University Of San Carlos");
    assertThat(actor.get("subjects")).isNotNull();
    assertThat((List) actor.get("subjects")).containsExactly("Operating Systems", "Algorithm Analysis and Design");
  }

  /**
   * METHODS FOR CREATED INPUT DATA
   **/

  private Object getNewTeacher() {

    Map<String, Object> addressMap = new HashMap<>();
    addressMap.put("id", "address-2");
    addressMap.put("street", "Java Street");
    addressMap.put("city", "San Diego");
    addressMap.put("zip", "12345");
    addressMap.put("state", "CA");
    addressMap.put("country", "United States");

    Map<String, Object> teacher = new HashMap<>();
    teacher.put("id", "teacher-2");
    teacher.put("name", "Mr. ComSci Teacher");
    teacher.put("address", addressMap);
    teacher.put("phoneNumber", "415 7654321");
    teacher.put("hobby", "BIKING");
    teacher.put("school", "University Of San Carlos");
    teacher.put("subjects", Arrays.asList("Operating Systems", "Algorithm Analysis and Design"));
    return teacher;
  }

  /**
   * INNER CLASS
   **/

  class ExtendedPersonTestService implements ServiceProvider {

    @Override
    public String getNameSpace() {
      return "PERSON";
    }

    @Override
    public Map<String, String> sdlFiles() {
      return TestHelper.getFileMapFromList(
          "top_level/type-extensions/schema.graphqls",
          "top_level/type-extensions/schema-actor.graphqls",
          "top_level/type-extensions/schema-operations.graphqls",
          "top_level/type-extensions/schema-person.graphqls",
          "top_level/type-extensions/schema-person-extensions1.graphqls",
          "top_level/type-extensions/schema-person-extensions2.graphqls",
          "top_level/type-extensions/schema-teacher.graphqls",
          "top_level/type-extensions/schema-teacher-extension.graphqls");
    }

    @Override
    public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
        GraphQLContext context) {

      Document document = (Document) executionInput.getRoot();
      OperationDefinition opDep = (OperationDefinition) document.getDefinitions().get(0);

      if (opDep.getOperation().equals(Operation.QUERY)) {
        Map<String, Object> data = new HashMap<>();
        opDep.getSelectionSet().getSelections().stream().forEach(selection -> {
          Field field = (Field) selection;
          String queryFieldName = field.getName();
          if ("person".equals(queryFieldName)) {
            data.put(queryFieldName, getTeacher());
          }

          if ("firstEmployedPerson".equals(queryFieldName)) {
            data.put(queryFieldName, getTeacher());
          }

          if ("secondEmployedPerson".equals(queryFieldName)) {
            data.put(queryFieldName, getActor());
          }
        });
        return CompletableFuture.completedFuture(ImmutableMap.of("data", data));
      }

      if (opDep.getOperation().equals(Operation.MUTATION)) {
        Map<String, Object> newteacher = (Map<String, Object>) executionInput.getVariables().get("newteacher");
        return CompletableFuture
            .completedFuture(ImmutableMap.of("data", ImmutableMap.of("addTeacher", newteacher)));

      }

      return CompletableFuture.completedFuture(ImmutableMap.of("data", null));
    }

    private Object getTeacher() {

      Map<String, Object> addressMap = new HashMap<>();
      addressMap.put("id", "address-1");
      addressMap.put("street", "Lombok Street");
      addressMap.put("city", "San Diego");
      addressMap.put("zip", "12345");
      addressMap.put("state", "CA");
      addressMap.put("country", "United States");

      Map<String, Object> person = new HashMap<>();
      person.put("__typename", "Teacher");
      person.put("id", "teacher-1");
      person.put("name", "Mr. Teacher");
      person.put("address", addressMap);
      person.put("phoneNumber", "415 1234567");
      person.put("hobby", "BIKING");
      person.put("school", "University Of San Diego");
      person.put("subjects", Arrays.asList("Accounting", "Chemistry"));
      return person;
    }

    private Object getActor() {
      Map<String, Object> addressMap = new HashMap<>();
      addressMap.put("id", "address-1");
      addressMap.put("street", "Secret Street");
      addressMap.put("city", "Los Angeles");
      addressMap.put("zip", "12345");
      addressMap.put("state", "CA");
      addressMap.put("country", "United States");

      Map<String, Object> actor = new HashMap<>();
      actor.put("__typename", "Actor");
      actor.put("id", "actor-1");
      actor.put("name", "Mr. Actor");
      actor.put("address", addressMap);
      actor.put("phoneNumber", "415 1111111");
      actor.put("hobby", "PHOTOGRAPHY");
      actor.put("school", "University Of Los Angeles");
      actor.put("movies", Arrays.asList("Goodfellas", "Batman"));
      return actor;
    }
  }

}
