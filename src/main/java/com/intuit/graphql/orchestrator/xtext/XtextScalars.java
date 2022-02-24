package com.intuit.graphql.orchestrator.xtext;

import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createScalarTypeDefinition;
import static org.eclipse.xtext.EcoreUtil2.copy;

import com.intuit.graphql.graphQL.ScalarTypeDefinition;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides all supported standard scalars in the GraphQL specification and extended scalars in GraphQL-Java as Xtext
 * ScalarTypeDefinitions. All methods return <b>new instances</b> for the respective scalar type.
 */
public class XtextScalars {

  public static Set<ScalarTypeDefinition> STANDARD_SCALARS = new HashSet<>(12);

  private static final ScalarTypeDefinition INT_TYPE_DEFINITION = createScalarTypeDefinition();
  private static final ScalarTypeDefinition STRING_TYPE_DEFINITION = createScalarTypeDefinition();
  private static final ScalarTypeDefinition FLOAT_TYPE_DEFINITION = createScalarTypeDefinition();
  private static final ScalarTypeDefinition BOOLEAN_TYPE_DEFINITION = createScalarTypeDefinition();
  private static final ScalarTypeDefinition ID_TYPE_DEFINITION = createScalarTypeDefinition();
  private static final ScalarTypeDefinition LONG_TYPE_DEFINITION = createScalarTypeDefinition();
  private static final ScalarTypeDefinition BIG_INT_TYPE_DEFINITION = createScalarTypeDefinition();
  private static final ScalarTypeDefinition BIG_DECIMAL_TYPE_DEFINITION = createScalarTypeDefinition();
  private static final ScalarTypeDefinition SHORT_TYPE_DEFINITION = createScalarTypeDefinition();
  private static final ScalarTypeDefinition CHAR_TYPE_DEFINITION = createScalarTypeDefinition();
  private static final ScalarTypeDefinition BYTE_TYPE_DEFINITION = createScalarTypeDefinition();
  private static final ScalarTypeDefinition FIELDSET_TYPE_DEFINITION = createScalarTypeDefinition();

  static {
    INT_TYPE_DEFINITION.setName("Int");
    STRING_TYPE_DEFINITION.setName("String");
    FLOAT_TYPE_DEFINITION.setName("Float");
    BOOLEAN_TYPE_DEFINITION.setName("Boolean");
    ID_TYPE_DEFINITION.setName("ID");
    LONG_TYPE_DEFINITION.setName("Long");
    BIG_INT_TYPE_DEFINITION.setName("BigInteger");
    BIG_DECIMAL_TYPE_DEFINITION.setName("BigDecimal");
    SHORT_TYPE_DEFINITION.setName("Short");
    CHAR_TYPE_DEFINITION.setName("Char");
    BYTE_TYPE_DEFINITION.setName("Byte");
    FIELDSET_TYPE_DEFINITION.setName("_FieldSet");
  }

  static {
    STANDARD_SCALARS.add(newIntType());
    STANDARD_SCALARS.add(newStringType());
    STANDARD_SCALARS.add(newFloatType());
    STANDARD_SCALARS.add(newBooleanType());
    STANDARD_SCALARS.add(newIdType());
    STANDARD_SCALARS.add(newLongType());
    STANDARD_SCALARS.add(newBigIntType());
    STANDARD_SCALARS.add(newBigDecimalType());
    STANDARD_SCALARS.add(newShortType());
    STANDARD_SCALARS.add(newCharType());
    STANDARD_SCALARS.add(newByteType());
    STANDARD_SCALARS.add(newFieldSetType());
  }

  public static ScalarTypeDefinition newStringType() {
    return copy(STRING_TYPE_DEFINITION);
  }

  public static ScalarTypeDefinition newFloatType() {
    return copy(FLOAT_TYPE_DEFINITION);
  }

  public static ScalarTypeDefinition newBooleanType() {
    return copy(BOOLEAN_TYPE_DEFINITION);
  }

  public static ScalarTypeDefinition newIdType() {
    return copy(ID_TYPE_DEFINITION);
  }

  public static ScalarTypeDefinition newLongType() {
    return copy(LONG_TYPE_DEFINITION);
  }

  public static ScalarTypeDefinition newBigIntType() {
    return copy(BIG_INT_TYPE_DEFINITION);
  }

  public static ScalarTypeDefinition newBigDecimalType() {
    return copy(BIG_DECIMAL_TYPE_DEFINITION);
  }

  public static ScalarTypeDefinition newShortType() {
    return copy(SHORT_TYPE_DEFINITION);
  }

  public static ScalarTypeDefinition newCharType() {
    return copy(CHAR_TYPE_DEFINITION);
  }

  public static ScalarTypeDefinition newByteType() {
    return copy(BYTE_TYPE_DEFINITION);
  }

  public static ScalarTypeDefinition newIntType() {
    return copy(INT_TYPE_DEFINITION);
  }

  public static ScalarTypeDefinition newFieldSetType() {
    return copy(FIELDSET_TYPE_DEFINITION);
  }
}