package com.intuit.graphql.orchestrator.xtext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.TestHelper;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import org.assertj.core.util.Lists;

public class BooksServiceNoLinkField implements ServiceProvider {
  private Map<String, Object> books;
  private final BiConsumer<ExecutionInput, GraphQLContext> assertFn;
  private static final BiConsumer NOOP = (_x, _y) -> {
  };

  BooksServiceNoLinkField() {
    this.books = createBooks();
    this.assertFn = NOOP;
  }

  public BooksServiceNoLinkField(final BiConsumer<ExecutionInput, GraphQLContext> function) {
    this.books = createBooks();
    this.assertFn = function;
  }

  private Map<String, Object> createBooks() {
    Map<String, Object> b1 = new HashMap<>();
    b1.put("id", "book-1");
    b1.put("name", "GraphQL Advanced Stitching");
    b1.put("pageCount", "2000");
    b1.put("weight", 1.8);
    b1.put("isFamilyFriendly", true);
    b1.put("author", ImmutableMap.of(
        "id", "author-1",
        "lastName", "AuthorOne"
    ));

    Map<String, Object> b2 = new HashMap<>();
    b2.put("id", "book-2");
    b2.put("name", "The Recursion");
    b2.put("pageCount", "100");
    b2.put("weight", 1.4);
    b2.put("isFamilyFriendly", true);
    b2.put("author", ImmutableMap.of(
        "id", "author-2",
        "lastName", "AuthorTwo"
    ));

    Map<String, Object> b3 = new HashMap<>();
    b3.put("id", "book-3");
    b3.put("name", "Spring In Action");
    b3.put("pageCount", "223");
    b3.put("weight", 1.2);
    b3.put("isFamilyFriendly", false);

    Map<String, Object> author3 = new HashMap<>();
    author3.put("id", "author-3");
    author3.put("lastName", "AuthorThree");
    author3.put("petId", null);
    b3.put("author", author3);
    Map<String, Object> newMap = new HashMap<>();
    newMap.put("books", Lists.list(b1, b2, b3));
    return newMap;
  }

  @Override
  public String getNameSpace() {
    return "BOOKS";
  }

  @Override
  public Map<String, String> sdlFiles() {
    return TestHelper.getFileMapFromList("top_level/books-and-pets/schema-books.graphqls",
        "top_level/books-and-pets/pet-author-link.graphqls");
  }

  @Override
  public Set<String> domainTypes() {
    return ImmutableSet.of("Book", "Author", "NonExistingType");
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {
    this.assertFn.accept(executionInput, context);
    return CompletableFuture.completedFuture(ImmutableMap.of("data", this.books));
  }
}
