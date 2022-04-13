package com.intuit.graphql.orchestrator.utils;

import static graphql.schema.GraphQLTypeUtil.isList;
import static graphql.schema.GraphQLTypeUtil.isNonNull;

import graphql.AssertException;
import graphql.Scalars;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.PropertyDataFetcherHelper;
import graphql.util.FpKit;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * The code from this is from graphql-java library AstValueHelper
 */
public class ValueUtil {
  public static Value<?> astFromValue(Object value, GraphQLType type) {
    if (value == null) {
      return null;
    }

    if (isNonNull(type)) {
      return handleNonNull(value, (GraphQLNonNull) type);
    }

    // Convert JavaScript array to GraphQL list. If the GraphQLType is a list, but
    // the value is not an array, convert the value using the list's item type.
    if (isList(type)) {
      return handleList(value, (GraphQLList) type);
    }

    // Populate the fields of the input object by creating ASTs from each value
    // in the JavaScript object according to the fields in the input type.
    if (type instanceof GraphQLInputObjectType) {
      return handleInputObject(value, (GraphQLInputObjectType) type);
    }

    if (!(type instanceof GraphQLScalarType || type instanceof GraphQLEnumType)) {
      throw new AssertException("Must provide Input Type, cannot use: " + type.getClass());
    }

    // Since value is an internally represented value, it must be serialized
    // to an externally represented value before converting into an AST.
    final Object serialized = serialize(type, value);
    if (isNullish(serialized)) {
      return null;
    }

    // Others serialize based on their corresponding JavaScript scalar types.
    if (serialized instanceof Boolean) {
      return BooleanValue.newBooleanValue().value((Boolean) serialized).build();
    }

    String stringValue = serialized.toString();
    // numbers can be Int or Float values.
    if (serialized instanceof Number) {
      return handleNumber(stringValue);
    }

    if (serialized instanceof String) {
      // Enum types use Enum literals.
      if (type instanceof GraphQLEnumType) {
        return EnumValue.newEnumValue().name(stringValue).build();
      }

      // ID types can use Int literals.
      if (type == Scalars.GraphQLID && stringValue.matches("^[0-9]+$")) {
        return IntValue.newIntValue().value(new BigInteger(stringValue)).build();
      }

      return StringValue.newStringValue().value(stringValue).build();
    }

    throw new AssertException("'Cannot convert value to AST: " + serialized);
  }

  private static Value<?> handleInputObject(Object javaValue, GraphQLInputObjectType type) {
    List<GraphQLInputObjectField> fields = type.getFields();
    List<ObjectField> fieldNodes = new ArrayList<>();
    fields.forEach(field -> {
      String fieldName = field.getName();
      GraphQLInputType fieldType = field.getType();
      Object fieldValueObj = PropertyDataFetcherHelper.getPropertyValue(fieldName, javaValue, fieldType);
      Value<?> nodeValue = astFromValue(fieldValueObj, fieldType);
      if (nodeValue != null) {

        fieldNodes.add(ObjectField.newObjectField().name(fieldName).value(nodeValue).build());
      }
    });
    return ObjectValue.newObjectValue().objectFields(fieldNodes).build();
  }

  private static Value<?> handleNumber(String stringValue) {
    if (stringValue.matches("^[0-9]+$")) {
      return IntValue.newIntValue().value(new BigInteger(stringValue)).build();
    } else {
      return FloatValue.newFloatValue().value(new BigDecimal(stringValue)).build();
    }
  }

  @SuppressWarnings("rawtypes")
  private static Value<?> handleList(Object _value, GraphQLList type) {
    GraphQLType itemType = type.getWrappedType();
    boolean isIterable = _value instanceof Iterable;
    if (isIterable || (_value != null && _value.getClass().isArray())) {
      Iterable<?> iterable = isIterable ? (Iterable<?>) _value : FpKit.toCollection(_value);
      List<Value> valuesNodes = new ArrayList<>();
      for (Object item : iterable) {
        Value<?> itemNode = astFromValue(item, itemType);
        if (itemNode != null) {
          valuesNodes.add(itemNode);
        }
      }
      return ArrayValue.newArrayValue().values(valuesNodes).build();
    }
    return astFromValue(_value, itemType);
  }

  private static Value<?> handleNonNull(Object _value, GraphQLNonNull type) {
    GraphQLType wrappedType = type.getWrappedType();
    return astFromValue(_value, wrappedType);
  }

  private static Object serialize(GraphQLType type, Object value) {
    if (type instanceof GraphQLScalarType) {
      return ((GraphQLScalarType) type).getCoercing().serialize(value);
    } else {
      return ((GraphQLEnumType) type).serialize(value);
    }
  }

  private static boolean isNullish(Object serialized) {
    if (serialized instanceof Number) {
      return Double.isNaN(((Number) serialized).doubleValue());
    }
    return serialized == null;
  }

}
