package com.intuit.graphql.orchestrator.schema;

import com.intuit.graphql.orchestrator.stitching.StitchingException;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.xtext.validation.Issue;

/**
 * An exception thrown when an error occur during schema parsing.
 */
public class SchemaParseException extends StitchingException {

  private static final String FORMATTER = "Issues found while parsing the schema. \n %s \n These parsing issues were found in the following files. \n %s";

  public SchemaParseException(String msg, Throwable t) {
    super(msg, t);
  }

  public SchemaParseException(String msg) {
    super(msg);
  }

  public SchemaParseException(List<Issue> issues, Set<String> files) {
    super(String.format(FORMATTER,
        StringUtils.join(issues, System.lineSeparator()), StringUtils.join(issues, System.lineSeparator())));
  }
}
