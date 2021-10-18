package com.intuit.graphql.orchestrator.utils;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.createNamedType;
import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.graphql.graphQL.ListType;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.impl.ListTypeImpl;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
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

}
