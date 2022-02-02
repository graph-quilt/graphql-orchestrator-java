package com.intuit.graphql.orchestrator.utils;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.createNamedType;
import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.toDescriptiveString;
import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.graphql.graphQL.ArgumentsDefinition;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.DirectiveDefinition;
import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InputObjectTypeDefinition;
import com.intuit.graphql.graphQL.InputValueDefinition;
import com.intuit.graphql.graphQL.ListType;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.PrimitiveType;
import com.intuit.graphql.graphQL.ScalarTypeDefinition;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import com.intuit.graphql.orchestrator.xtext.XtextScalars;
import org.junit.Test;

public class XtextTypeUtilsTest {

  @Test
  public void compareWrappedTypesTest() {

    ListType listType = GraphQLFactoryDelegate.createListType();
    ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition.setName("Arg1");
    NamedType namedType = createNamedType(objectTypeDefinition);
    listType.setType(namedType); //[Arg]

    ListType outerlistType = GraphQLFactoryDelegate.createListType();
    outerlistType.setType(listType); //[[Arg]]

    assertThat(XtextTypeUtils.compareTypes(listType, outerlistType)).isFalse();
  }

  @Test
  public void compareNonNullTypesTest() {

    ListType listType = GraphQLFactoryDelegate.createListType();
    ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition.setName("Arg1");
    NamedType namedType = createNamedType(objectTypeDefinition);
    namedType.setNonNull(true);
    listType.setType(namedType); //[Arg!]

    ListType listType2 = GraphQLFactoryDelegate.createListType();
    ObjectTypeDefinition objectTypeDefinition2 = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition2.setName("Arg1");
    NamedType namedType2 = createNamedType(objectTypeDefinition2);
    listType2.setType(namedType2); //[Arg]

    assertThat(XtextTypeUtils.compareTypes(listType, listType2)).isFalse();
  }

  @Test
  public void compareEqualWrappedNonNullTypesTest() {

    ListType listType = GraphQLFactoryDelegate.createListType();
    ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition.setName("Arg1");
    NamedType namedType = createNamedType(objectTypeDefinition);
    namedType.setNonNull(true);
    listType.setType(namedType);
    listType.setNonNull(true);//[Arg!]!

    ListType listType2 = GraphQLFactoryDelegate.createListType();
    ObjectTypeDefinition objectTypeDefinition2 = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition2.setName("Arg1");
    NamedType namedType2 = createNamedType(objectTypeDefinition2);
    namedType2.setNonNull(true);
    listType2.setType(namedType2);
    listType2.setNonNull(true); //[Arg!]!

    assertThat(XtextTypeUtils.compareTypes(listType, listType2)).isTrue();
  }

  @Test
  public void generateArgumentsDefinitionStringTest() {

    final ArgumentsDefinition argumentsDefinition = GraphQLFactoryDelegate.createArgumentsDefinition();
    InputValueDefinition ivf = GraphQLFactoryDelegate.createInputValueDefinition();
    ivf.setName("argument1");
    ivf.setDesc("desc1");
    argumentsDefinition.getInputValueDefinition().add(ivf);

    InputValueDefinition ivf2 = GraphQLFactoryDelegate.createInputValueDefinition();
    ivf2.setName("argument2");
    ivf2.setDesc("desc2");
    argumentsDefinition.getInputValueDefinition().add(ivf2);

    assertThat(toDescriptiveString(argumentsDefinition)).contains("argument1").contains("desc1").contains("argument2")
        .contains("desc2");
  }

  @Test
  public void generateDirectivesStringTest() {

    FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition();

    final DirectiveDefinition directiveDefinition = GraphQLFactoryDelegate.createDirectiveDefinition();
    directiveDefinition.setName("directive1");
    directiveDefinition.setDesc("description1");

    Directive directive = GraphQLFactoryDelegate.createDirective();
    directive.setDefinition(directiveDefinition);

    final DirectiveDefinition directiveDefinition2 = GraphQLFactoryDelegate.createDirectiveDefinition();
    directiveDefinition2.setName("directive2");
    directiveDefinition2.setDesc("description2");

    Directive directive2 = GraphQLFactoryDelegate.createDirective();
    directive2.setDefinition(directiveDefinition2);

    fieldDefinition.getDirectives().add(directive);
    fieldDefinition.getDirectives().add(directive2);

    assertThat(toDescriptiveString(fieldDefinition.getDirectives())).contains("directive1").contains("description1")
        .contains("directive2").contains("description2");
  }

  @Test
  public void throwsExceptionForObjectTypeNotValidAsInput() {
    ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition.setName("Arg1");
    NamedType namedType = createNamedType(objectTypeDefinition);

    ListType listType = GraphQLFactoryDelegate.createListType();
    listType.setType(namedType);

    assertThat(XtextTypeUtils.isValidInputType(listType)).isFalse();
  }

  @Test
  public void InputObjectTypeIsValidAsInput() {
    InputObjectTypeDefinition inputObjectTypeDefinition = GraphQLFactoryDelegate.createInputObjectTypeDefinition();
    inputObjectTypeDefinition.setName("Arg1");
    NamedType namedType = createNamedType(inputObjectTypeDefinition);

    ListType listType = GraphQLFactoryDelegate.createListType();
    listType.setType(namedType);
    assertThat(XtextTypeUtils.isValidInputType(listType)).isTrue();
  }

