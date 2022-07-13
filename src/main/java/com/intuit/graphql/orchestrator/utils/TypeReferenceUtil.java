package com.intuit.graphql.orchestrator.utils;

import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import java.util.List;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.util.EcoreUtil.UsageCrossReferencer;
import org.eclipse.xtext.EcoreUtil2;

public class TypeReferenceUtil {

  public static void updateTypeReferencesInAnXtextGraph(
      TypeDefinition targetType,
      TypeDefinition replacementType,
      XtextGraph xtextGraph) {
    List<Setting> crossReferences =
        (List<Setting>) UsageCrossReferencer.find(targetType, xtextGraph.getXtextResourceSet());
    updateAllCrossReferences(crossReferences, targetType, replacementType);
  }

  private static void updateAllCrossReferences(
      List<Setting> crossReferences, TypeDefinition targetType, TypeDefinition replacementType) {
    for (Setting setting : crossReferences) {
      EcoreUtil2.replace(setting, targetType, replacementType);
    }
  }

  public static void updateTypeReferencesInObjectType(
      TypeDefinition targetType,
      TypeDefinition replacementType,
      ObjectTypeDefinition objectTypeDefinition) {

    List<Setting> crossReferences =
        (List<Setting>) UsageCrossReferencer.find(targetType, objectTypeDefinition);
    updateAllCrossReferences(crossReferences, targetType, replacementType);
  }
}
