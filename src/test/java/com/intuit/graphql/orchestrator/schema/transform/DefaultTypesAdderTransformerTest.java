package com.intuit.graphql.orchestrator.schema.transform;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class DefaultTypesAdderTransformerTest {
  @Test
  public void testDomainTypesGetRenamed() {
    String schema = "schema { query: Query } type Query { a: A b: B } "
        + "type A { id: String } "
        + "type B { s: String } ";

    Set<String> domainTypes = new HashSet<>();
    domainTypes.add("A");
    domainTypes.add("B");

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("ABC").domainTypes(domainTypes)
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    XtextGraph defaultTypeAddedGraph = new DefaultTypesAdderTransformer().transform(xtextGraph);

    assertThat(defaultTypeAddedGraph.getTypes().size() == 11);
    assertThat(defaultTypeAddedGraph.getTypes().containsKey("Float"));
    assertThat(defaultTypeAddedGraph.getTypes().containsKey("Char"));
    assertThat(defaultTypeAddedGraph.getTypes().containsKey("Byte"));
    assertThat(defaultTypeAddedGraph.getTypes().containsKey("Long"));
    assertThat(defaultTypeAddedGraph.getTypes().containsKey("BigInteger"));
    assertThat(defaultTypeAddedGraph.getTypes().containsKey("ID"));
    assertThat(defaultTypeAddedGraph.getTypes().containsKey("String"));
    assertThat(defaultTypeAddedGraph.getTypes().containsKey("Boolean"));
    assertThat(defaultTypeAddedGraph.getTypes().containsKey("Short"));
    assertThat(defaultTypeAddedGraph.getTypes().containsKey("BigDecimal"));
    assertThat(defaultTypeAddedGraph.getTypes().containsKey("Int"));

    XtextGraph emptyGraph = new DefaultTypesRemoverTransformer().transform(defaultTypeAddedGraph);
    assertThat(emptyGraph.getTypes().isEmpty());
  }
}
