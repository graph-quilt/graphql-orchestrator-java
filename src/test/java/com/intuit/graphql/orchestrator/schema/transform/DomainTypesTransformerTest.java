package com.intuit.graphql.orchestrator.schema.transform;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder;
import com.intuit.graphql.utils.XtextTypeUtils;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class DomainTypesTransformerTest {

  @Test
  public void testDomainTypesGetRenamed() {
    /* TEST SCOPE: ObjectTypeDefinition, InterfaceTypeDefinition, UnionTypeDefinition, EnumTypeDefinition,
     * InputObjectTypeDefinition
     */

    String schema = "schema { query: Query } type Query { a: PageInfo m: MyType1 m2: MyInterface } "
        + "type PageInfo { id: String } "
        + "type MyType1 implements MyInterface { s: String } type MyType2 implements MyInterface { b: Boolean } "
        + "interface MyInterface { i : Int } union MyUnion = MyType1 | MyType2";

    Set<String> domainTypes = new HashSet<>();
    domainTypes.add("MyType1");
    domainTypes.add("MyInterface");
    domainTypes.add("MyUnion");

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC1").domainTypes(domainTypes)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    XtextGraph domainGraph = new DomainTypesTransformer().transform(xtextGraph);
    XtextGraph domainGraphTypes = new AllTypesTransformer().transform(domainGraph);

    ObjectTypeDefinition query = xtextGraph.getOperationMap().get(Operation.QUERY);

    assertThat(domainGraphTypes.getTypes().containsKey("SVC1_MyType1")).isTrue();
    assertThat(domainGraphTypes.getTypes().containsKey("SVC1_MyInterface")).isTrue();
    assertThat(domainGraphTypes.getTypes().containsKey("MyType2")).isTrue();

    assertThat(XtextTypeUtils.typeName(query.getFieldDefinition().get(1).getNamedType())).isEqualTo("SVC1_MyType1");
    assertThat(XtextTypeUtils.typeName(query.getFieldDefinition().get(2).getNamedType())).isEqualTo("SVC1_MyInterface");
  }
}
