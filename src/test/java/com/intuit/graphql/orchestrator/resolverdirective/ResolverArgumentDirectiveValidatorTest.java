package com.intuit.graphql.orchestrator.resolverdirective;

import static com.intuit.graphql.orchestrator.utils.XtextUtils.getAllTypes;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getObjectType;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getOperationType;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.STANDARD_SCALARS;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newIntType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newStringType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import com.intuit.graphql.orchestrator.xtext.UnifiedXtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ResolverArgumentDirectiveValidatorTest {

  @Mock
  public ResolverDirectiveTypeResolver resolver;

  private UnifiedXtextGraph source;

  private ResolverArgumentDirectiveValidator validator;

  private TypeDefinition enumType;
  private TypeDefinition typeA;

  @Before
  public void setUp() {
    initMocks(this);

    String schema = "schema { query: Query } "
        + "type Query { a: A } "
        + "type A { b: B } "
        + "type B { c: Int schema_enum: SchemaEnum } "
        + "input AInput { b: BInput } "
        + "input BInput { c: Int } "
        + "input AInputWrong { does_not_exist: Int } "
        + "enum SchemaEnum { a b c } "
        + "enum InputEnum { a b c } ";
    XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", schema);

    ObjectTypeDefinition query = getOperationType(Operation.QUERY, set);

    Map<String, TypeDefinition> types = Stream.concat(getAllTypes(set), STANDARD_SCALARS.stream())
        .collect(Collectors.toMap(TypeDefinition::getName, Function.identity()));

    source = UnifiedXtextGraph.newBuilder()
        .query(query)
        .types(types)
        .build();

    validator = new ResolverArgumentDirectiveValidator();
    validator.typeResolver = resolver;

    enumType = getType("SchemaEnum", set);
    typeA = getType("A", set);
  }

  @Test
  public void validationPassesOnScalars() {
    doReturn(newIntType()).when(resolver)
        .resolveField(eq("a.b.c"), any(UnifiedXtextGraph.class), anyString(), any(FieldContext.class));
    String resolverSchema = "type Query { other_a(arg: Int @resolver(field: \"a.b.c\")): Int } directive @resolver(field: String) on ARGUMENT_DEFINITION";

    final XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", resolverSchema);

    FieldDefinition fieldWithResolverDirective = getObjectType("Query", set).getFieldDefinition().get(0);

    validator.validateField(fieldWithResolverDirective, source, mock(FieldContext.class));
  }

  @Test
  public void validationPassesOnEnums() {
    doReturn(enumType).when(resolver)
        .resolveField(eq("a.b.schema_enum"), any(UnifiedXtextGraph.class), anyString(), any(FieldContext.class));

    String resolverSchema = "type Query { other_a(arg: SchemaEnum @resolver(field: \"a.b.schema_enum\")): Int } "
        + "enum SchemaEnum { a b c } "
        + "directive @resolver(field: String) on ARGUMENT_DEFINITION";

    final XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", resolverSchema);

    FieldDefinition fieldWithResolverDirective = getObjectType("Query", set).getFieldDefinition().get(0);

    validator.validateField(fieldWithResolverDirective, source, mock(FieldContext.class));
  }

  @Test
  public void validationPassesOnObjects() {
    doReturn(typeA).when(resolver)
        .resolveField(eq("a"), any(UnifiedXtextGraph.class), anyString(), any(FieldContext.class));

    String resolverSchema = "type Query { other_a(arg: AInput @resolver(field: \"a\")): Int } "
        + "input AInput { b: BInput } "
        + "input BInput { c: Int } "
        + "directive @resolver(field: String) on ARGUMENT_DEFINITION";

    final XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", resolverSchema);

    FieldDefinition fieldWithResolverDirective = getObjectType("Query", set).getFieldDefinition().get(0);

    validator.validateField(fieldWithResolverDirective, source, mock(FieldContext.class));
  }

  @Test(expected = ResolverArgumentFieldNotInSchema.class)
  public void fieldDoesNotExistInSchema() {
    doReturn(typeA).when(resolver)
        .resolveField(eq("a"), any(UnifiedXtextGraph.class), anyString(), any(FieldContext.class));

    String resolverSchema = "type Query { other_a(arg: AInputWrong @resolver(field: \"a\")): Int } "
        + "input AInputWrong { does_not_exist: Int } "
        + "directive @resolver(field: String) on ARGUMENT_DEFINITION";

    final XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", resolverSchema);

    FieldDefinition fieldWithResolverDirective = getObjectType("Query", set).getFieldDefinition().get(0);

    validator.validateField(fieldWithResolverDirective, source, mock(FieldContext.class));
  }

  @Test(expected = ResolverArgumentLeafTypeNotSame.class)
  public void notSameLeafName() {
    doReturn(newStringType()).when(resolver)
        .resolveField(eq("a.b.c"), any(UnifiedXtextGraph.class), anyString(), any(FieldContext.class));

    String resolverSchema = "type Query { other_a(arg: Int @resolver(field: \"a.b.c\")): Int } "
        + "directive @resolver(field: String) on ARGUMENT_DEFINITION";

    final XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", resolverSchema);

    FieldDefinition fieldWithResolverDirective = getObjectType("Query", set).getFieldDefinition().get(0);

    validator.validateField(fieldWithResolverDirective, source, mock(FieldContext.class));
  }

  @Test(expected = ResolverArgumentTypeMismatch.class)
  public void typeMismatch() {
    doReturn(newStringType()).when(resolver)
        .resolveField(eq("a"), any(UnifiedXtextGraph.class), anyString(), any(FieldContext.class));

    String resolverSchema = "type Query { other_a(arg: AInput @resolver(field: \"a\")): Int } "
        + "input AInput { b: Int } "
        + "directive @resolver(field: String) on ARGUMENT_DEFINITION";

    final XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", resolverSchema);

    FieldDefinition fieldWithResolverDirective = getObjectType("Query", set).getFieldDefinition().get(0);

    validator.validateField(fieldWithResolverDirective, source, mock(FieldContext.class));
  }
}