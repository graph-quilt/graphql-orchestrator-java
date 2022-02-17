package com.intuit.graphql.orchestrator.utils;

import com.intuit.graphql.graphQL.FieldDefinition;
import com.intuit.graphql.graphQL.NamedType;
import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.OperationTypeDefinition;
import com.intuit.graphql.graphQL.SchemaDefinition;
import com.intuit.graphql.graphQL.TypeDefinition;
import com.intuit.graphql.graphQL.TypeExtensionDefinition;
import com.intuit.graphql.graphQL.TypeSystem;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.graphQL.Value;
import com.intuit.graphql.graphQL.ValueWithVariable;
import com.intuit.graphql.orchestrator.federation.exceptions.InvalidFieldSetReferenceException;
import com.intuit.graphql.orchestrator.federation.keydirective.exceptions.EmptyFieldsArgumentKeyDirective;
import com.intuit.graphql.orchestrator.schema.Operation;
import com.intuit.graphql.orchestrator.xtext.XtextGraph;
import com.intuit.graphql.utils.XtextTypeUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.parser.Parser;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.XtextResourceSet;

import static com.intuit.graphql.orchestrator.utils.XtextTypeUtils.getFieldDefinitions;

/**
 * The Xtext utils to get information from the XText AST.
 */
public class XtextUtils {

  public static final String XTEXT_TYPE_FORMAT = "[name:%s, type:%s, description:%s]";

  //TODO Move federation constants to it's own class
  public static final String FEDERATION_KEY_DIRECTIVE = "key";
  public static final String FEDERATION_EXTERNAL_DIRECTIVE = "external";
  public static final String FEDERATION_EXTENDS_DIRECTIVE = "extends";
  public static final String FEDERATION_REQUIRES_DIRECTIVE = "requires";
  public static final String FEDERATION_PROVIDES_DIRECTIVE = "provides";

  private XtextUtils() {
  }

  /**
   * Find the schema definition from the {@link XtextResourceSet} as an Optional.
   *
   * @param set the set
   * @return the schema definition
   */
  public static Optional<SchemaDefinition> findSchemaDefinition(@NonNull final XtextResourceSet set) {
    return set.getResources().stream()
        .flatMap(resource -> resource.getContents().stream())
        .filter(TypeSystem.class::isInstance)
        .map(TypeSystem.class::cast)
        .flatMap(typeSystem -> typeSystem.getTypeSystemDefinition().stream())
        .map(TypeSystemDefinition::getSchema)
        .filter(Objects::nonNull)
        .findFirst();
  }

  /**
   * Find the operation ObjectTypeDefinition from the {@link SchemaDefinition} as an Optional.
   *
   * @param operation the operation
   * @param schemaDefinition the schema definition
   * @return the operation
   */
  public static Optional<ObjectTypeDefinition> findOperationType(Operation operation,
      @NonNull SchemaDefinition schemaDefinition) {
    return filteredStreamOf(ObjectTypeDefinition.class,
        filteredStreamOf(ObjectType.class,
            schemaDefinition.getOperationTypeDefinition().stream()
                .filter(opTD -> operation.getName().equalsIgnoreCase(opTD.getOperationType().toString()))
                .map(OperationTypeDefinition::getNamedType)
        ).map(ObjectType::getType))
        .findFirst();
  }

  /**
   * Finds the operation ObjectTypeDefinition from the xtext resource set. First checks if the operation inside the
   * {@link SchemaDefinition}, if not gets the operation from the list of {@link ObjectTypeDefinition}
   *
   * @param operation the operation
   * @param set the set
   * @return the operation
   */
  public static Optional<ObjectTypeDefinition> findOperationType(Operation operation, @NonNull XtextResourceSet set) {
    return findSchemaDefinition(set)
        .map(definition -> findOperationType(operation, definition))
        .orElseGet(() -> findObjectTypeStrict(operation.getName(), set));
  }

  private static Optional<ObjectTypeDefinition> findObjectTypeStrict(String name, @NonNull XtextResourceSet set) {
    /* strict variation of findObjectType that does not break namespace use cases.
    todo find a more suitable solution for findObject that considers strict types e.g. Query, Mutation, Subscription
    vs non strict types (types with namespaces)
     */
    return getAllContentsOfType(ObjectTypeDefinition.class, set)
        .filter(objectTypeDefinition -> objectTypeDefinition.getName().equals(name))
        .findFirst();
  }

