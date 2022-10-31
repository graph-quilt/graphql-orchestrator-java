package com.intuit.graphql.orchestrator.batch;

import static com.google.common.base.CharMatcher.anyOf;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueryOptimizerTest {

    private Field b;
    private Field c;
    private SelectionSet.Builder inputBuilder;

    @Before
    public void setup() {
        Field leaf = new Field("leaf");
        SelectionSet.Builder leafBuilder = SelectionSet.newSelectionSet();
        SelectionSet.Builder bBuilder = SelectionSet.newSelectionSet();
        SelectionSet.Builder cBuilder = SelectionSet.newSelectionSet();
        inputBuilder = SelectionSet.newSelectionSet();
        leafBuilder.selection(leaf);
        b = new Field("b",leafBuilder.build());
        c = new Field("c", leafBuilder.build());
        bBuilder.selection(b);
        cBuilder.selection(c);
        Field a1 = new Field("a", bBuilder.build());
        Field a2 = new Field("a", cBuilder.build());
        inputBuilder.selection(a1);
        inputBuilder.selection(a2);
    }

    @Test
    public void testQueryOptimiser(){
        QueryOptimizer queryOptimizer = new QueryOptimizer(OperationDefinition.Operation.QUERY,inputBuilder.build());
        SelectionSet.Builder expectedOutputBuilder = SelectionSet.newSelectionSet();
        SelectionSet.Builder expectedOutputBuilder2 = SelectionSet.newSelectionSet();
        SelectionSet.Builder mergedBuilder = SelectionSet.newSelectionSet();
        SelectionSet.Builder mergedBuilder2 = SelectionSet.newSelectionSet();
        mergedBuilder.selection(b);
        mergedBuilder.selection(c);
        mergedBuilder2.selection(c);
        mergedBuilder2.selection(b);
        Field expectedOutputA = new Field("a", mergedBuilder.build());
        Field expectedOutputA2 = new Field("a", mergedBuilder2.build());
        expectedOutputBuilder.selection(expectedOutputA);
        expectedOutputBuilder2.selection(expectedOutputA2);
        SelectionSet output = queryOptimizer.getTransformedSelectionSet();
        SelectionSet expectedOutput = expectedOutputBuilder.build();
        SelectionSet expectedOutput2 = expectedOutputBuilder2.build();
        assertThat(expectedOutput.getSelections().size()).isEqualTo(output.getSelections().size());
        assertThat(output.getSelections().get(0).toString()).isIn(expectedOutput.getSelections().get(0).toString(),
                expectedOutput2.getSelections().get(0).toString());
    }
}