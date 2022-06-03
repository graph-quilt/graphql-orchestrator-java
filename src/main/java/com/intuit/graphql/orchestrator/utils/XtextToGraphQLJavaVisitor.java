package com.intuit.graphql.orchestrator.utils;

import com.intuit.graphql.graphQL.Argument;
import com.intuit.graphql.graphQL.ArgumentsDefinition;
import com.intuit.graphql.graphQL.Directive;
import com.intuit.graphql.graphQL.DirectiveDefinition;
import com.intuit.graphql.graphQL.EnumTypeDefinition;
import com.intuit.graphql.graphQL.EnumValueDefinition;
import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.ImplementsInterfaces;
import com.intuit.graphql.graphQL.InputObjectTypeDefinition;
import com.intuit.graphql.graphQL.InputValueDefinition;
import com.intuit.graphql.graphQL.InterfaceTypeDefinition;
import com.intuit.graphql.graphQL.ListType;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectField;
import com.intuit.graphql.graphQL.ObjectFieldWithVariable;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.PrimitiveType;
import com.intuit.graphql.graphQL.ScalarTypeDefinition;
import com.intuit.graphql.graphQL.UnionTypeDefinition;
import com.intuit.graphql.graphQL.Value;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.graphQL.util.GraphQLSwitch;
import com.intuit.graphql.orchestrator.datafetcher.AliasablePropertyDataFetcher;
import com.intuit.graphql.orchestrator.schema.SchemaParseException;
import com.intuit.graphql.orchestrator.schema.transform.ExplicitTypeResolver;
import graphql.Scalars;
import graphql.introspection.Introspection.DirectiveLocation;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import graphql.schema.idl.EchoingWiringFactory;
import graphql.schema.idl.ScalarInfo;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.DEPRECATED_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.buildDeprecationReason;
import static com.intuit.graphql.orchestrator.utils.FederationConstants.FEDERATION_INACCESSIBLE_DIRECTIVE;
import static com.intuit.graphql.orchestrator.utils.XtextUtils.getDirectiveWithNameFromDefinition;
import static com.intuit.graphql.utils.XtextTypeUtils.typeName;
import static java.util.Objects.requireNonNull;

@Getter
public class XtextToGraphQLJavaVisitor extends GraphQLSwitch<GraphQLSchemaElement> {

  /**
   * A map of built-in scalar types as defined by the graphql specification
   */
  private static final Map<String, GraphQLScalarType> STANDARD_SCALAR_TYPES;
  private final Map<String, GraphQLType> graphQLObjectTypes;
  public final Map<String, GraphQLDirective> directiveDefinitions;

  public final HashSet<String> blacklistedTypes;

  private static final GraphQLScalarType FIELD_SET_SCALAR;

  static {
    STANDARD_SCALAR_TYPES = ScalarInfo.GRAPHQL_SPECIFICATION_SCALARS.stream()
        .collect(Collectors.toMap(GraphQLScalarType::getName, Function.identity()));

//    TODO REMOVE ONCE GRAPH-QL JAVA IS UPGRADED.....FIELD SET WILL ALREADY BE IN STANDARD SCALARS
    FIELD_SET_SCALAR = GraphQLScalarType.newScalar()
        .name("_FieldSet")
        .description("A selection set")
        .coercing(new Coercing<String, String>() {
          public String serialize(Object input) {
            return input.toString();
          }

          public String parseValue(Object input) {
            return this.serialize(input);
          }

          public String parseLiteral(Object input) {
            if (!(input instanceof StringValue)) {
              throw new CoercingParseLiteralException("Expected AST type '_FieldSet' or 'StringValue'");
            } else {
              return ((StringValue)input).getValue();
            }
          }
        })
        .build();

    STANDARD_SCALAR_TYPES.put("_FieldSet", FIELD_SET_SCALAR);
    STANDARD_SCALAR_TYPES.putAll(ExtendedScalarsSupport.GRAPHQL_EXTENDED_SCALARS.stream()
        .collect(Collectors.toMap(GraphQLScalarType::getName, Function.identity())));
  }

