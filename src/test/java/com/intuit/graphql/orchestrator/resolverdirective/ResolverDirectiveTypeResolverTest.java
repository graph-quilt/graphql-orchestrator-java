package com.intuit.graphql.orchestrator.resolverdirective;

import static com.intuit.graphql.orchestrator.utils.XtextUtils.getAllTypes;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getOperationType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.STANDARD_SCALARS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.schema.transform.ResolverArgumentListTypeNotSupported;
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

public class ResolverDirectiveTypeResolverTest {

  private ResolverDirectiveTypeResolver resolver;

  public UnifiedXtextGraph source;

  @Before
  public void setUp() {
    initMocks(this);
    this.resolver = new ResolverDirectiveTypeResolver();

    final String schemaString = "schema { query: Query } type Query { a: A list: [A] premature_leaf: Int some_enum: Enum } type A { b: Int } enum Enum { A }";
    final XtextResourceSet schemaResource = XtextResourceSetBuilder.newBuilder()
        .file("schema", schemaString)
        .build();

    final ObjectTypeDefinition queryOperation = getOperationType(Operation.QUERY, schemaResource);

    final Map<String, TypeDefinition> types = Stream.concat(getAllTypes(schemaResource), STANDARD_SCALARS.stream())
        .collect(Collectors.toMap(TypeDefinition::getName, Function.identity()));

    source = UnifiedXtextGraph.newBuilder()
        .query(queryOperation)
        .types(types)
        .build();

  }

  @Test
  public void resolvesRootTypeOfField() {
    String field = "a.b";

    final TypeDefinition result = resolver.resolveField(field, source, "someArg", mock(FieldContext.class));

    assertThat(result.getName()).isEqualTo("Int");
  }

  @Test(expected = ResolverArgumentFieldRootObjectDoesNotExist.class)
  public void fieldDoesNotExist() {
    String field = "c.d";

    resolver.resolveField(field, source, "someArg", mock(FieldContext.class));
  }

  @Test(expected = ResolverArgumentListTypeNotSupported.class)
  public void listsNotSupported() {
    final String field = "list";

    resolver.resolveField(field, source, "someArg", mock(FieldContext.class));
  }

  @Test(expected = ResolverArgumentPrematureLeafType.class)
  public void prematureLeafTypeScalar() {
    final String field = "premature_leaf.nested";

    resolver.resolveField(field, source, "someArg", mock(FieldContext.class));
  }

  @Test(expected = ResolverArgumentPrematureLeafType.class)
  public void prematureLeafTypeEnum() {
    final String field = "some_enum.nested";

    resolver.resolveField(field, source, "someArg", mock(FieldContext.class));
  }
}