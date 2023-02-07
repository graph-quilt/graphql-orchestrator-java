package com.intuit.graphql.orchestrator.utils;

import graphql.ExecutionInput;
import graphql.language.Argument;
import graphql.language.AstPrinter;
import graphql.language.BooleanValue;
import graphql.language.Definition;
import graphql.language.Directive;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_DIRECTIVE_NAME;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_IF_ARG;

/**
 * This class contains helper methods for GraphQL types. This class will often contain modifications of already built
 * methods in graphql-java's own GraphQlTypeUtil class.
 */
public class MultipartUtil {

  public static List<ExecutionInput> splitMultipartExecutionInput(ExecutionInput originalEI) {
    Document originalDoc = GraphQLUtil.parser.parseDocument(originalEI.getQuery());
    List<ExecutionInput> eiList = new ArrayList<>();

    originalDoc.getDefinitions().forEach(operationDef -> {
      OperationDefinition mappedOpDef =(OperationDefinition) operationDef;
      List<String> deferredPaths = new ArrayList<>();

      //adds all the paths that are needed to split for path to the list
      addMultipartChildPaths(mappedOpDef.getSelectionSet(), deferredPaths, "");

      if(!deferredPaths.isEmpty()) {
          constructDeferredOperationDefinitions((OperationDefinition) operationDef, deferredPaths)
          .stream()
          .map(Definition.class::cast)
          .map(Collections::singletonList)
          .map(multipartOperationDefs -> originalDoc.transform(builder ->
                  builder.definitions(multipartOperationDefs)
          ))
          .map(AstPrinter::printAst)
          .map(query -> originalEI.transform(builder -> builder.query(query)))
          .forEach(eiList::add);
      }
    });

    //add original ei back into list
    eiList.add(0, originalEI);
    return eiList;
  }

  private static List<OperationDefinition> constructDeferredOperationDefinitions(OperationDefinition operationDefinition, List<String> multipartPaths) {
    List<SelectionSet> multipartQueries = new ArrayList<>();

    AtomicReference<SelectionSet> initialSelectionSetRef = new AtomicReference<>(operationDefinition.getSelectionSet());
    multipartPaths.forEach(deferPath -> {
      String[] deferFieldPath = deferPath.split("\\.");
      int lastFieldInPathIndex = deferFieldPath.length -1;
      String multipartFieldName = deferFieldPath[lastFieldInPathIndex];
      List<Field> currentFieldPath = getQueryPathFields(initialSelectionSetRef.get(), deferFieldPath);

      Field multipartChildFieldContext = null;

      //iterates through path and constructs field/selectionSet without defer directive and fields irrelevant fields to defer query
      for (int j = lastFieldInPathIndex; j >= 0; j--) {
        Field multipartFieldContext = currentFieldPath.get(j);
        multipartChildFieldContext = updateMultipartSelectionSet(multipartFieldContext, multipartChildFieldContext, multipartFieldName);
      }

      multipartQueries.add(SelectionSet.newSelectionSet().selection(multipartChildFieldContext).build());
    });

    return multipartQueries
            .stream()
            .map(ss -> operationDefinition.transform(builder -> builder.selectionSet(ss)))
            .collect(Collectors.toList());
  }

  private static List<Field> getQueryPathFields(SelectionSet originalSelectionSet, String[] fieldKeys) {
    List<Field> currentFieldPath = new ArrayList<>();
    Field currentField;

    for (int i = 0; i < fieldKeys.length; i++) {

      if(i == 0) {
        currentField = (Field) getSelectionWithName(originalSelectionSet, fieldKeys[i]);
      } else {
        Field lastField = currentFieldPath.get(currentFieldPath.size() -1);
        currentField = (Field) getSelectionWithName(lastField.getSelectionSet(), fieldKeys[i]);
      }

      currentFieldPath.add(currentField);
    }

    return currentFieldPath;
  }

  private static Selection getSelectionWithName (SelectionSet selectionSet, String selectionName) {
    return selectionSet.getSelections()
              .stream()
              .map(Field.class::cast)
              .filter(field -> field.getName().equals(selectionName))
              .collect(Collectors.toList()).get(0);
  }

  private static Field updateMultipartSelectionSet(Field parentField, Field currentField, String deferName) {
    Field newField;

    if(parentField.getName().equals(deferName)) {
      List<Directive> prunedDirectives = parentField.getDirectives()
              .stream()
              .filter(directive -> !directive.getName().equals(DEFER_DIRECTIVE_NAME))
              .collect(Collectors.toList());

      newField = parentField.transform(builder -> builder.directives(prunedDirectives));
    } else {
        Field __typeName = Field.newField().name("__typename").build();

        newField = parentField.transform(builder -> builder.selectionSet(
              SelectionSet.newSelectionSet().selection(currentField).selection(__typeName).build()
      ));
    }

    return newField;
  }

  private static void addMultipartChildPaths(SelectionSet selectionSet, List<String> deferredPaths, String currentPath) {
    selectionSet.getSelections().stream().map(Field.class::cast).forEach(childSelection -> {
      String currentChildName = childSelection.getName();
      String childPath =  (StringUtils.isBlank(currentPath)) ? currentChildName : StringUtils.join( currentPath, ".", currentChildName);
      if(childSelection.hasDirective(DEFER_DIRECTIVE_NAME)) {
        Argument ifArg = childSelection.getDirectives(DEFER_DIRECTIVE_NAME).get(0).getArgument(DEFER_IF_ARG);
        if(ifArg == null || ((BooleanValue) ifArg.getValue()).isValue()) {
          deferredPaths.add(childPath);
        }
      }

      if(childSelection.getSelectionSet() != null && CollectionUtils.isNotEmpty(childSelection.getSelectionSet().getSelections())) {
        addMultipartChildPaths(childSelection.getSelectionSet(), deferredPaths, childPath);
      }
    });
  }
}
