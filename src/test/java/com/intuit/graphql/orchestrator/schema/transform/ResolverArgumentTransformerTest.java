package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.utils.XtextUtils.getAllTypes;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getOperationType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.STANDARD_SCALARS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.MockitoAnnotations.initMocks;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDirectiveValidator;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext;
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

public class ResolverArgumentTransformerTest {

  private UnifiedXtextGraph source;

  private ResolverArgumentTransformer transformer;

  private ObjectTypeDefinition queryType;

  @Mock
  ResolverArgumentDirectiveValidator validator;

  @Before
  public void setUp() {
    initMocks(this);

    //assume all validations pass unless otherwise stated.
    doNothing().when(validator)
        .validateField(any(FieldDefinition.class), any(UnifiedXtextGraph.class), any(FieldContext.class));

    String schema = "type Query { field(arg: Int @resolver(field: \"a.b.c\")): Int } directive @resolver(field: String) on ARGUMENT_DEFINITION";

    XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", schema);

    queryType = getOperationType(Operation.QUERY, set);

    final Map<String, TypeDefinition> types = Stream.concat(getAllTypes(set), STANDARD_SCALARS.stream())
        .collect(Collectors.toMap(TypeDefinition::getName, Function.identity()));

    final DataFetcherContext originalDataFetcherContext = DataFetcherContext.newBuilder()
        .namespace("test_namespace")
        .build();

    source = UnifiedXtextGraph.newBuilder()
        .query(queryType)
        .types(types)
        .dataFetcherContext(new FieldContext("Query", "field"), originalDataFetcherContext)
        .build();

    transformer = new ResolverArgumentTransformer();
    transformer.validator = validator;
  }

  @Test
  public void resolverArgumentTransformsGraph() {
    final UnifiedXtextGraph transformedSource = transformer.transform(source);

    assertThat(transformedSource.getResolverArgumentFields()).hasSize(1);

    FieldDefinition resultFieldDefinition = queryType.getFieldDefinition().get(0);
    assertThat(resultFieldDefinition.getArgumentsDefinition().getInputValueDefinition()).isEmpty();
  }
}