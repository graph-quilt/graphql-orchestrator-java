package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildEnumTypeDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildFieldDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildInterfaceTypeDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildObjectTypeDefinition;
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildUnionTypeDefinition;
import static com.intuit.graphql.orchestrator.schema.transform.FieldResolverTransformerPostMergeTestHelper.RESOLVER_DIRECTIVE_DEFINITION;
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
import com.intuit.graphql.graphQL.PrimitiveType;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.UnionTypeDefinition;
import com.intuit.graphql.orchestrator.fieldresolver.FieldResolverException;
import com.intuit.graphql.orchestrator.resolverdirective.ExternalTypeNotfoundException;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentNotAFieldOfParentException;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.TypeMetadata;
import com.intuit.graphql.orchestrator.stitching.StitchingException;
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

  private final Transformer<XtextGraph, XtextGraph> transformer = new FieldResolverTransformerPostMerge();

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
    verify(textGraphSpy, never()).getType(any(NamedType.class));

    assertThat(transformedSource.getCodeRegistry().size()).isEqualTo(0);
  }

  @Test
  public void transformResolvedFieldIsScalarThenProcessingSucceedsAndNoTypeReplacementOccur() {
    // GIVEN
    String schema =  ""
            + "type Query { "
            + "  a : AObjectType "
            + "  b1(id: String): String "
            + "} \n"
            + "type AObjectType { "
            + "  af1 : String "
            + "} \n"
            + "extend type AObjectType { "
            + "  a : String @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) @deprecated(reason: \"Use `newField`.\")"
            + "} \n"
            + RESOLVER_DIRECTIVE_DEFINITION;

    XtextGraph xtextGraph = createTestXtextGraph(schema);
    XtextGraph textGraphSpy = Mockito.spy(xtextGraph);

    // WHEN
    XtextGraph transformedSource = transformer.transform(textGraphSpy);

    // THEN
    verify(textGraphSpy, never()).getType(any(NamedType.class));

    FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(fieldResolverContext.getTargetFieldContext().getFieldName()).isEqualTo("b1");
    assertThat(fieldResolverContext.getTargetFieldContext().getParentType()).isEqualTo("Query");

    ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();
    PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType();
    assertThat(targetFieldArgumentType.getType()).isEqualTo("String");

    assertThat(transformedSource.getCodeRegistry().size()).isEqualTo(1);

    TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType");
    assertThat(typeMetadata.getFieldResolverContext("a")).isNotNull();
  }

  @Test
  public void transformResolvedFieldIsObjectTypeThenProcessingSucceeds() {
    // GIVEN
    String schema =  ""
            + "type Query { "
            + "  a : AObjectType "
            + "  b1(id: String): BObjectType "
            + "} \n"
            + "type AObjectType { "
            + "  af1 : String "
            + "} \n"
            + "extend type AObjectType { "
            + "  a : BObjectType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
            + "} "
            + "type BObjectType \n"
            + RESOLVER_DIRECTIVE_DEFINITION;

    XtextGraph xtextGraph = createTestXtextGraph(schema);
    xtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalObjectTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalObjectTypeDefinition);

    FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(fieldResolverContext.getTargetFieldContext().getFieldName()).isEqualTo("b1");
    assertThat(fieldResolverContext.getTargetFieldContext().getParentType()).isEqualTo("Query");

    ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();
    PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType();
    assertThat(targetFieldArgumentType.getType()).isEqualTo("String");

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();

    TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType");
    assertThat(typeMetadata.getFieldResolverContext("a")).isNotNull();
  }

  @Test
  public void transformResolvedFieldObjectTypeWrappedNotNullThenProcessingSucceeds() {
    // GIVEN
    String schema =  ""
            + "type Query { "
            + "  a : AObjectType "
            + "  b1(id: String): BObjectType! "
            + "} \n"
            + "type AObjectType { "
            + "  af1 : String "
            + "} \n"
            + "extend type AObjectType { "
            + "  a : BObjectType! @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
            + "} "
            + "type BObjectType \n"
            + RESOLVER_DIRECTIVE_DEFINITION;

    XtextGraph xtextGraph = createTestXtextGraph(schema);
    xtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalObjectTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalObjectTypeDefinition);

    FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(fieldResolverContext.getTargetFieldContext().getFieldName()).isEqualTo("b1");
    assertThat(fieldResolverContext.getTargetFieldContext().getParentType()).isEqualTo("Query");

    ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();
    PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType();
    assertThat(targetFieldArgumentType.getType()).isEqualTo("String");

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();

    TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType");
    assertThat(typeMetadata.getFieldResolverContext("a")).isNotNull();
  }

  @Test
  public void transformResolvedFieldIsObjectTypeArrayThenProcessingSucceeds() {
    // GIVEN
    String schema =  ""
            + "type Query { "
            + "  a : AObjectType "
            + "  b1(id: String): [BObjectType] "
            + "} \n"
            + "type AObjectType { "
            + "  af1 : String "
            + "} \n"
            + "extend type AObjectType { "
            + "  a : [BObjectType] @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
            + "} "
            + "type BObjectType \n"
            + RESOLVER_DIRECTIVE_DEFINITION;
    XtextGraph xtextGraph = createTestXtextGraph(schema);
    xtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalObjectTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalObjectTypeDefinition);

    FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(fieldResolverContext.getTargetFieldContext().getFieldName()).isEqualTo("b1");
    assertThat(fieldResolverContext.getTargetFieldContext().getParentType()).isEqualTo("Query");

    ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();
    PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType();
    assertThat(targetFieldArgumentType.getType()).isEqualTo("String");

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();

    TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType");
    assertThat(typeMetadata.getFieldResolverContext("a")).isNotNull();
  }

  @Test
  public void transformResolvedFieldIsObjectTypeNOtNullArrayThenProcessingSucceeds() {
    // GIVEN
    String schema =  ""
            + "type Query { "
            + "  a : AObjectType "
            + "  b1(id: String): [BObjectType!] "
            + "} \n"
            + "type AObjectType { "
            + "  af1 : String "
            + "} \n"
            + "extend type AObjectType { "
            + "  a : [BObjectType!] @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
            + "} "
            + "type BObjectType \n"
            + RESOLVER_DIRECTIVE_DEFINITION;

    XtextGraph xtextGraph = createTestXtextGraph(schema);
    xtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalObjectTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalObjectTypeDefinition);

    FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(fieldResolverContext.getTargetFieldContext().getFieldName()).isEqualTo("b1");
    assertThat(fieldResolverContext.getTargetFieldContext().getParentType()).isEqualTo("Query");

    ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();
    PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType();
    assertThat(targetFieldArgumentType.getType()).isEqualTo("String");

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();

    TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType");
    assertThat(typeMetadata.getFieldResolverContext("a")).isNotNull();
  }

  @Test
  public void transformResolvedFieldIsInterfaceThenProcessingSucceeds() {
    // GIVEN
    String schema =  ""
            + "type Query { "
            + "  a : AObjectType "
            + "  b1(id: String): BInterfaceType "
            + "} \n"
            + "type AObjectType { "
            + "  af1 : String "
            + "} \n"
            + "extend type AObjectType { "
            + "  a : BInterfaceType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
            + "} "
            + "interface BInterfaceType \n"
            + RESOLVER_DIRECTIVE_DEFINITION;

    XtextGraph xtextGraph = createTestXtextGraph(schema);
    xtextGraph.getTypes().put(EXTERNAL_INTERFACE_TYPENAME, externalInterfaceTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_INTERFACE_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalInterfaceTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_INTERFACE_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalInterfaceTypeDefinition);

    FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(fieldResolverContext.getTargetFieldContext().getFieldName()).isEqualTo("b1");
    assertThat(fieldResolverContext.getTargetFieldContext().getParentType()).isEqualTo("Query");

    ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();
    PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType();
    assertThat(targetFieldArgumentType.getType()).isEqualTo("String");

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();

    TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType");
    assertThat(typeMetadata.getFieldResolverContext("a")).isNotNull();
  }

  @Test
  public void transformResolvedFieldIsUnionThenProcessingSucceeds() {
    // GIVEN
    String schema =  ""
            + "type Query { "
            + "  a : AObjectType "
            + "  b1(id: String): BUnionType "
            + "} \n"
            + "type AObjectType { "
            + "  af1 : String "
            + "} \n"
            + "extend type AObjectType { "
            + "  a : BUnionType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
            + "} "
            + "union BUnionType \n"
            + RESOLVER_DIRECTIVE_DEFINITION;

    XtextGraph xtextGraph = createTestXtextGraph(schema);
    xtextGraph.getTypes().put(EXTERNAL_UNION_TYPENAME, externalUnionTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_UNION_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalUnionTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_UNION_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalUnionTypeDefinition);

    FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(fieldResolverContext.getTargetFieldContext().getFieldName()).isEqualTo("b1");
    assertThat(fieldResolverContext.getTargetFieldContext().getParentType()).isEqualTo("Query");

    ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();
    PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType();
    assertThat(targetFieldArgumentType.getType()).isEqualTo("String");

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();

    TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType");
    assertThat(typeMetadata.getFieldResolverContext("a")).isNotNull();
  }

  @Test
  public void transformResolvedFieldIsEnumThenProcessingSucceeds() {
    // GIVEN
    String schema =  ""
            + "type Query { "
            + "  a : AObjectType "
            + "  b1(id: String): BEnumType "
            + "} \n"
            + "type AObjectType { "
            + "  af1 : String "
            + "} \n"
            + "extend type AObjectType { "
            + "  a : BEnumType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
            + "} "
            + "enum BEnumType { } \n"
            + RESOLVER_DIRECTIVE_DEFINITION;
    XtextGraph xtextGraph = createTestXtextGraph(schema);
    xtextGraph.getTypes().put(EXTERNAL_ENUM_TYPENAME, externalEnumTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_ENUM_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalEnumTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_ENUM_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalEnumTypeDefinition);

    FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(fieldResolverContext.getTargetFieldContext().getFieldName()).isEqualTo("b1");
    assertThat(fieldResolverContext.getTargetFieldContext().getParentType()).isEqualTo("Query");

    ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();
    PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType();
    assertThat(targetFieldArgumentType.getType()).isEqualTo("String");

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();

    TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType");
    assertThat(typeMetadata.getFieldResolverContext("a")).isNotNull();
  }

  @Test
  public void transformResolvedFieldExternalTypeNotFoundThrowsException() {
    // GIVEN
    String schema =  ""
            + "type Query { "
            + "  a : AObjectType "
            + "} \n"
            + "type AObjectType { "
            + "  af1 : String "
            + "} \n"
            + "extend type AObjectType { "
            + "  a : BObjectType! @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
            + "} "
            + "type BObjectType \n"
            + RESOLVER_DIRECTIVE_DEFINITION;

    XtextGraph xtextGraph = createTestXtextGraph(schema);

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

    String schema =  ""
            + "type Query { "
            + "  a : AObjectType "
            + "  b1(id: String): BEnumType "
            + "} \n"
            + "type AObjectType { } \n"
            + "extend type AObjectType { "
            + "  a : BEnumType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
            + "} "
            + "enum BEnumType { } \n"
            + RESOLVER_DIRECTIVE_DEFINITION;

    exceptionRule.expect(ResolverArgumentNotAFieldOfParentException.class);
    String expectedMessage = "af1' is not a field of parent type. serviceName=TEST_SVC, "
        + "parentTypeName=AObjectType, fieldName=a";
    exceptionRule.expectMessage(expectedMessage);

    XtextGraph xtextGraph = createTestXtextGraph(schema);
    xtextGraph.getTypes().put(EXTERNAL_ENUM_TYPENAME, externalEnumTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_ENUM_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalEnumTypeDefinition);

    // WHEN
    transformer.transform(xtextGraph);
  }

  @Test
  public void transformResolverArgumentHasInvalidArgumentValueThrowsException() {
    // GIVEN

    String schema =  ""
        + "type Query { "
        + "  a : AObjectType "
        + "  b1(id: String): BEnumType "
        + "} \n"
        + "type AObjectType { } \n"
        + "extend type AObjectType { "
        + "  a : BEnumType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"{invalid object}\"}]) "
        + "} "
        + "enum BEnumType { } \n"
        + RESOLVER_DIRECTIVE_DEFINITION;

    exceptionRule.expect(StitchingException.class);
    String expectedMessage = "Invalid resolver argument value: ResolverArgumentDefinition(name=id, value={invalid object})";
    exceptionRule.expectMessage(expectedMessage);

    XtextGraph xtextGraph = createTestXtextGraph(schema);
    xtextGraph.getTypes().put(EXTERNAL_ENUM_TYPENAME, externalEnumTypeDefinition);

    ObjectTypeDefinition extendedType = (ObjectTypeDefinition) xtextGraph.getType(
        EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_ENUM_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalEnumTypeDefinition);

    // WHEN
    transformer.transform(xtextGraph);
  }

  @Test
  public void transformFieldResolverHasDifferentTypeThanTargetThrowsException() {
    // GIVEN
    String schema =
        ""
            + "type Query { "
            + "  a : AObjectType "
            + "  b1(id: String): [BObjectType] "
            + "} \n"
            + "type AObjectType { "
            + "  af1 : String "
            + "} \n"
            + "extend type AObjectType { "
            + "  a : BObjectType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
            + "} "
            + "type BObjectType \n"
            + RESOLVER_DIRECTIVE_DEFINITION;

    XtextGraph xtextGraph = createTestXtextGraph(schema);
    xtextGraph.getTypes().put(EXTERNAL_OBJECT_TYPENAME, externalObjectTypeDefinition);

    ObjectTypeDefinition extendedType =
        (ObjectTypeDefinition) xtextGraph.getType(EXTENDED_OBJECT_TYPENAME);

    TypeDefinition placeHolderTypeDefinition =
        getTypeFromFieldDefinitions(EXTERNAL_OBJECT_TYPENAME, extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalObjectTypeDefinition);

    String expectedMessage = "The type of field with @resolver is not compatible with target field type.  "
        + "fieldName=a,  parentTypeName=AObjectType,  "
        + "resolverDirectiveDefinition=ResolverDirectiveDefinition(field=b1, arguments=[ResolverArgumentDefinition(name=id, value=$af1)])";
    exceptionRule.expect(FieldResolverException.class);
    exceptionRule.expectMessage(expectedMessage);

    // WHEN..THEN throws error
    transformer.transform(xtextGraph);
  }

  @Test
  public void transformFieldResolverAndTargetFieldHasSameArrayTypeProcessingSucceeds() {
    // GIVEN
    String schema =
        ""
            + "type Query { "
            + "  a : AObjectType "
            + "  b1(id: String): [BObjectType] "
            + "} \n"
            + "type AObjectType { "
            + "  af1 : String "
            + "} \n"
            + "extend type AObjectType { "
            + "  a : [BObjectType] @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
            + "} "
            + "type BObjectType \n"
            + RESOLVER_DIRECTIVE_DEFINITION;

    XtextGraph xtextGraph = createTestXtextGraph(schema);
    xtextGraph.getTypes().put("BObjectType", externalObjectTypeDefinition);

    ObjectTypeDefinition extendedType =
        (ObjectTypeDefinition) xtextGraph.getType("AObjectType");

    TypeDefinition placeHolderTypeDefinition =
        getTypeFromFieldDefinitions("BObjectType", extendedType);

    assertThat(placeHolderTypeDefinition).isNotSameAs(externalObjectTypeDefinition);

    // WHEN
    XtextGraph transformedSource = transformer.transform(xtextGraph);

    // THEN
    TypeDefinition replacementTypeDefinition = getTypeFromFieldDefinitions(
        EXTERNAL_OBJECT_TYPENAME, extendedType);
    assertThat(replacementTypeDefinition).isSameAs(externalObjectTypeDefinition);

    FieldResolverContext fieldResolverContext = transformedSource.getFieldResolverContexts().get(0);
    assertThat(fieldResolverContext.getTargetFieldContext().getFieldName()).isEqualTo("b1");
    assertThat(fieldResolverContext.getTargetFieldContext().getParentType()).isEqualTo("Query");

    ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();
    PrimitiveType targetFieldArgumentType = (PrimitiveType) resolverDirectiveDefinition.getArguments().get(0).getNamedType();
    assertThat(targetFieldArgumentType.getType()).isEqualTo("String");

    FieldContext expectedFieldContent = new FieldContext("AObjectType", "a");
    assertThat(transformedSource.getCodeRegistry().get(expectedFieldContent)).isNotNull();

    TypeMetadata typeMetadata = transformedSource.getTypeMetadatas().get("AObjectType");
    assertThat(typeMetadata.getFieldResolverContext("a")).isNotNull();
  }
}
