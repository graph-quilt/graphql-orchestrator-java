package com.intuit.graphql.orchestrator.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class FieldReferenceUtil {

  private FieldReferenceUtil() {}

  /**
   * extracts field references from the given string.  a field reference
   * started with the dollar($) sign and followed by a valid fieldName.
   *
   * @param inputString string to extract field references from
   * @return set of field references
   */
  public static Set<String> getAllFieldReferenceFromString(String inputString) {
    if (StringUtils.isEmpty(inputString)) {
      return Collections.emptySet();
    }

    Set<String> output = new HashSet<>();
    Pattern pattern = Pattern.compile("\\$\\w+"); // matches a-z A-Z _ 0-9
    Matcher matcher = pattern.matcher(inputString);
    while (matcher.find()) {
      String fieldRef = extractVariable(matcher.group());
      output.add(fieldRef);
    }
    return output;
  }

  private static String extractVariable(String string) {
    return StringUtils.removeStart(string, "$");
  }

}
