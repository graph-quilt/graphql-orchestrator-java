package com.intuit.graphql.orchestrator.schema.transform;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class RenameTransformerTest {

  @Test
  public void testDomainTypesGetRenamed() {
    /* TEST SCOPE: ObjectTypeDefinition, InterfaceTypeDefinition, UnionTypeDefinition, EnumTypeDefinition,
     * InputObjectTypeDefinition
     */

    String schema = "directive @rename(from: String to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE "
          + "schema { query: Query } "
          + "type Query { a: MyType1 @rename(to: \"renamedA\") } "
          + "type MyType1 { test: String }";

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC1")
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    XtextGraph domainGraph = new RenameTransformer().transform(xtextGraph);
    XtextGraph domainGraphTypes = new AllTypesTransformer().transform(domainGraph);

    ObjectTypeDefinition query = xtextGraph.getOperationMap().get(Operation.QUERY);

    assertThat(domainGraphTypes.getTypes().containsKey("MyType1")).isTrue();
    assertThat(query.getFieldDefinition().get(0).getName()).isEqualTo("renamedA");
  }
}
