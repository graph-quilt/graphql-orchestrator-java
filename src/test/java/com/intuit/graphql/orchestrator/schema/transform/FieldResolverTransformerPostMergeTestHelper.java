package com.intuit.graphql.orchestrator.schema.transform;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.schema.fold.UnifiedXtextGraphFolder;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder;
import com.intuit.graphql.utils.XtextTypeUtils;
import java.util.Arrays;
import java.util.List;

public class FieldResolverTransformerPostMergeTestHelper {

  public static final String RESOLVER_DIRECTIVE_DEFINITION =
          "directive @resolver(field: String, arguments: [ResolverArgument!]) on FIELD_DEFINITION "
          + "type ResolverArgument { name : String! value : String! }\n";

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

  public static UnifiedXtextGraph createTestUnifiedXtextGraph(String schema) {
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

    UnifiedXtextGraph unifiedXtextGraph = new UnifiedXtextGraphFolder().fold(
        UnifiedXtextGraph.emptyGraph(), Arrays.asList(xtextGraph));

    return unifiedXtextGraph;
  }
}
