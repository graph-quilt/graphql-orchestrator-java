package com.intuit.graphql.orchestrator.xtext;

import com.intuit.graphql.graphQL.ObjectType;
import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import com.intuit.graphql.graphQL.OperationTypeDefinition;
import com.intuit.graphql.graphQL.SchemaDefinition;
import com.intuit.graphql.graphQL.TypeSystem;
import com.intuit.graphql.graphQL.TypeSystemDefinition;
import com.intuit.graphql.orchestrator.schema.Operation;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.XtextResourceSet;

/**
 * This class wraps a GraphQL XtextResource set and provides helper methods to extract types from the ResourceSet.
 */
@Getter
public class GraphQLResourceSet {

  /**
   * contains the SDL files that represents this GraphQL resource
   */
  private final XtextResourceSet graphQLResourceSet;

  //todo constructor temporary until proper builder style is created.
  public GraphQLResourceSet(@NonNull final XtextResourceSet graphQLResourceSet) {
    this.graphQLResourceSet = graphQLResourceSet;
  }


  /**
   * Find the schema definition from this {@link XtextResourceSet} as an Optional.
   *
   * @return the schema definition that exists in this resource as an Optional
   */
  public Optional<SchemaDefinition> findSchemaDefinition() {
    return graphQLResourceSet.getResources().stream()
        .flatMap(resource -> resource.getContents().stream())
        .filter(TypeSystem.class::isInstance)
        .map(TypeSystem.class::cast)
        .flatMap(typeSystem -> typeSystem.getTypeSystemDefinition().stream())
        .map(TypeSystemDefinition::getSchema)
        .filter(Objects::nonNull)
        .findFirst();
  }

  private Optional<ObjectTypeDefinition> findOperationType(Operation operation, SchemaDefinition schemaDefinition) {
    return filteredStreamOf(ObjectTypeDefinition.class,
        filteredStreamOf(ObjectType.class,
            schemaDefinition.getOperationTypeDefinition().stream()
                .filter(opTD -> operation.getName().equalsIgnoreCase(opTD.getOperationType().getName()))
                .map(OperationTypeDefinition::getNamedType)
        ).map(ObjectType::getType))
        .findFirst();
  }

  /**
   * Finds the operation ObjectTypeDefinition from this ResourceSet. First checks if the operation inside the {@link
   * SchemaDefinition}, if not gets the operation from the list of {@link ObjectTypeDefinition}
   *
   * @param operation the operation
   * @return the operation
   */
  public Optional<ObjectTypeDefinition> findOperationType(Operation operation) {
    return findSchemaDefinition()
        .map(definition -> findOperationType(operation, definition))
        .orElseGet(() -> findObjectType(operation.getName()));
  }

  /**
   * A variant on {@link #findOperationType} that returns null if the operation ObjectTypeDefinition does not exist in
   * this set.
   *
   * @param operation The operation to find
   * @return The ObjectTypeDefinition of the operation in this ResourceSet, or else null if it does not exist
   */
  public ObjectTypeDefinition getOperationType(Operation operation) {
    return findOperationType(operation).orElse(null);
  }

  /**
   * Finds the object type optional with the given name from the provided XtextResourceSet.
   *
   * @param name the name of the object type
   * @return the optional
   */
  public Optional<ObjectTypeDefinition> findObjectType(String name) {
    return getAllContentsOfType(ObjectTypeDefinition.class)
        .filter(objectTypeDefinition -> StringUtils.endsWithIgnoreCase(objectTypeDefinition.getName(), name))
        .findFirst();
  }

  /**
   * A variant on {@link #findObjectType(String)} that returns null if the object type does not exist in this
   * ResourceSet.
   *
   * @param name the name of the object type
   * @return the ObjectTypeDefinition that matches the name or else null
   */
  public ObjectTypeDefinition getObjectType(String name) {
    return findObjectType(name).orElse(null);
  }

  private Stream<TypeSystem> getTypeSystem() {
    return filteredStreamOf(TypeSystem.class, graphQLResourceSet.getResources().stream()
        .flatMap(resource -> resource.getContents().stream()));
  }

  private <T> Stream<T> filteredStreamOf(Class<T> type, Stream<Object> stream) {
    return stream.filter(type::isInstance)
        .map(type::cast);
  }

  public <T extends EObject> Stream<T> getAllContentsOfType(Class<T> type) {
    return getTypeSystem().flatMap(typeSystem -> EcoreUtil2
        .getAllContentsOfType(typeSystem, type).stream()).filter(Objects::nonNull);
  }
}
