package com.intuit.graphql.orchestrator.schema.transform;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder;
import com.intuit.graphql.utils.XtextTypeUtils;
import java.util.Arrays;
import java.util.List;

public class FieldResolverTransformerPostMergeTestHelper {

  public static final String A_OBJECT_TYPE =
      "type AObjectType { "
          + "  af1 : String "
          + "} \n";

  public static final String EMPTY_A_OBJECT_TYPE =
      "type AObjectType { } \n";

  public static final String TYPE_RESOLVED_FIELD_STRING =
      "extend type AObjectType { "
          + "  a : String @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) @deprecated(reason: \"Use `newField`.\")"
          + "} \n";

  public static final String TYPE_RESOLVED_FIELD_OBJECT =
      "extend type AObjectType { "
          + "  a : BObjectType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} "
          + "type BObjectType \n";

  public static final String TYPE_RESOLVED_FIELD_OBJECT_NOTNULL =
      "extend type AObjectType { "
          + "  a : BObjectType! @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} "
          + "type BObjectType \n";

  public static final String TYPE_RESOLVED_FIELD_OBJECT_ARRAY =
      "extend type AObjectType { "
          + "  a : [BObjectType] @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} "
          + "type BObjectType \n";

  public static final String TYPE_RESOLVED_FIELD_OBJECT_NOTNULL_ARRAY =
      "extend type AObjectType { "
          + "  a : [BObjectType!] @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} "
          + "type BObjectType \n";

  public static final String TYPE_RESOLVED_FIELD_INTERFACE =
      "extend type AObjectType { "
          + "  a : BInterfaceType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} "
          + "interface BInterfaceType \n";

  public static final String TYPE_RESOLVED_FIELD_UNION =
      "extend type AObjectType { "
          + "  a : BUnionType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} "
          + "union BUnionType \n";

  public static final String TYPE_RESOLVED_FIELD_ENUM =
      "extend type AObjectType { "
          + "  a : BEnumType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} "
          + "enum BEnumType { } \n";

  public static final String RESOLVER_DIRECTIVE_DEFINITION =
          "directive @resolver(field: String, arguments: [ResolverArgument!]) on FIELD_DEFINITION "
          + "type ResolverArgument { name : String! value : String! }\n";

  public static final String QUERY_TYPE =
      "      type Query { "
          + "  a : AObjectType "
          + "} \n";

  public static final String SCHEMA_FIELD_RESOLVER_IS_STRING_TYPE = QUERY_TYPE
      + A_OBJECT_TYPE
      + TYPE_RESOLVED_FIELD_STRING
      + RESOLVER_DIRECTIVE_DEFINITION;

  public static final String SCHEMA_FIELD_RESOLVER_IS_OBJECT_TYPE = QUERY_TYPE
      + A_OBJECT_TYPE
      + TYPE_RESOLVED_FIELD_OBJECT
      + RESOLVER_DIRECTIVE_DEFINITION;

  public static final String SCHEMA_FIELD_RESOLVER_IS_OBJECT_TYPE_NOTNULL = QUERY_TYPE
      + A_OBJECT_TYPE
      + TYPE_RESOLVED_FIELD_OBJECT_NOTNULL
      + RESOLVER_DIRECTIVE_DEFINITION;

  public static final String SCHEMA_FIELD_RESOLVER_IS_OBJECT_ARRAY = QUERY_TYPE
      + A_OBJECT_TYPE
      + TYPE_RESOLVED_FIELD_OBJECT_ARRAY
      + RESOLVER_DIRECTIVE_DEFINITION;

  public static final String SCHEMA_FIELD_RESOLVER_IS_OBJECT_NOTNULL_ARRAY = QUERY_TYPE
      + A_OBJECT_TYPE
      + TYPE_RESOLVED_FIELD_OBJECT_NOTNULL_ARRAY
      + RESOLVER_DIRECTIVE_DEFINITION;

  public static final String SCHEMA_FIELD_RESOLVER_IS_INTERFACE = QUERY_TYPE
      + A_OBJECT_TYPE
      + TYPE_RESOLVED_FIELD_INTERFACE
      + RESOLVER_DIRECTIVE_DEFINITION;

  public static final String SCHEMA_FIELD_RESOLVER_IS_UNION = QUERY_TYPE
      + A_OBJECT_TYPE
      + TYPE_RESOLVED_FIELD_UNION
      + RESOLVER_DIRECTIVE_DEFINITION;

  public static final String SCHEMA_FIELD_RESOLVER_IS_ENUM = QUERY_TYPE
      + A_OBJECT_TYPE
      + TYPE_RESOLVED_FIELD_ENUM
      + RESOLVER_DIRECTIVE_DEFINITION;

  public static final String SCHEMA_FIELD_EMPTY_OBJECT_TYPE = QUERY_TYPE
      + EMPTY_A_OBJECT_TYPE
      + TYPE_RESOLVED_FIELD_ENUM
      + RESOLVER_DIRECTIVE_DEFINITION;

  public static TypeDefinition getTypeFromFieldDefinitions(String typeName,
      ObjectTypeDefinition parentType) {

    return parentType.getFieldDefinition().stream()
        .map(fieldDefinition -> XtextTypeUtils.unwrapAll(fieldDefinition.getNamedType()))
        .filter(namedType -> namedType instanceof ObjectType)
        .filter(namedType -> ((ObjectType) namedType).getType().getName().equals(typeName))
        .map(namedType -> ((ObjectType) namedType).getType())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Unexpected to get type from ObjectTypeDefinition"));
  }

  public static XtextGraph createTestXtextGraph(String schema) {
    ServiceProvider serviceProvider = TestServiceProvider.newBuilder()
      .namespace("TEST_SVC")
      .sdlFiles(ImmutableMap.of("main/graphql/schema.graphqls", schema))
      .build();

    XtextGraph xtextGraph = XtextGraphBuilder.build(serviceProvider);
    List<Transformer<XtextGraph, XtextGraph>> transformers = Arrays.asList(
        new TypeExtensionTransformer(),
        new DomainTypesTransformer(),
        new AllTypesTransformer(),
        new DirectivesTransformer(),
        new UnionAndInterfaceTransformer(),
        new FieldResolverTransformerPreMerge()
    );

    for (Transformer<XtextGraph, XtextGraph> transformer : transformers) {
      xtextGraph = transformer.transform(xtextGraph);
    }
  return xtextGraph;

  }
}
