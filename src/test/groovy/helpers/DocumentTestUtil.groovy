package helpers

import com.intuit.graphql.orchestrator.testutils.SelectionSetUtil
import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition

class DocumentTestUtil {
    static Field getField(ArrayList<String> strings, Document document) {
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0)
        return SelectionSetUtil.getFieldByPath(strings, operationDefinition.getSelectionSet())
    }
}
