package com.intuit.graphql.orchestrator.utils;

import org.junit.Test;

import static com.intuit.graphql.orchestrator.utils.FederationUtils.getUniqueIdFromFieldSet;
import static org.assertj.core.api.Assertions.assertThat;

public class FederationUtilsTest {
    @Test
    public void canCreateUniqueIdFromFieldSetWithoutChildren() {
        String fieldSet = "{ foo bar c1}";
        String id = getUniqueIdFromFieldSet(fieldSet);
        assertThat(id).isEqualTo("barc1foo");
    }

    @Test
    public void canCreateUniqueIdFromFieldSetWithChildren() {
        String fieldSet = "{ foo bar c1 { d1 d2 d3 { e1 e2}}}";
        String id = getUniqueIdFromFieldSet(fieldSet);
        assertThat(id).isEqualTo("barc1food1d2d3e1e2");
    }

    @Test
    public void reorderedFieldSetResultInSameUniqueId() {
        String fieldSet1 = "{ foo bar c1 { d1 d2 d3 { e1 e2 } } }";
        String fieldSet2 = "{ bar foo c1 { d2 d1 d3 { e2 e1 } } }";
        String id = getUniqueIdFromFieldSet(fieldSet1);
        String id2 = getUniqueIdFromFieldSet(fieldSet2);
        assertThat(id).isEqualTo("barc1food1d2d3e1e2");
        assertThat(id2).isEqualTo(id);
    }
}
