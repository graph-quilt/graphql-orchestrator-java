package com.intuit.graphql.orchestrator.xtext;

import static com.intuit.graphql.orchestrator.TestHelper.toXtextResourceSet;
import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.SchemaDefinition;
import com.intuit.graphql.orchestrator.schema.Operation;
import java.util.Optional;
import org.junit.Test;

public class GraphQLResourceSetTest {

  private static GraphQLResourceSet SCHEMA;
  private static GraphQLResourceSet TYPE;
  private static GraphQLResourceSet SCHEMA_QUERY;
  private static GraphQLResourceSet TYPE_QUERY;

  static {
    SCHEMA = new GraphQLResourceSet(toXtextResourceSet("schema { query: Int }"));
    SCHEMA_QUERY = new GraphQLResourceSet(toXtextResourceSet("schema { query: foo } \n"
        + "type foo { bar: Int}"));
    TYPE_QUERY = new GraphQLResourceSet(toXtextResourceSet("type query { foo: FooType } \n"
        + "type FooType { bar: Int}"));
    TYPE = new GraphQLResourceSet(toXtextResourceSet("type foo { query: Int }"));

  }

  @Test
  public void getsSchemaDefinitionWhenPresent() {

    Optional<SchemaDefinition> sd = SCHEMA.findSchemaDefinition();
    assertThat(sd.isPresent()).isTrue();
    assertThat(sd.get().getOperationTypeDefinition().size()).isEqualTo(1);
    assertThat(sd.get().getOperationTypeDefinition().get(0).getOperationType().toString()).isEqualTo("query");
  }

  @Test
  public void doesNotGetSchemaDefinitionWhenNotPresent() {
    GraphQLResourceSet set = new GraphQLResourceSet(
        XtextResourceSetBuilder.newBuilder().file("foo", "type abc { foo: String }").build());
    Optional<SchemaDefinition> sd = set.findSchemaDefinition();
    assertThat(sd.isPresent()).isFalse();
  }

  @Test
  public void doesNotGetOperationFromSchemaDefinitionWhenNotPresent() {
    Optional<SchemaDefinition> sd = SCHEMA.findSchemaDefinition();
    assertThat(sd.isPresent()).isTrue();

    //Operation not of type object
    assertThat(SCHEMA.getOperationType(Operation.QUERY)).isNull();
    assertThat(SCHEMA.getOperationType(Operation.MUTATION)).isNull();
    assertThat(SCHEMA.getOperationType(Operation.SUBSCRIPTION)).isNull();

  }

  @Test
  public void getsOperationFromSchemaDefinitionWhenPresent() {
    Optional<SchemaDefinition> sd = SCHEMA_QUERY.findSchemaDefinition();
    assertThat(sd.isPresent()).isTrue();

    //Operation not of type object
    ObjectTypeDefinition operation = SCHEMA_QUERY.getOperationType(Operation.QUERY);
    assertThat(operation.getName()).isEqualTo("foo");

    assertThat(operation.getFieldDefinition().size()).isEqualTo(1);
    assertThat(operation.getFieldDefinition().get(0).getName()).isEqualTo("bar");

  }

  @Test
  public void doesNotGetOperationFromSetWhenNotPresent() {
    assertThat(TYPE.getOperationType(Operation.QUERY)).isNull();
    assertThat(TYPE_QUERY.getOperationType(Operation.MUTATION)).isNull();
    assertThat(SCHEMA_QUERY.getOperationType(Operation.SUBSCRIPTION)).isNull();
  }

  @Test
  public void getsOperationFromSetWhenPresent() {

    ObjectTypeDefinition operation = TYPE_QUERY.getOperationType(Operation.QUERY);

    assertThat(operation.getFieldDefinition().size()).isEqualTo(1);
    assertThat(operation.getFieldDefinition().get(0).getName()).isEqualTo("foo");

    ObjectTypeDefinition operation1 = SCHEMA_QUERY.getOperationType(Operation.QUERY);
    assertThat(operation1.getName()).isEqualToIgnoringCase("foo");

  }

  @Test
  public void getsObjectFromSetWhenPresent() {
    final ObjectTypeDefinition queryType = TYPE_QUERY.getObjectType(Operation.QUERY.getName());
    assertThat(queryType.getFieldDefinition().size()).isEqualTo(1);
    assertThat(queryType.getFieldDefinition().get(0).getName()).isEqualTo("foo");

    assertThat(SCHEMA_QUERY.getObjectType("foo")).isNotNull();

    assertThat(TYPE_QUERY.getObjectType("FooType")).isNotNull();
  }

  @Test
  public void doesntGetObjectFromSetWhenNotPresent() {
    assertThat(TYPE_QUERY.getObjectType(Operation.MUTATION.getName())).isNull();
    assertThat(SCHEMA_QUERY.getObjectType(Operation.SUBSCRIPTION.getName())).isNull();
    assertThat(TYPE_QUERY.getObjectType("BarType")).isNull();
  }

}