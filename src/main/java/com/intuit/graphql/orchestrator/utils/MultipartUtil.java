package com.intuit.graphql.orchestrator.utils;

import graphql.ExecutionInput;
import graphql.language.AstPrinter;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEFER_DIRECTIVE_NAME;

/**
 * This class contains helper methods for GraphQL types. This class will often contain modifications of already built
 * methods in graphql-java's own GraphQlTypeUtil class.
 */
public class MultipartUtil {

  public static List<ExecutionInput> splitMultipartExecutionInput(ExecutionInput originalEI) {
    List<Document> documents = new ArrayList<>();
    List<Definition> initialQueryDefinitions = new ArrayList<>();
    Document originalDoc = GraphQLUtil.parser.parseDocument(originalEI.getQuery());

    originalDoc.getDefinitions().forEach(operationDef -> {
       List<OperationDefinition> splitDefs = splitOperationDefinition((OperationDefinition) operationDef);

      for (int i = 0; i < splitDefs.size(); i++) {
        //initial query should be sent first so all the initial operations should be merged in document
        if(i == 0) {
          initialQueryDefinitions.add(splitDefs.get(i));
        } else {
          List<Definition> multipartOperationDefs = Collections.singletonList(splitDefs.get(i));
          Document multipartQueryDocument = originalDoc.transform(builder ->
                  builder.definitions(multipartOperationDefs)
          );

          documents.add(multipartQueryDocument);
        }
      }
    });

    //construct initial query
    Document initDoc = originalDoc.transform(builder -> builder.definitions(initialQueryDefinitions));
    //append it to the beginning of list of documents
    documents.add(0, initDoc);

    return documents.stream()
      .map(AstPrinter::printAst)
      .map(query -> originalEI.transform(builder -> builder.query(query)))
      .collect(Collectors.toList());
  }

  private static List<OperationDefinition> splitOperationDefinition(OperationDefinition operationDefinition) {
    List<String> multipartPaths = new ArrayList<>();
    List<SelectionSet> multipartQueries = new ArrayList<>();
    SelectionSet originalSelectionSet = operationDefinition.getSelectionSet();

    //adds all the paths that are needed to split for path to the list
    addMultipartChildPaths(operationDefinition.getSelectionSet(), multipartPaths, "");

    AtomicReference<SelectionSet> initialSelectionSetRef = new AtomicReference<>(originalSelectionSet);
    multipartPaths.forEach(deferPath -> {
      String[] deferFieldPath = deferPath.split("\\.");
      int lastFieldInPathIndex = deferFieldPath.length -1;
      String deferParentName = deferFieldPath[lastFieldInPathIndex-1];
      String multipartFieldName = deferFieldPath[lastFieldInPathIndex];
      List<Field> currentFieldPath = getQueryPathFields(initialSelectionSetRef.get(), deferFieldPath);

      Field multipartChildFieldContext = null;
      Field childFieldContext = null;

      for (int j = lastFieldInPathIndex; j >= 0; j--) {
        Field fieldContext = currentFieldPath.get(j);
        Field multipartFieldContext = currentFieldPath.get(j);

        //skip updating deferred field since we are going to remove it
        if(j != lastFieldInPathIndex) {
          childFieldContext = updateParentsSelectionSet(fieldContext, childFieldContext, deferParentName, multipartFieldName);
        }

        multipartChildFieldContext = updateMultipartSelectionSet(multipartFieldContext, multipartChildFieldContext, multipartFieldName);
      }

      multipartQueries.add(SelectionSet.newSelectionSet().selection(multipartChildFieldContext).build());
      SelectionSet initialSelectionSet = SelectionSet.newSelectionSet().selection(childFieldContext).build();

      //need to check if last path before adding back in
      if(deferPath.equals(multipartPaths.get(multipartPaths.size() -1))) {
        multipartQueries.add(0, initialSelectionSet);
      } else {
        initialSelectionSetRef.set(initialSelectionSet);
      }
    });

    return multipartQueries
            .stream()
            .map(ss -> operationDefinition.transform(builder -> builder.selectionSet(ss)))
            .collect(Collectors.toList());
  }

  private static List<Field> getQueryPathFields(SelectionSet originalSelectionSet, String[] fieldKeys) {
    List<Field> currentFieldPath = new ArrayList<>();
    //todo handle duplicates fieldNames when dealing with nestedQueries
    Map<String, Field> fieldMap = new HashMap<>();

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

  private static Field updateParentsSelectionSet(Field parentField, Field updatedField, String defersParentName ,String deferFieldName ) {
    List<Field> newSelections;
    if(parentField.getName().equals(defersParentName)) {
      //keep every selection/field in selectionSet that does not pertain to deferred field
      newSelections = parentField.getSelectionSet()
              .getSelections()
              .stream()
              .map(Field.class::cast)
              .filter(field -> !field.getName().equals(deferFieldName))
              .collect(Collectors.toList());
    } else {
      //replace the parents selection set with the new selectionSet
      newSelections = parentField.getSelectionSet()
              .getSelections()
              .stream()
              .map(Field.class::cast)
              .map(selection -> (selection.getName().equals(updatedField.getName())) ? updatedField : selection)
              .collect(Collectors.toList());
    }

    return parentField.transform(builder -> builder.selectionSet(
        SelectionSet.newSelectionSet().selections(newSelections).build()
      )
    );
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
        newField = parentField.transform(builder -> builder.selectionSet(
              SelectionSet.newSelectionSet().selection(currentField).build()
      ));
    }

    return newField;
  }

  private static void addMultipartChildPaths(SelectionSet selectionSet, List<String> deferredPaths, String currentPath) {
    selectionSet.getSelections().stream().map(Field.class::cast).forEach(childSelection -> {
      String currentChildName = childSelection.getName();
      String childPath =  (StringUtils.isBlank(currentPath)) ? currentChildName : StringUtils.join( currentPath, ".", currentChildName);
      if(childSelection.hasDirective(DEFER_DIRECTIVE_NAME)) {
        deferredPaths.add(childPath);
      }

      if(childSelection.getSelectionSet() != null && CollectionUtils.isNotEmpty(childSelection.getSelectionSet().getSelections())) {
        addMultipartChildPaths(childSelection.getSelectionSet(), deferredPaths, childPath);
      }
    });
  }

}
