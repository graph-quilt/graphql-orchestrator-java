package com.intuit.graphql.orchestrator.utils;

import graphql.language.Field;
import org.apache.commons.collections4.Equator;
import org.apache.commons.lang3.StringUtils;

public class FieldEquator implements Equator<Field> {

  @Override
  public boolean equate(Field f1, Field f2) {
    return StringUtils.equals(f1.getName(), f2.getName());
  }

  @Override
  public int hash(Field field) {
    return field.getName().hashCode();
  }
}
