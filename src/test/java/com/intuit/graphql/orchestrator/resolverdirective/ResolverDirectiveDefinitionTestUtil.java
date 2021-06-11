package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.ArrayValueWithVariable;
import com.intuit.graphql.graphQL.ObjectFieldWithVariable;
import com.intuit.graphql.graphQL.ObjectValueWithVariable;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;

public class ResolverDirectiveDefinitionTestUtil {

  /**
   * creates the 'field' part of @resolver directive.
   * e.g. @resolver(field: "pet")
   *
   * @param stringValue the value for the field which should be the field name of query operation of
   *                    a downstream service
   * @return an Argument instance representing the field part of @resolver
   */
  public static Argument createResolverField(String stringValue) {
    ValueWithVariable valueWithVariable = GraphQLFactoryDelegate.createValueWithVariable();
    valueWithVariable.setStringValue(stringValue);

    Argument field = GraphQLFactoryDelegate.createArgument();
    field.setName("field");
    field.setValueWithVariable(valueWithVariable);

    return field;
  }

  /**
   * creates the 'arguments' part @resolver directive with two entries.
   * e.g.  @resolver(field: "pet" arguments: [{name : "id", value: "$petId"}])
   *
   * @param name1 1st entry name field
   * @param value1 1st entry value field
   * @param name2 2nd entry name fjeld
   * @param value2 2nd entry name field
   * @return an Argument representing the 'arguments' part of @resolver
   */
  public static Argument createResolverArguments(String name1, String value1, String name2, String value2) {
    ArrayValueWithVariable arrayValueWithVariable = GraphQLFactoryDelegate.createArrayValueWithVariable();
    arrayValueWithVariable.getValueWithVariable().add(createArgumentsEntry(name1, value1));
    arrayValueWithVariable.getValueWithVariable().add(createArgumentsEntry(name2, value2));

    ValueWithVariable valueWithVariable = GraphQLFactoryDelegate.createValueWithVariable();
    valueWithVariable.setArrayValueWithVariable(arrayValueWithVariable);

    Argument arguments = GraphQLFactoryDelegate.createArgument();
    arguments.setName("arguments");
    arguments.setValueWithVariable(valueWithVariable);

    return arguments;
  }

  public static ValueWithVariable createArgumentsEntry(String name, String value) {
    ObjectFieldWithVariable namedField = createArgumentsEntryField("name", name);
    ObjectFieldWithVariable valueField = createArgumentsEntryField("value", value);

    ObjectValueWithVariable objectValueWithVariable = GraphQLFactoryDelegate.createObjectValueWithVariable();
    objectValueWithVariable.getObjectFieldWithVariable().add(namedField);
    objectValueWithVariable.getObjectFieldWithVariable().add(valueField);

    ValueWithVariable arrayEntryValueWithVariable = GraphQLFactoryDelegate.createValueWithVariable();
    arrayEntryValueWithVariable.setObjectValueWithVariable(objectValueWithVariable);

    return arrayEntryValueWithVariable;
  }

  public static ObjectFieldWithVariable createArgumentsEntryField(String fieldName, String fieldValue) {
    ValueWithVariable valueWithVariable = GraphQLFactoryDelegate.createValueWithVariable();
    valueWithVariable.setStringValue(fieldValue);

    ObjectFieldWithVariable fieldWithObjectVariable = GraphQLFactoryDelegate
        .createObjectFieldWithVariable();
    fieldWithObjectVariable.setName(fieldName);
    fieldWithObjectVariable.setValueWithVariable(valueWithVariable);

    return fieldWithObjectVariable;
  }
}