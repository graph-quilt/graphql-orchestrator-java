package com.intuit.graphql.orchestrator.utils;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.XtextResource;

public class XtextSerializer {

  public static String serialize(EObject eObject) {
    return ((XtextResource) eObject.eResource()).getSerializer().serialize(eObject);
  }
}
