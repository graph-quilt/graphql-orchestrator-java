package com.intuit.graphql.orchestrator.utils;

import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;

public class AstValueValidator {

  private AstValueValidator() {}

  public static Parser parser = new Parser();

  public static void validateAstValue(String astLiteral) throws InvalidSyntaxException {
    String toParse = "input X { x : String = " + astLiteral + "}";
    parser.parseDocument(toParse);
  }

}