  /**
   * A variant on {@link #findOperationType(Operation, XtextResourceSet)} that returns null if the operation
   * ObjectTypeDefinition does not exist in the set.
   *
   * @param operation The operation to find in the set
   * @param set The set that potentially contains the operation
   * @return The ObjectTypeDefinition of the operation in the set, or else null if it does not exist
   */
  public static ObjectTypeDefinition getOperationType(Operation operation, @NonNull XtextResourceSet set) {
    return findOperationType(operation, set).orElse(null);
  }

  /**
   * Finds the object type optional with the given name from the provided XtextResourceSet.
   *
   * @param name the name of the object type
   * @param set the set
   * @return the optional
   */
  public static Optional<ObjectTypeDefinition> findObjectType(String name, @NonNull XtextResourceSet set) {
    return getAllContentsOfType(ObjectTypeDefinition.class, set)
        .filter(objectTypeDefinition -> StringUtils.endsWith(objectTypeDefinition.getName(), name))
        .findFirst();
  }

  /**
   * A variant on {@link #findObjectType(String, XtextResourceSet)} that returns null if the object type does not exist
   * in the set.
   *
   * @param name the name of the object type
   * @param set the resource set that may or may not contain the type name
   * @return the ObjectTypeDefinition that matches the name or else null
   */
  public static ObjectTypeDefinition getObjectType(String name, @NonNull XtextResourceSet set) {
    return findObjectType(name, set).orElse(null);
  }

  /**
   * Finds any type definition with the given name from the provided resource set as an Optional.
   *
   * @param name the name of the type
   * @param set a resource set that may contain the type with the name
   * @return a TypeDefinition that matches the name as an Optional
   */
  public static Optional<TypeDefinition> findType(String name, @NonNull XtextResourceSet set) {
    return getAllContentsOfType(TypeDefinition.class, set)
        .filter(typeDefinition -> StringUtils.endsWith(typeDefinition.getName(), name))
        .findFirst();
  }

  /**
   * A variant on {@link #findType(String, XtextResourceSet)} that returns null if the type with the specified name does
   * not exist in the set.
   *
   * @param name the name of the object type
   * @param set the resource set that may contain the type name
   * @return the TypeDefinition that matches the name or else null
   */
  public static TypeDefinition getType(String name, @NonNull XtextResourceSet set) {
    return findType(name, set).orElse(null);
  }

  /**
   * Get all TypeExtensionDefinition for the given type name and class from the provided resourceset.
   *
   * @param name type name to search for
   * @param type  xtext type definition class
   * @param set xtext resource set
   * @param <T> generic type that is sub class of {@link TypeExtensionDefinition} to return
   * @return stream of TypeExtensionDefinitions for the given resource set
   */
  public static <T extends TypeExtensionDefinition> Stream<T> getAllTypeExtension(
      String name, Class<T> type, @NonNull XtextResourceSet set) {
    return getAllContentsOfType(type, set) //TODO: check this method (domain-types edge case) & getAllTypes.
        .filter(extensionDefinition -> StringUtils.endsWith(extensionDefinition.getName(), name));
  }

  /**
   * Get all TypeDefinitions from the provided resource set.
   *
   * @param set resource set
   * @return stream of type definitions from the given resource set.
   */
  public static Stream<TypeDefinition> getAllTypes(@NonNull XtextResourceSet set) {
    return getTypeSystemDefinition(set).map(TypeSystemDefinition::getType).filter(Objects::nonNull);
  }

  /**
   * Parse a String value from a ValueWithVariable.
   *
   * @param valueWithVariable a ValueWithVariable that has a non-null {@code getStringValue()}
   * @return A String with escaped double quotes (") removed from the String
   */
  public static String parseString(ValueWithVariable valueWithVariable) {
    //TODO: actually change grammar to parse out quotes from strings
    return valueWithVariable.getStringValue().replaceAll("\"", "");
  }

  /**
   * Parse a String value from a Value
   *
   * @param stringValue a Value that has a non-null {@code getStringValue()}
   * @return A String with escaped double quotes (") removed from the String
   */
  public static String parseString(Value stringValue) {
    return stringValue.getStringValue().replaceAll("\"", "");
  }

  /**
   * Filters the resourceSet to provide a stream of type T.
   *
   * @param <T> the type parameter
   * @param type the type
   * @param set the set
   * @return the stream
   */
  public static <T extends EObject> Stream<T> getAllContentsOfType(Class<T> type, @NonNull XtextResourceSet set) {
    return getTypeSystem(set).flatMap(typeSystem -> EcoreUtil2
        .getAllContentsOfType(typeSystem, type).stream()).filter(Objects::nonNull);
  }

