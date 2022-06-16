package com.intuit.graphql.orchestrator.schema.transform;

import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.exceptions.InvalidRenameException;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextGraphBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class RenameTransformerTest {

  @Test
  public void testRenamedFieldsGetRenamed() {
    String schema = "directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE "
          + "schema { query: Query } "
          + "type Query { a: MyType1 @rename(to: \"renamedA\") } "
          + "type MyType1 { test: String }";

    XtextGraph xtextGraph = XtextGraphBuilder
        .build(TestServiceProvider.newBuilder().namespace("SVC1")
            .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    XtextGraph domainGraph = new RenameTransformer().transform(xtextGraph);
    XtextGraph domainGraphTypes = new AllTypesTransformer().transform(domainGraph);

    ObjectTypeDefinition query = xtextGraph.getOperationMap().get(Operation.QUERY);

    assertThat(domainGraphTypes.getTypes().containsKey("MyType1")).isTrue();
    assertThat(query.getFieldDefinition().get(0).getName()).isEqualTo("renamedA");
  }
  @Test
  public void testRenamedTypesGetRenamed() {
    String schema = "directive @rename(from: String to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE "
            + "schema { query: Query } "
            + "type Query { a: MyType1 } "
            + "type MyType1 @rename(to: \"RenamedType\") { test: String }";

    XtextGraph xtextGraph = XtextGraphBuilder
            .build(TestServiceProvider.newBuilder().namespace("SVC1")
                    .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    XtextGraph domainGraph = new RenameTransformer().transform(xtextGraph);
    XtextGraph domainGraphTypes = new AllTypesTransformer().transform(domainGraph);

    ObjectTypeDefinition query = xtextGraph.getOperationMap().get(Operation.QUERY);

    assertThat(domainGraphTypes.getTypes().containsKey("RenamedType")).isTrue();
  }

  @Test
  public void testRenamedTypeAndFieldGetRenamed() {
    String schema = "directive @rename(to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE "
            + "schema { query: Query } "
            + "type Query { a: MyType1 @rename(to: \"renamedA\") } "
            + "type MyType1 @rename(to: \"RenamedType\") { test: String }";

    XtextGraph xtextGraph = XtextGraphBuilder
            .build(TestServiceProvider.newBuilder().namespace("SVC1")
                    .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    XtextGraph domainGraph = new RenameTransformer().transform(xtextGraph);
    XtextGraph domainGraphTypes = new AllTypesTransformer().transform(domainGraph);

    ObjectTypeDefinition query = xtextGraph.getOperationMap().get(Operation.QUERY);

    assertThat(domainGraphTypes.getTypes().containsKey("RenamedType")).isTrue();
    assertThat(query.getFieldDefinition().get(0).getName()).isEqualTo("renamedA");
  }

  @Test
  public void testExceptionCaughtFromRenamingExtension() {
    String schema = "directive @rename(from: String to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE "
            + "schema { query: Query } "
            + "type Query { a: MyType1 } "
            + "type MyType1 { test: String } "
            + "extend type MyType1 @rename(to: \"RenamedType\") { "
            + "id: ID "
            + "}";

    XtextGraph xtextGraph = XtextGraphBuilder
            .build(TestServiceProvider.newBuilder().namespace("SVC1")
                    .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    try {
      new RenameTransformer().transform(xtextGraph);
      Assert.fail("Expecting exception to be thrown");
    } catch (Exception ex) {
      if(ex instanceof InvalidRenameException) {
        Assert.assertEquals("Invalid rename directive for MyType1: Type Extensions cannot be renamed MyType1", ex.getMessage());
      } else {
        Assert.fail("Caught exception but is not a Stitching Exception");
      }
    }
  }

  @Test
  public void testExceptionCaughtFromRenamingFederationExtension() {
    String schema = "directive @rename(from: String to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE "
            + "schema { query: Query } "
            + "type Query { a: MyType1 } "
            + "type MyType1 @extends @rename(to: \"RenamedType\") { test: String } ";

    XtextGraph xtextGraph = XtextGraphBuilder
            .build(TestServiceProvider.newBuilder().namespace("SVC1")
                    .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                    .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    try {
      new RenameTransformer().transform(xtextGraph);
      Assert.fail("Expecting exception to be thrown");
    } catch (Exception ex) {
      if(ex instanceof InvalidRenameException) {
        Assert.assertEquals("Invalid rename directive for MyType1: Type Extensions cannot be renamed MyType1", ex.getMessage());
      } else {
        Assert.fail("Caught exception but is not a Stitching Exception");
      }
    }
  }

  @Test
  public void testExceptionCaughtFromBlankRename() {
    String schema = "directive @rename(from: String to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE "
            + "schema { query: Query } "
            + "type Query { a: MyType1 } "
            + "type MyType1 @rename(to: \" \") { test: String } ";

    XtextGraph xtextGraph = XtextGraphBuilder
            .build(TestServiceProvider.newBuilder().namespace("SVC1")
                    .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                    .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    try {
      new RenameTransformer().transform(xtextGraph);
      Assert.fail("Expecting exception to be thrown");
    } catch (Exception ex) {
      if(ex instanceof InvalidRenameException) {
        Assert.assertEquals("Invalid rename directive for MyType1: to argument is empty", ex.getMessage());
      } else {
        Assert.fail("Caught exception but is not a Stitching Exception");
      }
    }
  }

  @Test
  public void testExceptionCaughtFromRenameWithWhitespace() {
    String schema = "directive @rename(from: String to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE "
            + "schema { query: Query } "
            + "type Query { a: MyType1 } "
            + "type MyType1 @rename(to: \"Test Rename\") { test: String } ";

    XtextGraph xtextGraph = XtextGraphBuilder
            .build(TestServiceProvider.newBuilder().namespace("SVC1")
                    .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                    .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    try {
      new RenameTransformer().transform(xtextGraph);
      Assert.fail("Expecting exception to be thrown");
    } catch (Exception ex) {
      if(ex instanceof InvalidRenameException) {
        Assert.assertEquals("Invalid rename directive for MyType1: to argument (Test Rename) cannot contain whitespace", ex.getMessage());
      } else {
        Assert.fail("Caught exception but is not a Stitching Exception");
      }
    }
  }

  @Test
  public void testExceptionCaughtFromRenameWithNonAlphanumericChars() {
    String schema = "directive @rename(from: String to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE "
            + "schema { query: Query } "
            + "type Query { a: MyType1 } "
            + "type MyType1 @rename(to: \"Test Rename!2ef24$\") { test: String } ";

    XtextGraph xtextGraph = XtextGraphBuilder
            .build(TestServiceProvider.newBuilder().namespace("SVC1")
                    .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                    .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    try {
      new RenameTransformer().transform(xtextGraph);
      Assert.fail("Expecting exception to be thrown");
    } catch (Exception ex) {
      if(ex instanceof InvalidRenameException) {
        Assert.assertEquals("Invalid rename directive for MyType1: to argument (Test Rename!2ef24$) cannot contain whitespace", ex.getMessage());
      } else {
        Assert.fail("Caught exception but is not a Stitching Exception");
      }
    }
  }
  @Test
  public void testExceptionCaughtFromMultiOfSameTypeRenames() {
    String schema = "directive @rename(from: String to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE "
            + "schema { query: Query } "
            + "type Query { a: MyType1 b: MyType2} "
            + "type MyType1 @rename(to: \"Multi\") { test: String } "
            + "type MyType2 @rename(to: \"Multi\") { test: String } ";

    XtextGraph xtextGraph = XtextGraphBuilder
            .build(TestServiceProvider.newBuilder().namespace("SVC1")
                    .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                    .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    try {
      new RenameTransformer().transform(xtextGraph);
      Assert.fail("Expecting exception to be thrown");
    } catch (Exception ex) {
      if(ex instanceof InvalidRenameException) {
        Assert.assertEquals("Invalid rename directive for MyType2: Multiple definitions are renamed with the same name", ex.getMessage());
      } else {
        Assert.fail("Caught exception but is not a Stitching Exception");
      }
    }
  }

  @Test
  public void testExceptionCaughtFromMultiOfSameFieldRenames() {
    String schema = "directive @rename(from: String to: String!) on FIELD_DEFINITION | OBJECT | INTERFACE "
            + "schema { query: Query } "
            + "type Query { a: MyType1 } "
            + "type MyType1 { test: String @rename(to: \"Multi\") test2: String @rename(to: \"Multi\") } ";

    XtextGraph xtextGraph = XtextGraphBuilder
            .build(TestServiceProvider.newBuilder().namespace("SVC1")
                    .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                    .sdlFiles(ImmutableMap.of("schema.graphqls", schema)).build());

    try {
      new RenameTransformer().transform(xtextGraph);
      Assert.fail("Expecting exception to be thrown");
    } catch (Exception ex) {
      if(ex instanceof InvalidRenameException) {
        Assert.assertEquals("Invalid rename directive for test2: Multiple definitions are renamed with the same name", ex.getMessage());
      } else {
        Assert.fail("Caught exception but is not a Stitching Exception");
      }
    }
  }
}
