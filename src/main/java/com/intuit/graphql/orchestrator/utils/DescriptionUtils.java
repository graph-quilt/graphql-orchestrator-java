package com.intuit.graphql.orchestrator.utils;

import org.apache.commons.lang3.StringUtils;

public class DescriptionUtils {

  public static String attachNamespace(String namespace, String description) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(String.format("[%s]", StringUtils.trim(namespace)));
    if (StringUtils.isNotEmpty(description)) {
      stringBuilder.append(StringUtils.SPACE);
      stringBuilder.append(StringUtils.trim(description));
    }
    return stringBuilder.toString();
  }

  public static String mergeDescriptions(String firstDescription, String secondDescription) {
    String desc1 = StringUtils.substringAfter(firstDescription, "]");
    String desc2 = StringUtils.substringAfter(secondDescription, "]");
    String ns1 = StringUtils.substringBetween(firstDescription, "[", "]");
    String ns2 = StringUtils.substringBetween(secondDescription, "[", "]");
    return attachNamespace(join(ns1, ns2), join(desc1, desc2));
  }

  private static String join(String s1, String s2){
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(StringUtils.defaultString(s1));
    if(!StringUtils.isAnyBlank(s1,s2)){
      stringBuilder.append(',');
    }
    stringBuilder.append(StringUtils.defaultString(s2));
    return stringBuilder.toString();
  }

}
