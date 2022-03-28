package com.intuit.graphql.orchestrator.utils;

import static com.intuit.graphql.orchestrator.utils.XtextUtils.definitionContainsDirective;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getDirectivesWithNameFromDefinition;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createDirectiveDefinition;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createValue;
import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createValueWithVariable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.DirectiveDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.ObjectTypeExtensionDefinition;
import com.intuit.graphql.graphQL.SchemaDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeExtensionDefinition;
import com.intuit.graphql.graphQL.Value;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.orchestrator.TestHelper;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate;
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.junit.Test;

public class XtextUtilsTest {

  private static XtextResourceSet SCHEMA;
  private static XtextResourceSet TYPE;
  private static XtextResourceSet SCHEMA_QUERY;
  private static XtextResourceSet TYPE_QUERY;

  static {
    SCHEMA = TestHelper.toXtextResourceSet("schema { query: Int }");
    SCHEMA_QUERY = TestHelper.toXtextResourceSet("schema { query: foo } \n"
        + "type foo { bar: Int}");
    TYPE_QUERY = TestHelper.toXtextResourceSet("type Query { foo: FooType } \n"
        + "type FooType { bar: Int}");
    TYPE = TestHelper.toXtextResourceSet("type foo { query: Int }");

  }


  @Test
  public void getsSchemaDefinitionWhenPresent() {

    Optional<SchemaDefinition> sd = XtextUtils.findSchemaDefinition(SCHEMA);
    assertThat(sd.isPresent()).isTrue();
    assertThat(sd.get().getOperationTypeDefinition().size()).isEqualTo(1);
    assertThat(sd.get().getOperationTypeDefinition().get(0).getOperationType().toString()).isEqualTo("query");
  }

  @Test
  public void doesNotGetSchemaDefinitionWhenNotPresent() {
    assertThatThrownBy(() -> XtextUtils.findSchemaDefinition(null)).isInstanceOf(NullPointerException.class);

    XtextResourceSet set = XtextResourceSetBuilder.newBuilder().file("foo", "type abc { foo: String }").build();
    Optional<SchemaDefinition> sd = XtextUtils.findSchemaDefinition(set);
    assertThat(sd.isPresent()).isFalse();
  }

  @Test
  public void doesNotgetOperationFromSchemaDefinitionWhenNotPresent() {
    assertThatThrownBy(() -> XtextUtils.findOperationType(Operation.QUERY, (SchemaDefinition) null))
        .isInstanceOf(NullPointerException.class);

    Optional<SchemaDefinition> sd = XtextUtils.findSchemaDefinition(SCHEMA);
    assertThat(sd.isPresent()).isTrue();

    //Operation not of type object
    Optional<ObjectTypeDefinition> operation = XtextUtils.findOperationType(Operation.QUERY, sd.get());
    assertThat(operation.isPresent()).isFalse();

    //Operation absent
    Optional<ObjectTypeDefinition> operation1 = XtextUtils.findOperationType(Operation.MUTATION, sd.get());
    assertThat(operation1.isPresent()).isFalse();

    Optional<ObjectTypeDefinition> operation2 = XtextUtils.findOperationType(Operation.SUBSCRIPTION, sd.get());
    assertThat(operation2.isPresent()).isFalse();

  }

  @Test
  public void getsOperationFromSchemaDefinitionWhenPresent() {
    Optional<SchemaDefinition> sd = XtextUtils.findSchemaDefinition(SCHEMA_QUERY);
    assertThat(sd.isPresent()).isTrue();

    //Operation not of type object
    Optional<ObjectTypeDefinition> operation = XtextUtils.findOperationType(Operation.QUERY, sd.get());
    assertThat(operation.isPresent()).isTrue();
    assertThat(operation.get().getName()).isEqualTo("foo");

    assertThat(operation.get().getFieldDefinition().size()).isEqualTo(1);
    assertThat(operation.get().getFieldDefinition().get(0).getName()).isEqualTo("bar");

  }

  @Test
  public void doesNotgetOperationFromSetWhenNotPresent() {
    assertThatThrownBy(() -> XtextUtils.findOperationType(Operation.QUERY, (XtextResourceSet) null))
        .isInstanceOf(NullPointerException.class);

    //Operation not of type object
    Optional<ObjectTypeDefinition> operation = XtextUtils.findOperationType(Operation.QUERY, TYPE);
    assertThat(operation.isPresent()).isFalse();

    //Operation absent
    Optional<ObjectTypeDefinition> operation1 = XtextUtils.findOperationType(Operation.MUTATION, TYPE_QUERY);
    assertThat(operation1.isPresent()).isFalse();

    Optional<ObjectTypeDefinition> operation2 = XtextUtils.findOperationType(Operation.SUBSCRIPTION, SCHEMA_QUERY);
    assertThat(operation2.isPresent()).isFalse();

  }