  @Test
  public void EnumTypeIsValidAsInput() {
    EnumTypeDefinition enumTypeDefinition = GraphQLFactoryDelegate.createEnumTypeDefinition();
    enumTypeDefinition.setName("Arg1");
    NamedType namedType = createNamedType(enumTypeDefinition);
    assertThat(XtextTypeUtils.isValidInputType(namedType)).isTrue();
  }

  @Test
  public void ScalarTypeIsValidAsInput() {
    PrimitiveType primitiveType = GraphQLFactoryDelegate.createPrimitiveType();
    primitiveType.setType("String");
    assertThat(XtextTypeUtils.isValidInputType(primitiveType)).isTrue();
  }

  @Test
  public void ScalarTypeDefinitionIsValidAsInput() {
    ScalarTypeDefinition scalarTypeDefinition = GraphQLFactoryDelegate.createScalarTypeDefinition();
    scalarTypeDefinition.setName("TestScalar");
    NamedType namedType = createNamedType(scalarTypeDefinition);
    assertThat(XtextTypeUtils.isValidInputType(namedType)).isTrue();
  }

  @Test
  public void isCompatibleScalarsTest() {

    PrimitiveType primitiveTypeStub =  GraphQLFactoryDelegate.createPrimitiveType();
    primitiveTypeStub.setType("String");

    PrimitiveType primitiveTypeTarget = GraphQLFactoryDelegate.createPrimitiveType();
    primitiveTypeTarget.setType("String");

    // both nullable
    assertThat(XtextTypeUtils.isCompatible(primitiveTypeStub, primitiveTypeTarget)).isTrue();

    primitiveTypeStub.setNonNull(false);
    primitiveTypeTarget.setNonNull(true);
    assertThat(XtextTypeUtils.isCompatible(primitiveTypeStub, primitiveTypeTarget)).isTrue();

    // stud type should be less restrictive
    primitiveTypeStub.setNonNull(true);
    primitiveTypeTarget.setNonNull(false);
    assertThat(XtextTypeUtils.isCompatible(primitiveTypeStub, primitiveTypeTarget)).isFalse();

    primitiveTypeStub.setNonNull(true);
    primitiveTypeTarget.setNonNull(true);
    assertThat(XtextTypeUtils.isCompatible(primitiveTypeStub, primitiveTypeTarget)).isTrue();
  }

  @Test
  public void isCompatibleWrappedTypesTest() {

    ListType listType = GraphQLFactoryDelegate.createListType();
    ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition.setName("Arg1");
    NamedType namedType = createNamedType(objectTypeDefinition);
    listType.setType(namedType); //[Arg]

    ListType outerlistType = GraphQLFactoryDelegate.createListType();
    outerlistType.setType(listType); //[[Arg]]

    assertThat(XtextTypeUtils.isCompatible(listType, outerlistType)).isFalse();
    assertThat(XtextTypeUtils.isCompatible(outerlistType, listType)).isFalse();
  }

  @Test
  public void isCompatibleWrappedNonNullTypesTest() {

    ListType listType = GraphQLFactoryDelegate.createListType();
    ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition.setName("Arg1");
    NamedType namedType = createNamedType(objectTypeDefinition);
    namedType.setNonNull(true);
    listType.setType(namedType); //[Arg!]

    ListType listType2 = GraphQLFactoryDelegate.createListType();
    ObjectTypeDefinition objectTypeDefinition2 = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition2.setName("Arg1");
    NamedType namedType2 = createNamedType(objectTypeDefinition2);
    listType2.setType(namedType2); //[Arg]

    assertThat(XtextTypeUtils.isCompatible(listType, listType2)).isFalse();
    assertThat(XtextTypeUtils.isCompatible(listType2, listType)).isTrue(); // OK, less permissive

  }

  @Test
  public void isCompatibleSameTypeWrappingNonNullTypesTest() {

    ListType listType = GraphQLFactoryDelegate.createListType();
    ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition.setName("Arg1");
    NamedType namedType = createNamedType(objectTypeDefinition);
    namedType.setNonNull(true);
    listType.setType(namedType);
    listType.setNonNull(true);//[Arg!]!

    ListType listType2 = GraphQLFactoryDelegate.createListType();
    ObjectTypeDefinition objectTypeDefinition2 = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition2.setName("Arg1");
    NamedType namedType2 = createNamedType(objectTypeDefinition2);
    namedType2.setNonNull(true);
    listType2.setType(namedType2);
    listType2.setNonNull(true); //[Arg!]!

    assertThat(XtextTypeUtils.isCompatible(listType, listType2)).isTrue();
    assertThat(XtextTypeUtils.isCompatible(listType2, listType)).isTrue();
  }

  @Test
  public void isCompatibleWrappedAndNonWrappedTypeTest() {

    ListType listType = GraphQLFactoryDelegate.createListType();
    ObjectTypeDefinition objectTypeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition.setName("Arg1");
    NamedType namedType = createNamedType(objectTypeDefinition);
    namedType.setNonNull(true);
    listType.setType(namedType);//[Arg1]

    ObjectTypeDefinition objectTypeDefinition2 = GraphQLFactoryDelegate.createObjectTypeDefinition();
    objectTypeDefinition2.setName("Arg1");
    NamedType namedType2 = createNamedType(objectTypeDefinition2);
    namedType2.setNonNull(true); // Arg1

    assertThat(XtextTypeUtils.isCompatible(listType, namedType2)).isFalse();
    assertThat(XtextTypeUtils.isCompatible(namedType2, listType)).isFalse();
  }


}
