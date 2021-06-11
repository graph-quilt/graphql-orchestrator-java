package com.intuit.graphql.orchestrator;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NestedBooksService implements ServiceProvider {

  private Map<String, Object> book;

  NestedBooksService() {
    this.book = createBook();
  }

  private Map<String, Object> createBook() {
    Map<String, Object> b1 = new HashMap<>();
    b1.put("id", "book-1");
    b1.put("name", "GraphQL Advanced Stitching");
    b1.put("pageCount", "2000");
    b1.put("author", ImmutableMap.of(
        "id","author-1",
        "lastName","AuthorOne"
    ));
    return b1;
  }

  @Override
  public String getNameSpace() {
    return "BOOKS";
  }

  @Override
  public Map<String, String> sdlFiles() {
    return TestHelper.getFileMapFromList(
        "nested/books-pets-person/schema-books.graphqls",
        "nested/books-pets-person/pet-author-link.graphqls"
    );
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {
    return CompletableFuture.completedFuture(
        ImmutableMap.of("data", ImmutableMap.of("person", ImmutableMap.of("book", this.book))));
  }
}
