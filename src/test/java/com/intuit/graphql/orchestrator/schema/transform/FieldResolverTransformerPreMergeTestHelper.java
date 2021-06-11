package com.intuit.graphql.orchestrator.schema.transform;

import static com.intuit.graphql.orchestrator.utils.XtextUtils.getAllTypes;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getOperationType;
import static com.intuit.graphql.orchestrator.xtext.XtextScalars.STANDARD_SCALARS;

import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.TestServiceProvider;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.orchestrator.xtext.XtextResourceSetBuilder;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.xtext.resource.XtextResourceSet;

/**
 * This class should mimic how xtext stitching behaves.  An example of this is object type extension
 * is merged into object type.
 */
public class FieldResolverTransformerPreMergeTestHelper {

  // ==============
  // TEST TYPES
  // ==============

  public static final String A_OBJECT_WITH_SERVICE_LINK_TO_B =
      "type AObjectType { "
          + "  b1 : BObjectType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} ";

  // use to test ArgumentDefinitionNotAllowed
  public static final String A_OBJECT_FIELD_WITH_RESOLVER_HAS_ARGUMENT =
      "type AObjectType { "
          + "  b1(id: String) : BObjectType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} ";

  // use to test NotAValidLocationForFieldResolverDirective
  public static final String A_INTERFACE_WITH_SERVICE_LINK_TO_B =
      "interface AObjectType { "
          + "  b1 : BObjectType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} ";

  public static final String SERVICE_LINK_FOR_B_OBJECT_TYPE =
      "type BObjectType { } "
          + "directive @resolver(field: String, arguments: [ResolverArgument!]) on FIELD_DEFINITION "
          + "type ResolverArgument { name : String! value : String! }\n";

  public static final String A_OBJECT_WITH_TWO_SERVICE_LINK_TO_B =
      "type AObjectType { "
          + "  b1 : BObjectType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "  b2 : BObjectType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} ";

  public static final String A_AND_C_OBJECT_WITH_TWO_SERVICE_LINK_TO_B =
      "type AObjectType { "
          + "  b1 : BObjectType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} \n"
          + "type CObjectType { "
          + "  b1 : BObjectType @resolver(field: \"b1\" arguments: [{name : \"id\", value: \"$af1\"}]) "
          + "} ";

  // ==============
  // TEST SCHEMAS
  // ==============

  public static final String SCHEMA_A_IS_OBJECT_TYPE =
      "      type Query { "
          + "  a : AObjectType "
          + "} "
          + A_OBJECT_WITH_SERVICE_LINK_TO_B
          + SERVICE_LINK_FOR_B_OBJECT_TYPE;

  public static final String SCHEMA_A_TYPE_IS_NOT_NULL =
      "      type Query { "
          + "  a : AObjectType! "
          + "} "
          + A_OBJECT_WITH_SERVICE_LINK_TO_B
          + SERVICE_LINK_FOR_B_OBJECT_TYPE;

  public static final String SCHEMA_A_TYPE_WRAPPED_IN_ARRAY =
      "      type Query { "
          + "  a : [AObjectType] "
          + "} "
          + A_OBJECT_WITH_SERVICE_LINK_TO_B
          + SERVICE_LINK_FOR_B_OBJECT_TYPE;

  public static final String SCHEMA_A_NOT_NULL_WRAPPED_IN_ARRAY =
      "      type Query { "
          + "  a : [AObjectType!] "
          + "} "
          + A_OBJECT_WITH_SERVICE_LINK_TO_B
          + SERVICE_LINK_FOR_B_OBJECT_TYPE;

  public static final String SCHEMA_A_IS_INTERFACE =
      "      type Query { "
          + "  a : AObjectType "
          + "} "
          + A_INTERFACE_WITH_SERVICE_LINK_TO_B
          + SERVICE_LINK_FOR_B_OBJECT_TYPE;

  public static final String SCHEMA_FIELD_WITH_RESOLVER_HAS_ARGUMENT =
      "      type Query { "
          + "  a : AObjectType "
          + "} "
          + A_OBJECT_FIELD_WITH_RESOLVER_HAS_ARGUMENT
          + SERVICE_LINK_FOR_B_OBJECT_TYPE;

  public static final String SCHEMA_A_WITH_TWO__FIELD_RESOLVERS =
      "      type Query { "
          + "  a : AObjectType "
          + "} "
          + A_OBJECT_WITH_TWO_SERVICE_LINK_TO_B
          + SERVICE_LINK_FOR_B_OBJECT_TYPE;

  public static final String SCHEMA_A_AND_C_WITH_WITH_FIELD_RESOLVER =
      "      type Query { "
          + "  a : AObjectType "
          + "  c : CObjectType "
          + "} "
          + A_AND_C_OBJECT_WITH_TWO_SERVICE_LINK_TO_B
          + SERVICE_LINK_FOR_B_OBJECT_TYPE;


  public static XtextGraph createTestXtextGraph(String schema) {
    XtextResourceSet set = XtextResourceSetBuilder.singletonSet("schema", schema);

    final Map<String, TypeDefinition> types =
        Stream.concat(getAllTypes(set), STANDARD_SCALARS.stream())
            .collect(Collectors.toMap(TypeDefinition::getName, Function.identity()));

    ServiceProvider serviceProvider = TestServiceProvider.newBuilder().namespace("TEST_SVC").build();

    return XtextGraph.newBuilder()
        .query(getOperationType(Operation.QUERY, set))
        .types(types)
        .xtextResourceSet(set)
        .serviceProvider(serviceProvider)
        .build();
  }
}
