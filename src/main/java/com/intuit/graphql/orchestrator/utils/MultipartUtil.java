package com.intuit.graphql.orchestrator.utils;

import com.intuit.graphql.orchestrator.deferDirective.MultipartQueryRequest;
import graphql.ExecutionInput;
import graphql.language.Argument;
import graphql.language.AstPrinter;
import graphql.language.BooleanValue;
import graphql.language.Definition;
import graphql.language.Directive;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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

    Map<String, FragmentDefinition> fragmentDefinitions = originalDoc.getDefinitionsOfType(FragmentDefinition.class)
            .stream()
            .collect(Collectors.toMap(FragmentDefinition::getName, Function.identity()));

    originalDoc.getDefinitionsOfType(OperationDefinition.class).forEach(operationDef -> {
      OperationDefinition mappedOpDef = operationDef;
      List<String> deferredPaths = new ArrayList<>();

      //adds all the paths that are needed to split for path to the list
      addMultipartChildPaths(mappedOpDef.getSelectionSet(), deferredPaths, "", new HashMap<>());

      if(!deferredPaths.isEmpty()) {
          constructDeferredOperationDefinitions(operationDef, deferredPaths)
          .stream()
          .map(multipartRequest -> {
            List<Definition> operationDefinitions = new ArrayList<>();
            List<FragmentDefinition> fragmentSpreadDefs = multipartRequest.getFragmentSpreadNames()
                    .stream()
                    .map(fragmentDefinitions::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            operationDefinitions.add(multipartRequest.getMultipartOperationDef());
            operationDefinitions.addAll(fragmentSpreadDefs);
            return operationDefinitions;
          })
          .map(multipartOperationDefs -> originalDoc.transform(builder -> builder.definitions(multipartOperationDefs)))
          .map(AstPrinter::printAst)
          .map(query -> originalEI.transform(builder -> builder.query(query)))
          .forEach(eiList::add);
      }
    });

    return eiList;
  }

  //fragment name maps to definition
  private static List<MultipartQueryRequest> constructDeferredOperationDefinitions(OperationDefinition operationDefinition, List<String> multipartPaths) {
    List<MultipartQueryRequest> multipartQueryRequests = new ArrayList<>();
    multipartPaths.forEach(deferPath -> {
      String[] deferFieldPath = deferPath.split("\\.");
      int lastSelectionInPathIndex = deferFieldPath.length -1;
      String multipartFieldName = deferFieldPath[lastSelectionInPathIndex];
      List<Selection> currentFieldPath = getQueryPathSelections(operationDefinition.getSelectionSet(), deferFieldPath);
      Set<String> fragmentsNeeded = new HashSet<>();

      Selection multipartChildFieldContext = null;

      //iterates through path and constructs field/selectionSet without defer directive and fields irrelevant fields to defer query
      for (int j = lastSelectionInPathIndex; j >= 0; j--) {
        Selection multipartFieldContext = currentFieldPath.get(j);
        if(multipartFieldContext instanceof FragmentSpread) {
          fragmentsNeeded.add(((FragmentSpread) multipartFieldContext).getName());
        }
        if(multipartFieldContext instanceof InlineFragment) {
          multipartFieldName = multipartFieldName.split("-")[0];
        }

        multipartChildFieldContext = updateMultipartSelectionSetField(multipartFieldContext, multipartChildFieldContext, multipartFieldName);
      }

      SelectionSet multipartSelectionSet = SelectionSet.newSelectionSet()
              .selection(multipartChildFieldContext)
              .build();

      OperationDefinition newMultipartOperationDefinition = operationDefinition.transform(
              builder -> builder.selectionSet(multipartSelectionSet)
      );

      multipartQueryRequests.add(
          MultipartQueryRequest.builder()
            .multipartOperationDef(newMultipartOperationDefinition)
            .fragmentSpreadNames(fragmentsNeeded)
            .build()
      );
    });

     return multipartQueryRequests;
  }

  private static List<Selection> getQueryPathSelections(SelectionSet originalSelectionSet, String[] fieldKeys) {
    List<Selection> currentFieldPath = new ArrayList<>();
    Selection currentField;

    for (int i = 0; i < fieldKeys.length; i++) {
      Selection lastField = ( i > 0) ? currentFieldPath.get(currentFieldPath.size() -1) : null;
      SelectionSet selectionSet = (i==0) ? originalSelectionSet : getLastFieldsSelectionSet(lastField);

      boolean isChildAnInlineFragment = fieldKeys[i].contains("-");
      currentField = getSelectionWithName(selectionSet, fieldKeys[i], isChildAnInlineFragment);
      currentFieldPath.add(currentField);
    }

    return currentFieldPath;
  }

  private static SelectionSet getLastFieldsSelectionSet(Selection selection) {
    if(selection instanceof Field) {
      return ((Field) selection).getSelectionSet();
    } else if(selection instanceof InlineFragment) {
      return ((InlineFragment) selection).getSelectionSet();
    } else {
      //throw exception because it should not reach
      return null;
    }
  }

  private static String getSelectionName(Selection selection) {
    if(selection instanceof Field) return ((Field) selection).getName();
    else if (selection instanceof FragmentSpread) return ((FragmentSpread) selection).getName();
    else if (selection instanceof InlineFragment) return ((InlineFragment) selection).getTypeCondition().getName();
    else {
      //throw error
      return null;
    }
  }

  private static Selection getSelectionWithName (SelectionSet selectionSet, String selectionName, boolean isInlineFragment) {
    String name = (!isInlineFragment) ? selectionName : selectionName.split("-")[0];
    int idx = (!isInlineFragment) ? 0 : Integer.parseInt(selectionName.split("-")[1]);

    return selectionSet.getSelections()
              .stream()
              .filter(selection -> getSelectionName(selection).equals(name))
              .collect(Collectors.toList()).get(idx);
  }

  private static Selection updateMultipartSelectionSetField(Selection parentField, Selection currentField, String deferName) {
    Selection newSelection = null;

    if(getSelectionName(parentField).equals(deferName)) {
      if(parentField instanceof Field) {
        List<Directive> prunedDirectives = ((Field)parentField).getDirectives()
                .stream()
                .filter(directive -> !directive.getName().equals(DEFER_DIRECTIVE_NAME))
                .collect(Collectors.toList());

        newSelection = ((Field)parentField).transform(builder -> builder.directives(prunedDirectives));
      } else if (parentField instanceof FragmentSpread) {
        List<Directive> prunedDirectives = ((FragmentSpread)parentField).getDirectives()
                .stream()
                .filter(directive -> !directive.getName().equals(DEFER_DIRECTIVE_NAME))
                .collect(Collectors.toList());

        newSelection = ((FragmentSpread) parentField).transform(builder -> builder.directives(prunedDirectives));
      } else if (parentField instanceof InlineFragment) {
        List<Directive> prunedDirectives = ((InlineFragment)parentField).getDirectives()
                .stream()
                .filter(directive -> !directive.getName().equals(DEFER_DIRECTIVE_NAME))
                .collect(Collectors.toList());

        newSelection = ((InlineFragment)parentField).transform(builder -> builder.directives(prunedDirectives));
      } else {
        return null;
      }
    } else {
        Field __typeName = Field.newField().name("__typename").build();

        if(parentField instanceof Field) {
          newSelection = ((Field)parentField).transform(builder -> builder.selectionSet(
                  SelectionSet.newSelectionSet().selection(currentField).selection(__typeName).build()
          ));
        } else if (parentField instanceof InlineFragment) {
          newSelection = ((InlineFragment)parentField).transform(builder -> builder.selectionSet(
                  SelectionSet.newSelectionSet().selection(currentField).selection(__typeName).build()
          ));
        } else {
          return null;
        }
    }

    return newSelection;
  }

  private static void addMultipartChildPaths(SelectionSet selectionSet, List<String> deferredPaths, String currentPath, HashMap<String, Integer> inlineFragmentIdxMap) {
    selectionSet.getSelections()
            .stream()
            .filter(InlineFragment.class::isInstance)
            .map(InlineFragment.class::cast)
            .forEach(childInlineFragment -> {
              String inlineFragmentName = childInlineFragment.getTypeCondition().getName();
              Integer currentIdx = inlineFragmentIdxMap.getOrDefault(inlineFragmentName, 0);
              processInlineFragment(childInlineFragment, deferredPaths, currentPath, currentIdx, inlineFragmentIdxMap);
              currentIdx++;
              inlineFragmentIdxMap.put(inlineFragmentName, currentIdx);
            });

    selectionSet.getSelections()
            .stream()
            .filter(FragmentSpread.class::isInstance)
            .map(FragmentSpread.class::cast)
            .forEach(childFragmentSpread -> processFragmentSpread(childFragmentSpread, deferredPaths, currentPath));

    selectionSet.getSelections()
            .stream()
            .filter(Field.class::isInstance)
            .map(Field.class::cast)
            .forEach(childField -> processField(childField, deferredPaths, currentPath, inlineFragmentIdxMap));
  }

  private static void processField(Field field,  List<String> deferredPaths, String currentPath, HashMap<String, Integer> inlineFragmentMap) {
    String currentChildName = field.getName();
    String childPath =  (StringUtils.isBlank(currentPath)) ? currentChildName : StringUtils.join( currentPath, ".", currentChildName);
    if(field.hasDirective(DEFER_DIRECTIVE_NAME)) {
      Argument ifArg = field.getDirectives(DEFER_DIRECTIVE_NAME).get(0).getArgument(DEFER_IF_ARG);
      if(ifArg == null || ((BooleanValue) ifArg.getValue()).isValue()) {
        deferredPaths.add(childPath);
      }
    }

    if(field.getSelectionSet() != null && CollectionUtils.isNotEmpty(field.getSelectionSet().getSelections())) {
      addMultipartChildPaths(field.getSelectionSet(), deferredPaths, childPath, inlineFragmentMap);
    }
  }

  private static void processInlineFragment(InlineFragment inlineFragment,  List<String> deferredPaths, String currentPath, int index, HashMap<String, Integer> inlineFragmentMap) {
    String currentChildName = StringUtils.join(inlineFragment.getTypeCondition().getName(), "-",  index);
    String childPath =  (StringUtils.isBlank(currentPath)) ? currentChildName : StringUtils.join( currentPath, ".", currentChildName);
    if(inlineFragment.hasDirective(DEFER_DIRECTIVE_NAME)) {
      Argument ifArg = inlineFragment.getDirectives(DEFER_DIRECTIVE_NAME).get(0).getArgument(DEFER_IF_ARG);
      if(ifArg == null || ((BooleanValue) ifArg.getValue()).isValue()) {
        deferredPaths.add(childPath);
      }
    }

    if(inlineFragment.getSelectionSet() != null && CollectionUtils.isNotEmpty(inlineFragment.getSelectionSet().getSelections())) {
      addMultipartChildPaths(inlineFragment.getSelectionSet(), deferredPaths, childPath, inlineFragmentMap);
    }
  }

  private static void processFragmentSpread(FragmentSpread fragmentSpread,  List<String> deferredPaths, String currentPath) {
    String currentChildName = fragmentSpread.getName();
    String childPath =  (StringUtils.isBlank(currentPath)) ? currentChildName : StringUtils.join( currentPath, ".", currentChildName);
    if(fragmentSpread.hasDirective(DEFER_DIRECTIVE_NAME)) {
      Argument ifArg = fragmentSpread.getDirectives(DEFER_DIRECTIVE_NAME).get(0).getArgument(DEFER_IF_ARG);
      if(ifArg == null || ((BooleanValue) ifArg.getValue()).isValue()) {
        deferredPaths.add(childPath);
      }
    }
  }
}