  private XtextToGraphQLJavaVisitor(Builder builder) {
    graphQLObjectTypes = builder.graphQLObjectTypes;
    directiveDefinitions = builder.directiveDefinitions;
    blacklistedTypes = new HashSet<>();
    blacklistedTypes.addAll(builder.blacklistedTypes);

    createBuiltInDirectives();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public GraphQLSchemaElement caseFieldDefinition(final FieldDefinition object) {
    GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition()
        .name(object.getName())
        .description(object.getDesc())
        .dataFetcher(new AliasablePropertyDataFetcher(object.getName()));

    if (Objects.nonNull(object.getNamedType())) {
      final GraphQLOutputType graphQLType = (GraphQLOutputType) doSwitch(object.getNamedType());
      builder.type(graphQLType);

      //If the outputting reference is now removed/is inaccessible, return null so it can be filtered out
      if(graphQLType == null) return null;
    }

    if (Objects.nonNull(object.getArgumentsDefinition())) {
      builder.arguments(createGraphqlArguments(object.getArgumentsDefinition()));
    }

    List<GraphQLDirective> directives = createGraphqlDirectives(object.getDirectives());
    directives.forEach(builder::withDirective);
    builder.deprecate(buildDeprecationReason(directives));

    return builder.build();
  }

  //Types

  private GraphQLSchemaElement nonNullGraphQl(final NamedType namedType, final GraphQLType type) {
    return namedType.isNonNull() ? GraphQLNonNull.nonNull(type) : type;
  }

  @Override
  public GraphQLSchemaElement casePrimitiveType(final PrimitiveType primitiveType) {
    GraphQLScalarType type = STANDARD_SCALAR_TYPES.getOrDefault(primitiveType.getType(), Scalars.GraphQLString);
    return nonNullGraphQl(primitiveType, type);
  }

  @Override
  public GraphQLSchemaElement caseObjectType(ObjectType object) {
    //Need to check blacklist due to reference for namedType being different.
    //When this changes, remove blacklist logic and switch to checking type definition for inaccessible directive
    if (Objects.nonNull(object.getType()) && !blacklistedTypes.contains(object.getType().getName())) {
      return nonNullGraphQl(object, (GraphQLType) doSwitch(object.getType()));
    }
    return null;
  }

  @Override
  public GraphQLSchemaElement caseListType(final ListType object) {
    if (Objects.nonNull(object.getType())) {
      return nonNullGraphQl(object, new GraphQLList((GraphQLType) doSwitch(object.getType())));
    }
    return null;
  }


  @Override
  public GraphQLSchemaElement caseObjectTypeDefinition(final ObjectTypeDefinition object) {
    final String me = object.getName();

    GraphQLType graphQLType = graphQLObjectTypes.get(me);
    if (Objects.nonNull(graphQLType)) {
      return graphQLType;
    }

    graphQLObjectTypes.put(me, new GraphQLTypeReference(me));

    GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(me);
    if (Objects.nonNull(object.getFieldDefinition())) {
      builder.fields(
          object
            .getFieldDefinition()
            .stream()
            .filter(fieldDefinition -> !getDirectiveWithNameFromDefinition(fieldDefinition, FEDERATION_INACCESSIBLE_DIRECTIVE).isPresent())
            .map(this::doSwitch)
            .filter(Objects::nonNull)
            .map(GraphQLFieldDefinition.class::cast)
            .collect(Collectors.toList())
      );

      if (Objects.nonNull(object.getImplementsInterfaces())) {
        builder.withInterfaces(
            createGraphQLTypeReferences(object.getImplementsInterfaces()));
      }
    }

    builder.description(object.getDesc());

    createGraphqlDirectives(object.getDirectives()).forEach(builder::withDirective);

    graphQLType = builder.build();
    graphQLObjectTypes.put(me, graphQLType);
    return graphQLType;
  }

  @Override
  public GraphQLSchemaElement caseInputObjectTypeDefinition(final InputObjectTypeDefinition object) {
    final String me = object.getName();

    GraphQLType graphQLType = graphQLObjectTypes.get(me);
    if (Objects.nonNull(graphQLType)) {
      return graphQLType;
    }
    graphQLObjectTypes.put(me, new GraphQLTypeReference(me));

    GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject().name(me);
    if (Objects.nonNull(object.getInputValueDefinition())) {
      builder.fields(
          object.getInputValueDefinition().stream()
              .map(this::doSwitch)
              .map(GraphQLInputObjectField.class::cast)
              .collect(Collectors.toList())
      );
    }

    builder.description(object.getDesc());

    createGraphqlDirectives(object.getDirectives()).forEach(builder::withDirective);

    graphQLType = builder.build();
    graphQLObjectTypes.put(me, graphQLType);
    return graphQLType;
  }

  @Override
  public GraphQLSchemaElement caseScalarTypeDefinition(final ScalarTypeDefinition object) {
    final String me = object.getName();
    // Newer versions of graphql-java have removed inbuilt scalar support, so providers
    // will need to define scalar in the sdl file. Since we are on old graphql-java,
    // we need to handle the conflict of scalars defined in sdl with the ones defined
    // in older graphql-java.
    GraphQLScalarType inBuiltScalar = STANDARD_SCALAR_TYPES.get(me);
    if(inBuiltScalar != null){
      return inBuiltScalar;
    }

    GraphQLType graphQLType = graphQLObjectTypes.get(me);
    if (Objects.nonNull(graphQLType)) {
      return graphQLType;
    }

    graphQLType = EchoingWiringFactory.fakeScalar(me).transform(builder -> {

      createGraphqlDirectives(object.getDirectives()).forEach(builder::withDirective);

      builder.description(object.getDesc());
    });

    graphQLObjectTypes.put(me, graphQLType);
    return graphQLType;
  }

  @Override
  public GraphQLSchemaElement caseEnumTypeDefinition(final EnumTypeDefinition object) {
    final String me = object.getName();

    GraphQLType graphQLType = graphQLObjectTypes.get(me);
    if (Objects.nonNull(graphQLType)) {
      return graphQLType;
    }
    graphQLObjectTypes.put(me, new GraphQLTypeReference(me));

    GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name(me);
    if (Objects.nonNull(object.getEnumValueDefinition())) {
      builder.values(
          object.getEnumValueDefinition().stream()
              .map(this::doSwitch)
              .map(GraphQLEnumValueDefinition.class::cast)
              .collect(Collectors.toList())
      );
    }

    createGraphqlDirectives(object.getDirectives()).forEach(builder::withDirective);

    builder.description(object.getDesc());

    graphQLType = builder.build();
    graphQLObjectTypes.put(me, graphQLType);
    return graphQLType;
  }

  @Override
  public GraphQLSchemaElement caseInterfaceTypeDefinition(final InterfaceTypeDefinition object) {
    final String me = object.getName();

    GraphQLType graphQLType = graphQLObjectTypes.get(me);
    if (Objects.nonNull(graphQLType)) {
      return graphQLType;
    }
    graphQLObjectTypes.put(me, new GraphQLTypeReference(me));

    GraphQLInterfaceType.Builder builder = GraphQLInterfaceType.newInterface().name(me).fields(
        object.getFieldDefinition().stream()
            .map(this::doSwitch)
            .map(GraphQLFieldDefinition.class::cast)
            .collect(Collectors.toList())
    );

    createGraphqlDirectives(object.getDirectives()).forEach(builder::withDirective);

    builder.typeResolver(new ExplicitTypeResolver());

    builder.description(object.getDesc());

    if (Objects.nonNull(object.getImplementsInterfaces())) {
      builder.withInterfaces(
          createGraphQLTypeReferences(object.getImplementsInterfaces()));
    }

    graphQLType = builder.build();
    graphQLObjectTypes.put(me, graphQLType);
    return graphQLType;

  }

  @Override
  public GraphQLSchemaElement caseUnionTypeDefinition(final UnionTypeDefinition object) {
    final String me = object.getName();
    GraphQLType graphQLType = graphQLObjectTypes.get(me);
    if (Objects.nonNull(graphQLType)) {
      return graphQLType;
    }

    graphQLObjectTypes.put(me, new GraphQLTypeReference(me));

    GraphQLUnionType.Builder builder = GraphQLUnionType.newUnionType()
        .name(me)
        .possibleTypes(
            object.getUnionMemberShip().getUnionMembers().getNamedUnion().stream()
                .map(namedType -> (GraphQLObjectType) doSwitch(namedType)).toArray(GraphQLObjectType[]::new)
        );

    builder.typeResolver(new ExplicitTypeResolver());
    builder.description(object.getDesc());

    createGraphqlDirectives(object.getDirectives()).forEach(builder::withDirective);

    graphQLType = builder.build();
    graphQLObjectTypes.put(me, graphQLType);
    return graphQLType;
  }

  @Override
  public GraphQLSchemaElement caseInputValueDefinition(final InputValueDefinition object) {

    GraphQLInputObjectField.Builder builder = GraphQLInputObjectField.newInputObjectField()
        .name(object.getName())
        .description(object.getDesc())
        .type((GraphQLInputType) doSwitch(object.getNamedType()));

    if (Objects.nonNull(object.getDefaultValue())) {
      builder.defaultValue(createValue(object.getDefaultValue()));
    }

    createGraphqlDirectives(object.getDirectives()).forEach(builder::withDirective);

    return builder.build();
  }

  @Override
  public GraphQLSchemaElement caseEnumValueDefinition(final EnumValueDefinition object) {

    GraphQLEnumValueDefinition.Builder builder = GraphQLEnumValueDefinition.newEnumValueDefinition()
        .name(object.getEnumValue())
        .value(object.getEnumValue())
        .description(object.getDesc());

    List<GraphQLDirective> directives = createGraphqlDirectives(object.getDirectives());
    directives.forEach(builder::withDirective);
    builder.deprecationReason(buildDeprecationReason(directives));

    return builder.build();
  }

  //Types

  //Arguments

  public List<GraphQLArgument> createGraphqlArguments(final ArgumentsDefinition object) {
    return object.getInputValueDefinition().stream()
        .map(this::createGraphqlArgument)
        .collect(Collectors.toList());
  }

  private GraphQLArgument createGraphqlArgument(final InputValueDefinition object) {
    GraphQLArgument.Builder builder = GraphQLArgument.newArgument().name(object.getName())
        .description(object.getDesc());

    if (!XtextTypeUtils.isValidInputType(object.getNamedType())) {
      String typeName = typeName(object.getNamedType());
      throw new SchemaParseException(String.format("Not a valid input type %s", typeName));
    }

    GraphQLInputType type = (GraphQLInputType) doSwitch(object.getNamedType());
    builder.type(type);

    createGraphqlDirectives(object.getDirectives()).forEach(builder::withDirective);

    if (Objects.nonNull(object.getDefaultValue())) {
      Object value = createValue(object.getDefaultValue());
      builder.defaultValue(value);
    }

    return builder.build();
  }

  //Arguments

  public Object createValue(Value object) {
    if (Objects.nonNull(object.getNullValue())) {
      return null;
    } else if (Objects.nonNull(object.getIntValue())) {
      return Integer.parseInt(object.getIntValue());
    } else if (Objects.nonNull(object.getFloatValue())) {
      return Float.parseFloat(object.getFloatValue());
    } else if (Objects.nonNull(object.getBoolValue())) {
      return Boolean.parseBoolean(object.getBoolValue());
    } else if (Objects.nonNull(object.getStringValue())) {
      return StringUtils.remove(object.getStringValue(), '"');
    } else if (Objects.nonNull(object.getEnumValue())) {
      return object.getEnumValue();
    } else if (Objects.nonNull(object.getObjectValue())) {
      return object.getObjectValue()
          .getObjectField()
          .stream()
          .collect(Collectors.toMap(ObjectField::getName,
              o -> createValue(o.getValue())
          ));
    } else if (Objects.nonNull(object.getArrayValue())) {
      return object.getArrayValue()
          .getValue()
          .stream()
          .map(this::createValue)
          .collect(Collectors.toList());
    }

    return null;
  }

  public Object createValueWithVariable(ValueWithVariable object) {
    if (Objects.nonNull(object.getNullValue())) {
      return null;
    } else if (Objects.nonNull(object.getIntValue())) {
      return Integer.parseInt(object.getIntValue());
    } else if (Objects.nonNull(object.getFloatValue())) {
      return Float.parseFloat(object.getFloatValue());
    } else if (Objects.nonNull(object.getBoolValue())) {
      return Boolean.parseBoolean(object.getBoolValue());
    } else if (Objects.nonNull(object.getStringValue())) {
      return StringUtils.remove(object.getStringValue(), '"');
    } else if (Objects.nonNull(object.getEnumValue())) {
      return object.getEnumValue();
    } else if (Objects.nonNull(object.getObjectValueWithVariable())) {
      return object.getObjectValueWithVariable()
          .getObjectFieldWithVariable()
          .stream()
          .collect(Collectors.toMap(ObjectFieldWithVariable::getName,
              o -> createValueWithVariable(o.getValueWithVariable())
          ));
    } else if (Objects.nonNull(object.getArrayValueWithVariable())) {
      return object.getArrayValueWithVariable()
          .getValueWithVariable()
          .stream()
          .map(this::createValueWithVariable)
          .collect(Collectors.toList());
    } else if (Objects.nonNull(object.getVariable())) {
      //This case should never be called
      throw new SchemaParseException("Variable not supported in schema definition");
    }
    return null;
  }

  //Directives

  private List<GraphQLDirective> createGraphqlDirectives(final List<Directive> directives) {
    if (CollectionUtils.isNotEmpty(directives)) {
      return directives.stream()
          .map(this::doSwitch)
          .map(GraphQLDirective.class::cast)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  @Override
  public GraphQLSchemaElement caseDirective(final Directive object) {

    GraphQLDirective definition = (GraphQLDirective) doSwitch(object.getDefinition());

    if (object.getArguments().isEmpty()) {
      return definition.transform(GraphQLDirective.Builder::clearArguments);
    }

    Map<String, GraphQLArgument> argDefMap = definition.getArguments().stream()
        .collect(Collectors.toMap(GraphQLArgument::getName, Function.identity()));

    Map<String, Object> argValueMap = new HashMap<>();
    object.getArguments().stream()
        .filter(t -> Objects.nonNull(t.getValueWithVariable()))
        .forEach(arg -> argValueMap.put(arg.getName(), createValueWithVariable(arg.getValueWithVariable())));

    return definition.transform(builder -> {
      builder.clearArguments(); //Clear existing arguments

      object.getArguments().stream()
          .map(Argument::getName)
          .forEach(arg -> {
            //find existing definition and value
            GraphQLArgument def = argDefMap.get(arg);
            Object value = argValueMap.get(arg);

            if (Objects.nonNull(def) && Objects.nonNull(value)) {
              builder.argument(def.transform(b -> b.value(value)));
            }
          });
    });
  }

  @Override
  public GraphQLSchemaElement caseDirectiveDefinition(DirectiveDefinition object) {
    final String me = object.getName();

    GraphQLDirective graphQLDirective = directiveDefinitions.get(me);
    if (Objects.nonNull(graphQLDirective)) {
      return graphQLDirective;
    }

    GraphQLDirective.Builder builder = GraphQLDirective.newDirective().name(object.getName());

    if (Objects.nonNull(object.getArgumentsDefinition()) && Objects.nonNull(
        object.getArgumentsDefinition().getInputValueDefinition())) {
      object.getArgumentsDefinition()
          .getInputValueDefinition()
          .forEach(inputValueDefinition -> {
            GraphQLArgument directiveArgument = createGraphqlArgument(inputValueDefinition);
            builder.argument(directiveArgument);
          });
    }

    object.getDirectiveLocations()
        .forEach(location -> builder.validLocation(DirectiveLocation.valueOf(location.getNamedDirective())));

    builder.repeatable(object.isRepeatable());
    if (Objects.nonNull(object.getDesc())) {
      builder.description(object.getDesc());
    }
    graphQLDirective = builder.build();
    directiveDefinitions.put(me, graphQLDirective);
    return graphQLDirective;
  }

  public void createBuiltInDirectives() {
    directiveDefinitions.put(DEPRECATED_DIRECTIVE.getName(), DEPRECATED_DIRECTIVE);
  }

  //Directives

  //Interfaces
  private GraphQLInterfaceType[] createGraphQLTypeReferences(final ImplementsInterfaces object) {
    return object.getNamedType().stream()
        .filter(Objects::nonNull)
        .map(this::doSwitch)
        .map(GraphQLInterfaceType.class::cast)
        .toArray(GraphQLInterfaceType[]::new);
  }
  //Interfaces


  public static final class Builder {

    private Map<String, GraphQLType> graphQLObjectTypes = new HashMap<>();
    private Map<String, GraphQLDirective> directiveDefinitions = new HashMap<>();

    private HashSet<String> blacklistedTypes = new HashSet<>();

    private Builder() {
    }

    public Builder graphqlObjectTypes(Map<String, GraphQLType> val) {
      graphQLObjectTypes.putAll(requireNonNull(val));
      return this;
    }

    public Builder graphqlBlackList(Set<String> blacklistedTypes) {
      this.blacklistedTypes.addAll(blacklistedTypes);
      return this;
    }

    public Builder directiveDefinitions(Map<String, GraphQLDirective> val) {
      directiveDefinitions.putAll(requireNonNull(val));
      return this;
    }

    public XtextToGraphQLJavaVisitor build() {
      return new XtextToGraphQLJavaVisitor(this);
    }
  }
}
