package com.intuit.graphql.orchestrator.utils;

import graphql.parser.InvalidSyntaxException;
import org.junit.Test;

public class AstValueValidatorTest {

  @Test(expected = InvalidSyntaxException.class)
  public void validateCanThrowsInvalidSyntaxForObjectValueLiteral(){
    AstValueValidator.validateAstValue("{ num 100}");
  }

  @Test
  public void validateSuccessFOrObjectValueLiteral(){
    AstValueValidator.validateAstValue("{ num : 100}");
  }

  @Test
  public void validateSuccessForDoubleQoutedString(){
    AstValueValidator.validateAstValue("\"qouted string\"");
  }

  @Test
  public void validateSuccessForBooleanValue(){
    AstValueValidator.validateAstValue("{key : true}");
  }

}
