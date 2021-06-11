package com.intuit.graphql.orchestrator.datafetcher;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class AliasablePropertyDataFetcherTest {

  private static final MergedField aliasField = MergedField.newMergedField()
      .addField(Field.newField().name("original").alias("alias").build()).build();

  private Map<String, Object> data;

  private DataFetchingEnvironmentImpl.Builder dfeBuilder;

  @Before
  public void setUp() {
    this.data = new HashMap<>();
    dfeBuilder = DataFetchingEnvironmentImpl.newDataFetchingEnvironment();
  }

  @Test
  public void getsCorrectAliasValueFromMap() {

    data.put("alias", "data");
    final DataFetchingEnvironment dataFetchingEnvironment = dfeBuilder
        .source(data)
        .mergedField(aliasField)
        .build();

    final AliasablePropertyDataFetcher aliasablePropertyDataFetcher = new AliasablePropertyDataFetcher("original");

    assertThat(aliasablePropertyDataFetcher.get(dataFetchingEnvironment)).isEqualTo("data");
  }

  @Test
  public void getsOriginalValueWithField() {

    data.put("original", "data");
    final MergedField fieldWithoutAlias = MergedField.newMergedField()
        .addField(Field.newField().name("original").build()).build();

    final DataFetchingEnvironment dataFetchingEnvironment = dfeBuilder.source(data)
        .mergedField(fieldWithoutAlias).build();

    final AliasablePropertyDataFetcher aliasablePropertyDataFetcher = new AliasablePropertyDataFetcher("original");

    assertThat(aliasablePropertyDataFetcher.get(dataFetchingEnvironment)).isEqualTo("data");
  }

  @Test
  public void delegatesToDefaultDataFetcher() {

    final DataFetchingEnvironment dataFetchingEnvironment = dfeBuilder.source(new Original())
        .build();

    final AliasablePropertyDataFetcher aliasablePropertyDataFetcher = new AliasablePropertyDataFetcher("original");

    assertThat(aliasablePropertyDataFetcher.get(dataFetchingEnvironment)).isEqualTo("original");
  }

  private static class Original {

    public String getOriginal() {
      return "original";
    }
  }
}
