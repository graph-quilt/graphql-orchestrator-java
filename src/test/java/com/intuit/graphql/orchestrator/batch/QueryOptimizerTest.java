package com.intuit.graphql.orchestrator.batch;

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
        SelectionSet.Builder mergedBuilder = SelectionSet.newSelectionSet();
        mergedBuilder.selection(b);
        mergedBuilder.selection(c);
        Field expectedOutputA = new Field("a", mergedBuilder.build());
        expectedOutputBuilder.selection(expectedOutputA);
        SelectionSet output = queryOptimizer.getTransformedSelectionSet();
        SelectionSet expectedOutput = expectedOutputBuilder.build();
        assertThat(expectedOutput.getSelections().size()).isEqualTo(output.getSelections().size());
        assertThat(expectedOutput.getSelections().get(0).toString()).isEqualTo(output.getSelections().get(0).toString());
    }
}