package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildEnumTypeDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildInterfaceTypeDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildUnionTypeDefinition;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.SCHEMA_FIELD_EMPTY_OBJECT_TYPE;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.SCHEMA_FIELD_RESOLVER_IS_ENUM;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.SCHEMA_FIELD_RESOLVER_IS_INTERFACE;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.SCHEMA_FIELD_RESOLVER_IS_OBJECT_ARRAY;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.SCHEMA_FIELD_RESOLVER_IS_OBJECT_NOTNULL_ARRAY;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.SCHEMA_FIELD_RESOLVER_IS_OBJECT_TYPE;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.SCHEMA_FIELD_RESOLVER_IS_OBJECT_TYPE_NOTNULL;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.SCHEMA_FIELD_RESOLVER_IS_STRING_TYPE;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.SCHEMA_FIELD_RESOLVER_IS_UNION;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.createTestXtextGraph;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.getTypeFromFieldDefinitions;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.UnionTypeDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ExternalTypeNotfoundException;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentNotAFieldOfParentException;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class FieldResolverTransformerPostMergeTest {

  private static final String EXTENDED_OBJECT_TYPENAME = "AObjectType";
  private static final String EXTERNAL_OBJECT_TYPENAME = "BObjectType";
  private static final String EXTERNAL_INTERFACE_TYPENAME = "BInterfaceType";
  private static final String EXTERNAL_UNION_TYPENAME = "BUnionType";
  private static final String EXTERNAL_ENUM_TYPENAME = "BEnumType";

  private static ObjectTypeDefinition externalObjectTypeDefinition;
  private static InterfaceTypeDefinition externalInterfaceTypeDefinition;
  private static UnionTypeDefinition externalUnionTypeDefinition;
  private static EnumTypeDefinition externalEnumTypeDefinition;

  private Transformer<XtextGraph, XtextGraph> transformer = new FieldResolverTransformerPostMerge();

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @BeforeClass
  public static void setupOnce() {
    List<FieldDefinition> fieldDefinitions = singletonList(buildFieldDefinition("fieldA"));
    externalObjectTypeDefinition = buildObjectTypeDefinition(EXTERNAL_OBJECT_TYPENAME,
        fieldDefinitions);
    externalInterfaceTypeDefinition = buildInterfaceTypeDefinition(EXTERNAL_INTERFACE_TYPENAME,
        fieldDefinitions);

    ObjectType objectType = GraphQLFactoryDelegate.createObjectType();
    objectType.setType(externalObjectTypeDefinition);

    List<NamedType> unionMemberNamedTypes = singletonList(objectType);

    externalUnionTypeDefinition = buildUnionTypeDefinition(EXTERNAL_UNION_TYPENAME, unionMemberNamedTypes);
    externalEnumTypeDefinition = buildEnumTypeDefinition(EXTERNAL_ENUM_TYPENAME, "ENUM_VAL1");
  }

  @Test
  public void transformSchemaWithoutResolverDirectiveOnFieldDefinitionNoProcessingOccur() {
    // GIVEN
    String schema =
        "type Query { "
            + "   basicField(arg: Int) : String"
            + "   fieldWithArgumentResolver(arg: Int @resolver(field: \"a.b.c\")): Int "
            + "} "
            + "directive @resolver(field: String) on ARGUMENT_DEFINITION";
    XtextGraph xtextGraph = createTestXtextGraph(schema);
    assertThat(xtextGraph.hasFieldResolverDirective()).isFalse();

    XtextGraph textGraphSpy = Mockito.spy(xtextGraph);

    // WHEN
    XtextGraph transformedSource = transformer.transform(textGraphSpy);

    // THEN
    assertThat(transformedSource).isSameAs(textGraphSpy);
    verify(textGraphSpy, never()).getType(any(NamedType.class));

    assertThat(transformedSource.getCodeRegistry().size()).isEqualTo(0);
  }

  @Test
  public void transformResolvedFieldIsScalarThenProcessingSucceedsNoTypeReplacementOccur() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_FIELD_RESOLVER_IS_STRING_TYPE);
    XtextGraph textGraphSpy = Mockito.spy(xtextGraph);

    // WHEN
    XtextGraph transformedSource = transformer.transform(textGraphSpy);

    // THEN
    assertThat(transformedSource).isSameAs(textGraphSpy);
    verify(textGraphSpy, never()).getType(any(NamedType.class));

    assertThat(transformedSource.getCodeRegistry().size()).isEqualTo(1);
  }

  @Test
  public void transformResolvedFieldIsObjectTypeThenProcessingSucceeds() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_FIELD_RESOLVER_IS_OBJECT_TYPE);
    xtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalObjectTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource).isSameAs(xtextGraph);

    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalObjectTypeDefinition);

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();
  }

  @Test
  public void transformResolvedFieldIsObjectTypeNotNullThenProcessingSucceeds() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_FIELD_RESOLVER_IS_OBJECT_TYPE_NOTNULL);
    xtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalObjectTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource).isSameAs(xtextGraph);

    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalObjectTypeDefinition);

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();
  }

  @Test
  public void transformResolvedFieldIsObjectTypeArrayThenProcessingSucceeds() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_FIELD_RESOLVER_IS_OBJECT_ARRAY);
    xtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalObjectTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource).isSameAs(xtextGraph);

    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalObjectTypeDefinition);

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();
  }

  @Test
  public void transformResolvedFieldIsObjectTypeNOtNullArrayThenProcessingSucceeds() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_FIELD_RESOLVER_IS_OBJECT_NOTNULL_ARRAY);
    xtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalObjectTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource).isSameAs(xtextGraph);

    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalObjectTypeDefinition);

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();
  }

  @Test
  public void transformResolvedFieldIsInterfaceThenProcessingSucceeds() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_FIELD_RESOLVER_IS_INTERFACE);
    xtextGraph.getTypes().put(EXTERNAL_INTERFACE_TYPENAME, externalInterfaceTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_INTERFACE_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalInterfaceTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource).isSameAs(xtextGraph);

    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_INTERFACE_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalInterfaceTypeDefinition);

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();
  }

  @Test
  public void transformResolvedFieldIsUnionThenProcessingSucceeds() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_FIELD_RESOLVER_IS_UNION);
    xtextGraph.getTypes().put(EXTERNAL_UNION_TYPENAME, externalUnionTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_UNION_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalUnionTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource).isSameAs(xtextGraph);

    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_UNION_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalUnionTypeDefinition);

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();
  }

  @Test
  public void transformResolvedFieldIsEnumThenProcessingSucceeds() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_FIELD_RESOLVER_IS_ENUM);
    xtextGraph.getTypes().put(EXTERNAL_ENUM_TYPENAME, externalEnumTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_ENUM_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalEnumTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    assertThat(transformedSource).isSameAs(xtextGraph);

    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_ENUM_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalEnumTypeDefinition);

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();
  }

  @Test
  public void transformResolvedFieldExternalTypeNotFoundThrowsException() {
    // GIVEN
    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_FIELD_RESOLVER_IS_OBJECT_TYPE_NOTNULL);

    String expectedMessage = "External type not found.  serviceName=TEST_SVC, parentTypeName=AObjectType, "
        + "fieldName=a, placeHolderTypeDescription=[name:BObjectType, type:ObjectTypeDefinition, description:null";
    exceptionRule.expect(ExternalTypeNotfoundException.class);
    exceptionRule.expectMessage(expectedMessage);

    // WHEN..THEN
    transformer.transform(xtextGraph);
  }

  @Test
  public void transformResolverArgumentNotInParentTypeThrowsException() {
    // GIVEN

    exceptionRule.expect(ResolverArgumentNotAFieldOfParentException.class);
    String expectedMessage = "Resolver argument value $af1 should be a reference to a field in "
        + "Parent Type AObjectType";
    exceptionRule.expectMessage(expectedMessage);

    XtextGraph xtextGraph = createTestXtextGraph(SCHEMA_FIELD_EMPTY_OBJECT_TYPE);
    xtextGraph.getTypes().put(EXTERNAL_ENUM_TYPENAME, externalEnumTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_ENUM_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalEnumTypeDefinition);

    // WHEN
    transformer.transform(xtextGraph);
  }

}