  @Test
  public void getsOperationFromSetWhenPresent() {

    Optional<ObjectTypeDefinition> operation = XtextUtils.findOperationType(Operation.QUERY, TYPE_QUERY);
    assertThat(operation.isPresent()).isTrue();
    assertThat(operation.get().getName()).isEqualToIgnoringCase(Operation.QUERY.getName());

    assertThat(operation.get().getFieldDefinition().size()).isEqualTo(1);
    assertThat(operation.get().getFieldDefinition().get(0).getName()).isEqualTo("foo");

    Optional<ObjectTypeDefinition> operation1 = XtextUtils.findOperationType(Operation.QUERY, SCHEMA_QUERY);
    assertThat(operation1.isPresent()).isTrue();
    assertThat(operation1.get().getName()).isEqualToIgnoringCase("foo");

  }

  @Test
  public void getsObjectFromSetWhenPresent() {

    Optional<ObjectTypeDefinition> operation = XtextUtils.findObjectType(Operation.QUERY.getName(), TYPE_QUERY);
    assertThat(operation.isPresent()).isTrue();
    assertThat(operation.get().getName()).isEqualToIgnoringCase(Operation.QUERY.getName());

    assertThat(operation.get().getFieldDefinition().size()).isEqualTo(1);
    assertThat(operation.get().getFieldDefinition().get(0).getName()).isEqualTo("foo");

    Optional<ObjectTypeDefinition> operation1 = XtextUtils.findObjectType("foo", SCHEMA_QUERY);
    assertThat(operation1.isPresent()).isTrue();
    assertThat(operation1.get().getName()).isEqualToIgnoringCase("foo");

    Optional<ObjectTypeDefinition> operation2 = XtextUtils.findObjectType("FooType", TYPE_QUERY);
    assertThat(operation2.isPresent()).isTrue();
    assertThat(operation2.get().getName()).isEqualToIgnoringCase("FooType");

  }

  @Test
  public void doesntGetObjectFromSetWhenNotPresent() {

    assertThatThrownBy(() -> XtextUtils.findObjectType(Operation.QUERY.getName(), (XtextResourceSet) null))
        .isInstanceOf(NullPointerException.class);

    Optional<ObjectTypeDefinition> operation = XtextUtils.findObjectType(Operation.MUTATION.getName(), TYPE_QUERY);
    assertThat(operation.isPresent()).isFalse();

    Optional<ObjectTypeDefinition> operation1 = XtextUtils
        .findObjectType(Operation.SUBSCRIPTION.getName(), SCHEMA_QUERY);
    assertThat(operation1.isPresent()).isFalse();

    Optional<ObjectTypeDefinition> operation2 = XtextUtils.findObjectType("BarType", TYPE_QUERY);
    assertThat(operation2.isPresent()).isFalse();

  }

  @Test
  public void parseStringFromValueWithVariable() {
    ValueWithVariable valueWithVariable = createValueWithVariable();
    valueWithVariable.setStringValue("\"test\"");

    ValueWithVariable noQuotes = createValueWithVariable();
    noQuotes.setStringValue("test");

    assertThat(XtextUtils.parseString(valueWithVariable)).isEqualTo("test");
    assertThat(XtextUtils.parseString(noQuotes)).isEqualTo("test");
  }

  @Test
  public void parseStringFromValue() {
    Value description = createValue();
    description.setStringValue("\"\"\"test_description\"\"\"");
    Value string = createValue();
    string.setStringValue("\"test_string\"");
    Value no_quotes = createValue();
    no_quotes.setStringValue("test_no_quotes");

    assertThat(XtextUtils.parseString(description)).isEqualTo("test_description");
    assertThat(XtextUtils.parseString(string)).isEqualTo("test_string");
    assertThat(XtextUtils.parseString(no_quotes)).isEqualTo("test_no_quotes");

  }