  private static Stream<TypeSystemDefinition> getTypeSystemDefinition(@NonNull XtextResourceSet set) {
    return filteredStreamOf(TypeSystem.class, set.getResources().stream()
        .flatMap(resource -> resource.getContents().stream()))
        .flatMap(typeSystem -> typeSystem.getTypeSystemDefinition().stream());
  }

  private static Stream<TypeSystem> getTypeSystem(@NonNull XtextResourceSet set) {
    return filteredStreamOf(TypeSystem.class, set.getResources().stream()
        .flatMap(resource -> resource.getContents().stream()));
  }

  public static <T> Stream<T> filteredStreamOf(Class<T> type, Stream<Object> stream) {
    return stream.filter(type::isInstance)
        .map(type::cast);
  }

  public static boolean isObjectType(final NamedType type) {
    //nested stitch only supported for ObjectTypes
    TypeDefinition typeDefinition = XtextTypeUtils.getObjectType(type);
    return Objects.nonNull(typeDefinition) && typeDefinition instanceof ObjectTypeDefinition;
  }

  public static String toDescriptiveString(TypeDefinition typeDefinition) {
    return String.format(XTEXT_TYPE_FORMAT, typeDefinition.getName(), typeDefinition.eClass().getName(),
        typeDefinition.getDesc());
  }

  public static String toDescriptiveString(NamedType namedType) {
    TypeDefinition objectType = XtextTypeUtils.getObjectType(namedType);
    return Objects.nonNull(objectType) ? toDescriptiveString(objectType)
        : String.format(XTEXT_TYPE_FORMAT, XtextTypeUtils.typeName(namedType), StringUtils.EMPTY, StringUtils.EMPTY);
  }

  public static boolean typeContainsDirective(TypeDefinition typeDefinition, String directiveName) {
    return typeDefinition.getDirectives().stream()
            .anyMatch(directive -> directive.getDefinition().getName().equals(directiveName));
  }

  public static void checkFieldReferenceRecursively(XtextGraph sourceGraph, String typeName, List<FieldDefinition> declaredDefinitions, Field fieldToCheck) {
    String fieldName = fieldToCheck.getName();
    Optional<FieldDefinition> optionalFieldDefinition = declaredDefinitions.stream()
            .filter(fieldDefinition -> fieldDefinition.getName().equals(fieldName))
            .findFirst();

    if(optionalFieldDefinition.isPresent()) {
      if(CollectionUtils.isNotEmpty(fieldToCheck.getChildren())) {
        FieldDefinition fieldDefinition = optionalFieldDefinition.get();
        TypeDefinition childType = sourceGraph.getType(fieldDefinition.getNamedType());

        //should only be 1 element, but put in loop just in case something changes later
        for(SelectionSet childSelectionSet: fieldToCheck.getChildren().stream().map(SelectionSet.class::cast).collect(Collectors.toList())) {
          for(Field childField : getFieldsFromSelectionSet(childSelectionSet)) {
            checkFieldReferenceRecursively(sourceGraph, childType.getName(), getFieldDefinitions(childType), childField);
          }
        }
      }
    } else {
      throw new InvalidFieldSetReferenceException(fieldName, typeName);
    }
  }

  public static List<Field> getFieldsFromSelectionSet(SelectionSet selectionSet) {
    return selectionSet.getSelections().stream().map(Field.class::cast).collect(Collectors.toList());
  }

  public static void checkFieldSetValidity(XtextGraph sourceGraph, TypeDefinition typeDefinition, String fieldSet) {
    if(StringUtils.isBlank(fieldSet)) {
      throw new EmptyFieldsArgumentKeyDirective(typeDefinition.getName());
    }

    if(!fieldSet.startsWith("{")) {
      fieldSet = "{ " + fieldSet;
      fieldSet = fieldSet + " }";
    }

    //Throws InvalidSyntaxException if fieldSet is incorrect
    Document fieldSetDocument = Parser.parse(fieldSet);

    List<OperationDefinition> definitions = fieldSetDocument.getDefinitions().stream()
            .map(OperationDefinition.class::cast).collect(Collectors.toList());

    List<FieldDefinition> typeFieldDefinitions = getFieldDefinitions(typeDefinition);

    for( final OperationDefinition definition : definitions) {
      List<Field> fields = definition.getSelectionSet().getSelections().stream().map(Field.class::cast).collect(Collectors.toList());
      for(Field field : fields) {
        checkFieldReferenceRecursively(sourceGraph,typeDefinition.getName(), typeFieldDefinitions, field);
      }
    }
  }

}