  @Test
  public void getDirectivesWithNameFromDefinitionsTypeDefReturnsEmptyWhenNotFound() {
    TypeDefinition typeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition();

    Directive fooDirective = GraphQLFactoryDelegate.createDirective();
    Directive barDirective = GraphQLFactoryDelegate.createDirective();

    DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition();
    DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition();

    fooDirectiveDefinition.setName("Foo");
    fooDirective.setDefinition(fooDirectiveDefinition);

    barDirectiveDefinition.setName("Bar");
    barDirective.setDefinition(barDirectiveDefinition);

    typeDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective));

    List<Directive> result = getDirectivesWithNameFromDefinition(typeDefinition, "Bad");

    assert CollectionUtils.isEmpty(result);
  }

  @Test
  public void getDirectivesWithNameFromDefinitionsTypedDefReturnsDirectives() {
    TypeDefinition typeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition();

    Directive fooDirective = GraphQLFactoryDelegate.createDirective();
    Directive barDirective = GraphQLFactoryDelegate.createDirective();
    Directive bar2Directive = GraphQLFactoryDelegate.createDirective();

    DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition();
    DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition();

    fooDirectiveDefinition.setName("Foo");
    fooDirective.setDefinition(fooDirectiveDefinition);

    barDirectiveDefinition.setName("Bar");
    barDirective.setDefinition(barDirectiveDefinition);
    bar2Directive.setDefinition(barDirectiveDefinition);

    typeDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive));

    List<Directive> result = getDirectivesWithNameFromDefinition(typeDefinition, "Bar");

    assert CollectionUtils.isNotEmpty(result);
    assert result.size() == 2;
    assert result.get(0).getDefinition().getName().equals("Bar");
  }

  @Test
  public void definitionContainsDirectiveTypeDefReturnsFalseWhenNotFound() {
    TypeDefinition typeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition();

    Directive fooDirective = GraphQLFactoryDelegate.createDirective();
    Directive barDirective = GraphQLFactoryDelegate.createDirective();

    DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition();
    DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition();

    fooDirectiveDefinition.setName("Foo");
    fooDirective.setDefinition(fooDirectiveDefinition);

    barDirectiveDefinition.setName("Bar");
    barDirective.setDefinition(barDirectiveDefinition);

    typeDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective));

    assert !definitionContainsDirective(typeDefinition, "Bad");
  }

  @Test
  public void definitionContainsDirectiveTypedDefReturnsTrueWhenExists() {
    TypeDefinition typeDefinition = GraphQLFactoryDelegate.createObjectTypeDefinition();

    Directive fooDirective = GraphQLFactoryDelegate.createDirective();
    Directive barDirective = GraphQLFactoryDelegate.createDirective();
    Directive bar2Directive = GraphQLFactoryDelegate.createDirective();

    DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition();
    DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition();

    fooDirectiveDefinition.setName("Foo");
    fooDirective.setDefinition(fooDirectiveDefinition);

    barDirectiveDefinition.setName("Bar");
    barDirective.setDefinition(barDirectiveDefinition);
    bar2Directive.setDefinition(barDirectiveDefinition);

    typeDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive));

    assert definitionContainsDirective(typeDefinition, "Foo");
  }

  @Test
  public void getDirectivesWithNameFromDefinitionsFieldDefReturnsEmptyWhenNotFound() {
    FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition();

    Directive fooDirective = GraphQLFactoryDelegate.createDirective();
    Directive barDirective = GraphQLFactoryDelegate.createDirective();

    DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition();
    DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition();

    fooDirectiveDefinition.setName("Foo");
    fooDirective.setDefinition(fooDirectiveDefinition);

    barDirectiveDefinition.setName("Bar");
    barDirective.setDefinition(barDirectiveDefinition);

    fieldDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective));

    List<Directive> result = getDirectivesWithNameFromDefinition(fieldDefinition, "Bad");

    assert CollectionUtils.isEmpty(result);
  }

  @Test
  public void getDirectivesWithNameFromDefinitionsFieldDefReturnsDirectives() {
    FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition();

    Directive fooDirective = GraphQLFactoryDelegate.createDirective();
    Directive barDirective = GraphQLFactoryDelegate.createDirective();
    Directive bar2Directive = GraphQLFactoryDelegate.createDirective();

    DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition();
    DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition();

    fooDirectiveDefinition.setName("Foo");
    fooDirective.setDefinition(fooDirectiveDefinition);

    barDirectiveDefinition.setName("Bar");
    barDirective.setDefinition(barDirectiveDefinition);
    bar2Directive.setDefinition(barDirectiveDefinition);

    fieldDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive));

    List<Directive> result = getDirectivesWithNameFromDefinition(fieldDefinition, "Bar");

    assert CollectionUtils.isNotEmpty(result);
    assert result.size() == 2;
    assert result.get(0).getDefinition().getName().equals("Bar");
  }

  @Test
  public void definitionContainsDirectivFieldDefReturnsFalseWhenNotFound() {
    FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition();

    Directive fooDirective = GraphQLFactoryDelegate.createDirective();
    Directive barDirective = GraphQLFactoryDelegate.createDirective();

    DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition();
    DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition();

    fooDirectiveDefinition.setName("Foo");
    fooDirective.setDefinition(fooDirectiveDefinition);

    barDirectiveDefinition.setName("Bar");
    barDirective.setDefinition(barDirectiveDefinition);

    fieldDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective));

    assert !definitionContainsDirective(fieldDefinition, "Bad");
  }

  @Test
  public void definitionContainsDirectiveFieldDefReturnsTrueWhenExists() {
    FieldDefinition fieldDefinition = GraphQLFactoryDelegate.createFieldDefinition();

    Directive fooDirective = GraphQLFactoryDelegate.createDirective();
    Directive barDirective = GraphQLFactoryDelegate.createDirective();
    Directive bar2Directive = GraphQLFactoryDelegate.createDirective();

    DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition();
    DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition();

    fooDirectiveDefinition.setName("Foo");
    fooDirective.setDefinition(fooDirectiveDefinition);

    barDirectiveDefinition.setName("Bar");
    barDirective.setDefinition(barDirectiveDefinition);
    bar2Directive.setDefinition(barDirectiveDefinition);

    fieldDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive));

    assert definitionContainsDirective(fieldDefinition, "Foo");
  }

  @Test
  public void getDirectivesWithNameFromDefinitionsTypeExtDefReturnsEmptyWhenNotFound() {
    ObjectTypeExtensionDefinition typeExtensionDefinition = GraphQLFactoryDelegate.createObjectTypeExtensionDefinition();

    Directive fooDirective = GraphQLFactoryDelegate.createDirective();
    Directive barDirective = GraphQLFactoryDelegate.createDirective();

    DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition();
    DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition();

    fooDirectiveDefinition.setName("Foo");
    fooDirective.setDefinition(fooDirectiveDefinition);

    barDirectiveDefinition.setName("Bar");
    barDirective.setDefinition(barDirectiveDefinition);

    typeExtensionDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective));

    List<Directive> result = getDirectivesWithNameFromDefinition(typeExtensionDefinition, "Bad");

    assert CollectionUtils.isEmpty(result);
  }

  @Test
  public void getDirectivesWithNameFromDefinitionsTypeExtDefReturnsDirectives() {
    ObjectTypeExtensionDefinition typeExtensionDefinition = GraphQLFactoryDelegate.createObjectTypeExtensionDefinition();

    Directive fooDirective = GraphQLFactoryDelegate.createDirective();
    Directive barDirective = GraphQLFactoryDelegate.createDirective();
    Directive bar2Directive = GraphQLFactoryDelegate.createDirective();

    DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition();
    DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition();

    fooDirectiveDefinition.setName("Foo");
    fooDirective.setDefinition(fooDirectiveDefinition);

    barDirectiveDefinition.setName("Bar");
    barDirective.setDefinition(barDirectiveDefinition);
    bar2Directive.setDefinition(barDirectiveDefinition);

    typeExtensionDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive));

    List<Directive> result = getDirectivesWithNameFromDefinition(typeExtensionDefinition, "Bar");

    assert CollectionUtils.isNotEmpty(result);
    assert result.size() == 2;
    assert result.get(0).getDefinition().getName().equals("Bar");
  }

  @Test
  public void definitionContainsDirectiveTypeExtDefReturnsFalseWhenNotFound() {
    TypeExtensionDefinition typeExtensionDefinition = GraphQLFactoryDelegate.createObjectTypeExtensionDefinition();

    Directive fooDirective = GraphQLFactoryDelegate.createDirective();
    Directive barDirective = GraphQLFactoryDelegate.createDirective();

    DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition();
    DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition();

    fooDirectiveDefinition.setName("Foo");
    fooDirective.setDefinition(fooDirectiveDefinition);

    barDirectiveDefinition.setName("Bar");
    barDirective.setDefinition(barDirectiveDefinition);

    typeExtensionDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective));

    assert !definitionContainsDirective(typeExtensionDefinition, "Bad");
  }

  @Test
  public void definitionContainsDirectiveTypeExtDefReturnsTrueWhenExists() {
    TypeExtensionDefinition typeExtensionDefinition = GraphQLFactoryDelegate.createObjectTypeExtensionDefinition();

    Directive fooDirective = GraphQLFactoryDelegate.createDirective();
    Directive barDirective = GraphQLFactoryDelegate.createDirective();
    Directive bar2Directive = GraphQLFactoryDelegate.createDirective();

    DirectiveDefinition fooDirectiveDefinition = createDirectiveDefinition();
    DirectiveDefinition barDirectiveDefinition = createDirectiveDefinition();

    fooDirectiveDefinition.setName("Foo");
    fooDirective.setDefinition(fooDirectiveDefinition);

    barDirectiveDefinition.setName("Bar");
    barDirective.setDefinition(barDirectiveDefinition);
    bar2Directive.setDefinition(barDirectiveDefinition);

    typeExtensionDefinition.getDirectives().addAll(Arrays.asList(fooDirective, barDirective, bar2Directive));

    assert definitionContainsDirective(typeExtensionDefinition, "Foo");
  }
}
